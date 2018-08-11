package com.example.android.braillefeeder;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.braillefeeder.data.ApiUtils;
import com.example.android.braillefeeder.data.model.Article;
import com.example.android.braillefeeder.data.ArticleList;
import com.example.android.braillefeeder.data.model.ArticleSettings;
import com.example.android.braillefeeder.remote.NewsService;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends Activity implements VoiceControl.VoiceControlListener{

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private List<Article> mArticleList;

    private NewsService mNewsService;

    private TextRead mTextRead;

    VoiceControl mVoiceControl = new VoiceControl(this);

    private SpeechToText mSpeechToTextService;
    private SpeechRecorder mSpeechRecorder;
    private final SpeechRecorder.SpeechRecorderCallback mRecorderCallback = new SpeechRecorder.SpeechRecorderCallback() {

        @Override
        public void onRecordStarted() {
            if( mSpeechToTextService != null) {
                mSpeechToTextService.startRecognizing(mSpeechRecorder.getSampleRate());
            }
        }

        @Override
        public void onRecordListening(byte[] data, int size) {
            if( mSpeechToTextService != null) {
                mSpeechToTextService.recognize(data, size);
            }
        }

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
            Log.e("ServiceConnection", "onServiceConnected");

            mSpeechToTextService = SpeechToText.from(iBinder);
            mSpeechToTextService.addListener(mSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("ServiceConnection", "onServiceDisconnected");

            mSpeechToTextService = null;
        }
    };

    @BindView(R.id.button)
    Button mButton;

    @BindView(R.id.textview)
    TextView mTextView;

    String api = "";
    Map<String, String> apiMap = new HashMap<>();

    private int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if( mArticleList != null) {
                    if( i < mArticleList.size()) {
                        mTextRead.speakText(mArticleList.get(i));
                        stopVoiceRecorder();
                        i++;
                    }
                } else {
                    loadAnswers();
                }

            }
        });

        mTextRead = new TextRead(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                mTextRead.mTextToSpeech.setLanguage(new Locale("us"));
            }
        });
        mNewsService = ApiUtils.getNewService();
        loadAnswers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, SpeechToText.class), mServiceConnection, BIND_AUTO_CREATE);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        } else {
            startVoiceRecorder();
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
                                    Log.d("Main", text);
                                    mTextView.setText(text);
                                    VoiceControl.recognizeCommand(text);
                                }
                            }
                        });
                    }
                }
            };


    public void loadAnswers() {
        buildQuery();
        mNewsService.getResponse(apiMap).enqueue(new Callback<ArticleList>() {
            @Override
            public void onResponse(Call<ArticleList> call, Response<ArticleList> response) {
                if(response.isSuccessful()) {
                    Log.d("loadAnswers", response.raw().request().url().toString());
                    ArticleList articleList = response.body();
                    mArticleList = articleList.getArticleList();
                    i = 0;
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
        apiMap.put("country", "us");
        apiMap.put("apiKey", api);
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
        mTextRead.speakText(getResources().getString(R.string.help_voice));
    }
}