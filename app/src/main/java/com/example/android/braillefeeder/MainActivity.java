package com.example.android.braillefeeder;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.android.braillefeeder.data.ApiUtils;
import com.example.android.braillefeeder.data.Article;
import com.example.android.braillefeeder.data.ArticleList;
import com.example.android.braillefeeder.data.remote.NewsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends Activity {

    private List<Article> mArticleList;

    private NewsService mNewsService;

    private TextRead mTextRead;

    Button mButton;

    String api = "c5c6e9d0834c42e086cad21c0bb29f11";

    private int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), 0);

        mButton = findViewById(R.id.button);



        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if( mArticleList != null) {
                    i++;
                    mTextRead.speakText(mArticleList.get(i));
                } else {
                    loadAnswers();
                }
            }
        });

        mTextRead = new TextRead(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                mTextRead.mTextToSpeech.setLanguage(new Locale("sk"));
            }
        });
        mNewsService = ApiUtils.getNewService();

        Log.e("URL: ", mNewsService.getResponse(api).request().toString());
        loadAnswers();
    }

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
}

/*
if(response.isSuccessful()) {
                    ArrayList arrayList = response.body();
                    mTextRead.speakText(mArticleList.get(0));
                }else {
                    Log.e("MainActivity", "Response unsuccesful: " + response.code());
                }


                 Log.e("MainActivity", "Response failure: " + t.toString());
 */