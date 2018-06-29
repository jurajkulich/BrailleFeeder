package com.example.android.braillefeeder;

import android.app.Activity;
import android.app.usage.NetworkStats;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.braillefeeder.data.ApiUtils;
import com.example.android.braillefeeder.data.Article;
import com.example.android.braillefeeder.data.ArticleList;
import com.example.android.braillefeeder.data.remote.NewsService;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.grpc.Context;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity {

    private List<Article> mArticleList;

    private NewsService mNewsService;

    private TextRead mTextRead;

    Button mButton;

    TextView mTextView;

    String api = "";

    private int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), 0);

        mButton = findViewById(R.id.button);
        mTextView = findViewById(R.id.textview);


        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if( mArticleList != null) {
                    i++;
                    if( i < 20) {
                        mTextRead.speakText(mArticleList.get(i));
                    }
                    // startSpeechToTextActivity();
                    new RecognizeTextAsyncTask().execute();
                } else {
                    loadAnswers();
                }
            }
        });

        mTextRead = new TextRead(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                mTextRead.mTextToSpeech.setLanguage(new Locale("en"));
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
    /*
    static void authExplicit(String jsonPath) throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(jsonPath))
                .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));

        // Context.Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

        System.out.println("Buckets:");
        Page<NetworkStats.Bucket> buckets = storage.list();
        for (NetworkStats.Bucket bucket : buckets.iterateAll()) {
            System.out.println(bucket.toString());
        }
    }
    */

    private class RecognizeTextAsyncTask extends AsyncTask<Void, Void, List<SpeechRecognitionResult>> {

        @Override
        protected List<SpeechRecognitionResult> doInBackground(Void... voids) {
            List<SpeechRecognitionResult> results = null;
            try (SpeechClient speechClient = SpeechClient.create()) {

                // The path to the audio file to transcribe
                String fileName = "hlasok.mp3";

                // Reads the audio file into memory
                Path path = Paths.get(fileName);
                byte[] data = Files.readAllBytes(path);
                ByteString audioBytes = ByteString.copyFrom(data);

                // Builds the sync recognize request
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                        .setSampleRateHertz(16000)
                        .setLanguageCode("en-US")
                        .build();
                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(audioBytes)
                        .build();

                // Performs speech recognition on the audio file
                RecognizeResponse response = speechClient.recognize(config, audio);
                results = response.getResultsList();


            } catch (IOException e) {
                e.printStackTrace();
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<SpeechRecognitionResult> speechRecognitionResults) {
            super.onPostExecute(speechRecognitionResults);
            if (speechRecognitionResults != null) {
                for (SpeechRecognitionResult result : speechRecognitionResults) {
                    // There can be several alternative transcripts for a given chunk of speech. Just use the
                    // first (most likely) one here.
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    Log.d("Recognition", alternative.getTranscript());
                }
            }
        }
    }

    public void startSpeechToTextActivity() {

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