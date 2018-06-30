package com.example.android.braillefeeder;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.braillefeeder.data.ApiUtils;
import com.example.android.braillefeeder.data.Article;
import com.example.android.braillefeeder.data.ArticleList;
import com.example.android.braillefeeder.data.remote.NewsService;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends Activity implements PocketSphinxSTT.PocketSphinxListener{

    private static final String HEY_BRAILLE_CONTEXT = "HEY_BRAILLE_CONTEXT";

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private List<Article> mArticleList;

    private NewsService mNewsService;

    private TextRead mTextRead;

    Button mButton;

    TextView mTextView;

    String api = "";

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

        mPocketSphinxSTT = new PocketSphinxSTT(this, this);

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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                mPocketSphinxSTT = new PocketSphinxSTT(this, this);
            } else {
                finish();
            }
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPocketSphinxSTT.onDestroy();
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
    */
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