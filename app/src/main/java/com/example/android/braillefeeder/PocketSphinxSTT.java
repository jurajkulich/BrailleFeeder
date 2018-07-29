package com.example.android.braillefeeder;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by Juraj on 6/30/2018.
 */

public class PocketSphinxSTT extends Activity implements RecognitionListener {

    public interface PocketSphinxListener {
        void onSpeechRecognizerReady();
        void onActivationPhraseDetected();
        void onTextRecognized(String recognizedText);
        void onTimeout();
    }

    private static final String TAG = PocketSphinxSTT.class.getSimpleName();

    private static final String ACTIVATION_PHRASE = "dog man";
    private static final String WAKEUP_SEARCH = "wakeup";
    private static final String ACTION_SEARCH = "action";

    private PocketSphinxListener mPocketSphinxListener;
    private SpeechRecognizer recognizer;

    public PocketSphinxSTT(Context context, PocketSphinxListener listener) {
        this.mPocketSphinxListener = listener;
        runRecognizerSetup(context);
    }


    private void runRecognizerSetup(final Context context) {
        Log.d(TAG, "runRecognizerSetup");

        new AsyncTask<Void, Void, Exception>() {

            @Override
            protected Exception doInBackground(Void... voids) {
                try {
                    Assets assets = new Assets(context);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                if( e != null ) {
                    Log.e(TAG, "Failed to initialize recognizer: " + e);
                } else {
                    mPocketSphinxListener.onSpeechRecognizerReady();
                }
            }
        }.execute();
    }

    public void startListeningToActivationPhrase() {
        Log.i(TAG, "Start listening for the \"ok things\" keyphrase");
        recognizer.startListening(WAKEUP_SEARCH);
    }

    public void startListeningToAction() {
        Log.i(TAG, "Start listening for some actions with a 10secs timeout");
        recognizer.startListening(ACTION_SEARCH, 10000);
    }

    private void setupRecognizer(File assetDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup().setAcousticModel(new File(assetDir, "en-us-ptm"))
                .setDictionary(new File(assetDir, "cmudict-en-us.dict"))
                .getRecognizer();
        recognizer.addListener(this);

        recognizer.addKeyphraseSearch(WAKEUP_SEARCH, ACTIVATION_PHRASE);
        recognizer.addNgramSearch(ACTION_SEARCH, new File(assetDir, "predefined.lm.bin"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }


    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }

        String text = hypothesis.getHypstr();
        if (text.equals(ACTIVATION_PHRASE)) {
            Log.i(TAG, "Activation keyphrase detected during a partial result");
            recognizer.stop();
        } else {
            // Log.i(TAG, "On partial result: " + text);
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }

        String text = hypothesis.getHypstr();
        Log.i(TAG, "On result: " + text);

        if (ACTIVATION_PHRASE.equals(text)) {
            mPocketSphinxListener.onActivationPhraseDetected();
        } else {
            mPocketSphinxListener.onTextRecognized(text);
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "OnError", e);
    }

    @Override
    public void onTimeout() {
        Log.i(TAG, "onTimeout");
        recognizer.stop();
        mPocketSphinxListener.onTimeout();
    }



}
