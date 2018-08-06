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
import com.example.android.braillefeeder.data.Article;
import com.example.android.braillefeeder.data.ArticleList;
import com.example.android.braillefeeder.data.remote.NewsService;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends Activity implements PocketSphinxSTT.PocketSphinxListener{

    private static final String HEY_BRAILLE_CONTEXT = "HEY_BRAILLE_CONTEXT";

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private List<Article> mArticleList;

    private NewsService mNewsService;

    private TextRead mTextRead;

    private SpeechToText mSpeechToTextService;
    private SpeechRecorder mSpeechRecorder;
    private final SpeechRecorder.SpeechRecorderCallback mRecorderCallback = new SpeechRecorder.SpeechRecorderCallback() {

        @Override
        public void onRecordStarted() {
            if( mSpeechToTextService != null) {
                Log.d("onRecordStarted", "true");
                mSpeechToTextService.startRecognizing(mSpeechRecorder.getSampleRate());
            }
        }

        @Override
        public void onRecordListening(byte[] data, int size) {
//            Log.d("onRecordListening", "true");
            if( mSpeechToTextService != null) {
                mSpeechToTextService.recognize(data, size);
            }
        }

        @Override
        public void onRecordEnded() {
//            Log.d("onRecordEnded", "true");
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

    Button mButton;
    TextView mTextView;
    String api = "c5c6e9d0834c42e086cad21c0bb29f11";
    private int i;
    private PocketSphinxSTT mPocketSphinxSTT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

//        mPocketSphinxSTT = new PocketSphinxSTT(this, this);

        mButton = findViewById(R.id.button);
        mTextView = findViewById(R.id.textview);


        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                /*
                if( mArticleList != null) {
                    i++;
                    if( i < 20) {
                        mTextRead.speakText(mArticleList.get(i));
                    }
                } else {
                    loadAnswers();
                }
                */
            }
        });

        mTextRead = new TextRead(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                mTextRead.mTextToSpeech.setLanguage(new Locale("en"));
            }
        });
        mNewsService = ApiUtils.getNewService();
//        Log.e("URL: ", mNewsService.getResponse(api).request().toString());
        //loadAnswers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
//                mPocketSphinxSTT = new PocketSphinxSTT(this, this);
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d("onStart", "Permission granted");
            startVoiceRecorder();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
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
                                }
                            }
                        });
                    }
                }
            };


    public void loadAnswers() {
        mNewsService.getResponse(api).enqueue(new Callback<ArticleList>() {
            @Override
            public void onResponse(Call<ArticleList> call, Response<ArticleList> response) {
                if(response.isSuccessful()) {

                    ArticleList articleList = response.body();
                    mArticleList = articleList.getArticleList();
                    i = 0;
                    mTextRead.speakText(mArticleList.get(0));
                }else {
                    Log.e("MainActivity", "Response unsuccesful: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ArticleList> call, Throwable t) {
                Log.e("MainActivity", "Response failure: " + t.toString());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( mTextRead != null) {
            mTextRead.shutDownSpeaker();
        }
//        mPocketSphinxSTT.onDestroy();
    }

    @Override
    public void onSpeechRecognizerReady() {
        mPocketSphinxSTT.startListeningToActivationPhrase();
    }

    @Override
    public void onActivationPhraseDetected() {
        mPocketSphinxSTT.startListeningToAction();
    }

    @Override
    public void onTextRecognized(String recognizedText) {
        Log.d("onTextRecognized", recognizedText);
    }

    @Override
    public void onTimeout() {
        mPocketSphinxSTT.startListeningToActivationPhrase();
    }


}