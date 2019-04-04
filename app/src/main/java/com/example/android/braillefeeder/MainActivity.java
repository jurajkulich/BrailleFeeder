package com.example.android.braillefeeder;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.braillefeeder.apis.SpeechToText;
import com.example.android.braillefeeder.apis.TextRead;
import com.example.android.braillefeeder.apis.VisionService;
import com.example.android.braillefeeder.brailleutils.BrailleConverter;
import com.example.android.braillefeeder.data.ApiUtils;
import com.example.android.braillefeeder.data.model.ArticleList;
import com.example.android.braillefeeder.data.dao.ArticleRoomDatabase;
import com.example.android.braillefeeder.data.model.Article;
import com.example.android.braillefeeder.data.model.ArticleSettings;
import com.example.android.braillefeeder.hardwareconnections.CameraService;
import com.example.android.braillefeeder.hardwareconnections.ConnectionUtil;
import com.example.android.braillefeeder.hardwareconnections.PeripheralConnections;
import com.example.android.braillefeeder.hardwareconnections.SpeechRecorder;
import com.example.android.braillefeeder.remote.NewsService;
import com.example.android.braillefeeder.utils.VoiceControl;
import com.google.android.things.pio.PeripheralManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// MainActivity - where the shit happens
public class MainActivity extends Activity implements
        VoiceControl.VoiceControlListener,
        TextRead.TextReadListener,
        VisionService.VisionServiceListener {

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_CAMERA = 2;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 3;
    private static final int PERMISSION_REQUEST_LOCATION = 4;

    private static int DEFAULT_DELAY = 400;
    private static final String OFF_SOLENOID_STATE = "000000";
    private static final String ON_SOLENOID_STATE = "111111";

    private Handler brailleHandler = new Handler();
    private Runnable brailleRunnable;

    private static final String DATABASE = "articles_db";
    private ArticleRoomDatabase mArticleDatabase;

    private AudioManager mAudioManager;
    private int mAudioMax;

    // list of current fetched articles
    private List<Article> mArticleList;

    private NewsService mNewsService;

    // Text To Speech service
    private TextRead mTextRead;
    // Speech To Text service
    private SpeechToText mSpeechToTextService;
    // Voice recorder for voice control
    private SpeechRecorder mSpeechRecorder;
    // Camera Service
    private CameraService mCameraService;
    // Vision service for photo analysis
    private VisionService mVisionService;

    private VoiceControl mVoiceControl = new VoiceControl(this);

    // Class for accessing peripherals
    private PeripheralConnections mPeripheralConnections;

    // Callback for SpeechRecorder
    private final SpeechRecorder.SpeechRecorderCallback mRecorderCallback = new SpeechRecorder.SpeechRecorderCallback() {

        // Function is called when the speaking is detected
        @Override
        public void onRecordStarted() {
            if( mSpeechToTextService != null) {
                mSpeechToTextService.startRecognizing(mSpeechRecorder.getSampleRate());
            }
        }

        // Function is called when the speaking is still active
        @Override
        public void onRecordListening(byte[] data, int size) {
            if( mSpeechToTextService != null) {
                mSpeechToTextService.recognize(data, size);
            }
        }

        // Function is called when the speaking is done
        @Override
        public void onRecordEnded() {
            if( mSpeechToTextService != null) {
                mSpeechToTextService.finishRecognizing();
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("ServiceConnection", "onServiceConnected");
            mSpeechToTextService = SpeechToText.from(iBinder);
            mSpeechToTextService.addListener(mSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("ServiceConnection", "onServiceDisconnected");
            mSpeechToTextService = null;
        }
    };

    @BindView(R.id.button)
    Button mButton;

    @BindView(R.id.button_camera)
    Button mCameraButton;

    @BindView(R.id.textview)
    TextView mSpeechTextView;

    @BindView(R.id.vision_answer_textview)
    TextView mVisionAnswerTextView;

    @BindView(R.id.article_textview)
    TextView mArticleTextView;

    @BindView(R.id.imageview)
    ImageView mImageView;

    @BindView(R.id.button_recorder_on)
    Button mButtonRecorderOn;

    @BindView(R.id.button_recorder_off)
    Button mButtonRecorderOff;

    // Api key for News API
    private final static String API_KEY_NEWS = "cb79c51392b84f89a51b4020b8a4aa90";
    // Hashmap for query build
    private Map<String, String> apiMap = new HashMap<>();

    private static SharedPreferences mSharedPreferences;
    private String mLocale;

    private boolean brailleActive = false;
    // current position from fetched articles
    private int mArticlePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ButterKnife binds all views to variables
        ButterKnife.bind(this);

        mPeripheralConnections = new PeripheralConnections(PeripheralManager.getInstance());
        mPeripheralConnections.openSolenoidsGpio();

        mPeripheralConnections.openSwitchButtonsGpio(new com.google.android.things.contrib.driver.button.Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(com.google.android.things.contrib.driver.button.Button button, boolean pressed) {
                if(pressed) {
                    if( mTextRead != null) {
                        mTextRead.stopSpeaker();
                    }
                    if( brailleHandler != null) {
                        brailleHandler.removeCallbacksAndMessages(null);

                    }
                    Log.d("Article button", "button pressed: " + button.toString() + pressed);
                    changeArticle(1);
                }
            }
        });

        mPeripheralConnections.openVolumeButtonsGpio(new com.google.android.things.contrib.driver.button.Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(com.google.android.things.contrib.driver.button.Button button, boolean pressed) {
                if(pressed) {
                    Log.d("Volume button", "button pressed: " + pressed);
                    onVolumeSettingCommand();
                }
            }
        });

        mPeripheralConnections.openLengthButtonsGpio(new com.google.android.things.contrib.driver.button.Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(com.google.android.things.contrib.driver.button.Button button, boolean pressed) {
                if(pressed) {
                    if( mTextRead != null) {
                        mTextRead.stopSpeaker();
                    }
                    if( brailleHandler != null) {
                        brailleHandler.removeCallbacksAndMessages(null);

                    }
                    if( mPeripheralConnections != null && brailleActive) {
                        mPeripheralConnections.sendGpioValues("000000");
                    }
                    Log.d("Recorder button", "button pressed: " + pressed);
                    startVoiceRecorder();
                }
            }
        });

        // setVolumeControlStream set default control stream to music, with
        // this setting we can change volume of speaking
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        // Getting the database instance
        mArticleDatabase = ArticleRoomDatabase.getDatabase(this);

        mNewsService = ApiUtils.getNewService();

        mVisionService = new VisionService(this, this);

        mTextRead = new TextRead(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                mTextRead.getTextToSpeech().setLanguage(new Locale(mLocale));
                mTextRead.speakText(getResources().getString(R.string.welcome_speech));
                loadAnswers();
            }
        }, this);


        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( mTextRead != null) {
                    mTextRead.stopSpeaker();
                }
                if( brailleHandler != null) {
                    brailleHandler.removeCallbacksAndMessages(null);

                }
                changeArticle(1);
            }
        });

        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraService.takePicture();
            }
        });

        mButtonRecorderOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( mTextRead != null) {
                    mTextRead.stopSpeaker();
                }
                if( brailleHandler != null) {
                    brailleHandler.removeCallbacksAndMessages(null);

                }
                startVoiceRecorder();
            }
        });

        mButtonRecorderOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopVoiceRecorder();
            }
        });
    }

    // Callback for RequestingPermissions
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startVoiceRecorder();
            } else {
                finish();
            }
        } else if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, SpeechToText.class), mServiceConnection, BIND_AUTO_CREATE);

        mCameraService = CameraService.getInstance();
        mCameraService.initializeCamera(this, mOnImageAvailableListener);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mLocale = getLocale();
        onLocaleChangeCommand(mLocale);

        // We need permissions for Camera - photo capturing, and Audio - voice control
        int permission_all = 1;
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        if( !hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, permission_all);
        } else {
//            startVoiceRecorder();
        }
    }

    @Override
    protected void onStop() {
        stopVoiceRecorder();

        if( mSpeechToTextService != null) {
            mSpeechToTextService.removeListener(mSpeechServiceListener);
        }
        unbindService(mServiceConnection);
        mSpeechToTextService = null;

        mCameraService.shutdown();

        super.onStop();
    }

    private void startVoiceRecorder() {
        if (mSpeechRecorder != null) {
            mSpeechRecorder.stopRecorder();
        }
        mSpeechRecorder = new SpeechRecorder(mRecorderCallback);
        mSpeechRecorder.startRecorder();
    }

    private void stopVoiceRecorder() {
        if (mSpeechRecorder != null) {
            mSpeechRecorder.stopRecorder();
            mSpeechRecorder = null;
        }
    }

    private final SpeechToText.SpeechToTextListener mSpeechServiceListener =
            new SpeechToText.SpeechToTextListener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        mSpeechRecorder.dismiss();
                        Log.d("SpeechToTextListener", "isFinal");
                    }
                    if (text != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    Log.d("Voice", text);
                                    mSpeechTextView.setText(text);
                                    VoiceControl.recognizeCommand(text);
                                }
                            }
                        });
                    }
                }
            };


    private void loadAnswers() {
        Log.d("loadAnswers()", "loadAnswers()");
        if( ConnectionUtil.getConnectivityStatus(this) == ConnectionUtil.TYPE_NOT_CONNECTED) {
            Log.e("loadAnswers()", "not connected to internet");

            mTextRead.speakText(getString(R.string.not_connected));
            loadSavedArticles();
            return;
        }

        mTextRead.speakText(getString(R.string.downloading_articles));
        Log.e("changeArticle", "loadAnswers() because articleList is Null");

        buildQuery();
        mNewsService.getResponse(apiMap).enqueue(new Callback<ArticleList>() {
            @Override
            public void onResponse(Call<ArticleList> call, Response<ArticleList> response) {
                if(response.isSuccessful()) {
                    Log.d("loadAnswers", response.raw().request().url().toString());
                    if( response.body() != null) {
                        mArticleList = response.body().getArticleList();
                    }
                    mArticlePosition = -1;
                }else {
                    Log.d("loadAnswers", response.raw().request().url().toString());
                    Log.e("MainActivity", "Response unsuccesful: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ArticleList> call, Throwable t) {
                Log.e("MainActivity", "Response failure: " + t.toString());
            }
        });
    }

    private void buildQuery() {
        if( mLocale.equals("sk")) {
            apiMap.put("country", mLocale);
        } else {
            apiMap.put("country", "us");
        }
        apiMap.put("apiKey", API_KEY_NEWS);
        apiMap.put("pageSize", Integer.toString(60));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( mTextRead != null) {
            mTextRead.shutDownSpeaker();
        }
    }

    @Override
    public void onRecognizeCommand(ArticleSettings articleSettings) {
        Log.d("onRecognizeCommand", "onRecognizeCommand");
        if( articleSettings.getAbout() != null) {
            apiMap.put("q", articleSettings.getAbout());
            apiMap.remove("category");
        } else if( articleSettings.getCategory() != null) {
            apiMap.put("category", articleSettings.getCategory());
            apiMap.remove("q");
        }
        loadAnswers();
    }

    @Override
    public void onHelpCommand() {
        mTextRead.speakText(getResources().getString(R.string.help_speech));
    }

    @Override
    public void onLocaleChangeCommand(String locale) {
        if( mSharedPreferences != null) {
            mSharedPreferences.edit().putString("defaultLanguage", locale).apply();
        }
        setLocale(new Locale(locale));
        loadAnswers();
        onContentChanged();
    }

    @Override
    public void onNextArticleCommand() {
        changeArticle(1);
    }

    @Override
    public void onPreviousArticleCommand() {
        changeArticle(-1);
    }

    @Override
    public void onTakePhotoCommand() {
        mCameraService.takePicture();
    }

    @Override
    public void onCommandNotFound() {
        mTextRead.speakText(getString(R.string.command_not_found));
    }

    @Override
    public void onSaveArticleCommand() {
        if( mArticleDatabase != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mArticleDatabase.mArticleDao().insert(mArticleList.get(mArticlePosition));
                }
            }).start();
            mTextRead.speakText(getString(R.string.article_saved));
        }
    }

    @Override
    public void onLoadSavedArticleCommand() {
        loadSavedArticles();
    }

    @Override
    public void onVolumeSettingCommand() {
        if(!brailleActive) {
            float per = (mAudioMax / 8);
            float volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume + per > mAudioMax) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            } else {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (volume + per), 0);
            }
        } else {
            if( DEFAULT_DELAY == 400) {
                DEFAULT_DELAY = 600;
            } else if ( DEFAULT_DELAY == 600) {
                DEFAULT_DELAY = 800;
            } else {
                DEFAULT_DELAY = 400;
            }
        }
    }

    @Override
    public void onVolumePercentSettingCommand(float percent) {
        int per = (int) (mAudioMax*percent);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, per, 0);
        mTextRead.speakText(getString(R.string.volume_set_up) + per + " %.");
    }

    @Override
    public void onBrailleActiveCommand(boolean active) {
        if(active) {
            mTextRead.speakText(getString(R.string.braillecell_active));
        } else {
            mTextRead.speakText(getString(R.string.braillecell_inactive));
        }
        brailleActive = active;
    }


    @Override
    public void onTextReadCompleted() {
//        startVoiceRecorder();
    }

    @Override
    public void onTextReadStarted() {
        stopVoiceRecorder();
    }

    public void changeArticle(int pos) {
        if( mArticleList != null) {
            if( (mArticlePosition + pos) < mArticleList.size() && (mArticlePosition + pos) >= 0) {
                mArticlePosition += pos;
                if( !brailleActive) {
                    mTextRead.speakText(mArticleList.get(mArticlePosition));
                } else {
                    showInBraille(mArticleList.get(mArticlePosition));
                }
                mArticleTextView.setText(mArticleList.get(mArticlePosition).getDescription());
            }
        } else {
            loadAnswers();
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = imageReader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    final Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    image.close();
                    mVisionService.callCloudVision(bitmapImage);
                    mTextRead.speakText(getResources().getString(R.string.on_take_photo));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImageView.setImageBitmap(bitmapImage);
                        }
                    });

                }
    };

    @Override
    public void onVisionCompleted(String result) {
        mTextRead.speakText(result);
        mVisionAnswerTextView.setText(result);
        Log.d("onVisionCompleted", result);
    }

    private void loadSavedArticles() {
        if( mArticleDatabase != null) {
            mTextRead.speakText("Loading offline articles.");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mArticlePosition = 0;
                    mArticleList = mArticleDatabase.mArticleDao().getAllArticles();
                    Log.d("loadSavedArticles()", String.valueOf(mArticleList.size()));
                }
            }).start();
        } else {
            Log.d("loadSavedArticles()", "Database is null");
        }
    }

    private boolean hasPermissions(String... permissions) {
        if(permissions != null) {
            for( String permission: permissions) {
                if( ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setLocale(Locale locale) {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocales(new LocaleList(locale));
        mLocale = locale.toString();
        Log.d("mainActivity()","setLocale - locale changed to: " + mLocale);
        resources.updateConfiguration(configuration, null);
        mTextRead.changeLanguage(locale);
    }

    public static String getLocale() {
        return mSharedPreferences.getString("defaultLanguage", "us");
    }

    private void showInBraille(Article article) {
        final List<String> words = BrailleConverter.convertFromWords(article.getTitle(), article.getDescription());
//        final List<String> words =  new ArrayList<String>(Arrays.asList("111111", "111111", "111111", "111111", "111111", "111111"
//        ,"111111", "111111", "111111", "111111", "111111", "111111", "111111", "111111", "111111", "111111"));
        words.add(0, ON_SOLENOID_STATE);
        words.add(0, ON_SOLENOID_STATE);
        words.add(OFF_SOLENOID_STATE);
        brailleHandler = new Handler();
        for( int i = 0; i < words.size(); i++) {
            final String word = words.get(i);
            brailleHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("configureGpio", word);
                    mPeripheralConnections.sendGpioValues(word);
                }
            }, DEFAULT_DELAY * i);
        }
        resetSolenoids();
    }

    public void resetSolenoids() {
        mPeripheralConnections.sendGpioValues(OFF_SOLENOID_STATE);
    }
}