package com.example.android.braillefeeder;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.example.android.braillefeeder.data.model.Article;

import java.util.Locale;

/**
 * Created by Juraj on 3/19/2018.
 */

public class TextRead {

    public interface TextReadListener {
        void onTextReadCompleted();
        void onTextReadStarted();
    }

    private static final String UTTERANCE_ID
            = "com.example.android.braillefeeder.UTTERANCE_ID";

    private TextToSpeech mTextToSpeech;
    private TextReadListener mTextReadListener;

    public TextRead(Context context, TextToSpeech.OnInitListener onInitListener, TextReadListener textReadListener) {
        mTextToSpeech = new TextToSpeech(context, onInitListener);
        mTextReadListener = textReadListener;

        mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                mTextReadListener.onTextReadStarted();
            }

            @Override
            public void onDone(String s) {
                mTextReadListener.onTextReadCompleted();
            }

            @Override
            public void onError(String s) {
                mTextReadListener.onTextReadCompleted();
            }
        });
    }

    public void speakText(Article article) {
        if( article.getDescription() != null) {
            if (!article.getDescription().isEmpty()) {
                mTextToSpeech.speak(article.getDescription(), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
//                Log.e("AA", article.getDescription());
            }
        } else if( article.getTitle() != null) {
            if(!article.getTitle().isEmpty()) {
                mTextToSpeech.speak(article.getTitle(), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
            }
        }

    }

    public void speakText(String text) {
        if (!text.isEmpty()) {
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
        }
    }

    public void shutDownSpeaker() {
        mTextToSpeech.stop();
        mTextToSpeech.shutdown();
    }

    public void stopSpeaker() {
        mTextToSpeech.stop();
    }

    public TextToSpeech getTextToSpeech() {
        return mTextToSpeech;
    }

    public void setTextToSpeech(TextToSpeech textToSpeech) {
        mTextToSpeech = textToSpeech;
    }

    public void changeLanguage(Locale locale) {
        if( mTextToSpeech.isLanguageAvailable(locale) < 0) {
            Log.d("TextRead", "Locale Unvailaible");
            Log.d("TextRead", Locale.getAvailableLocales().toString());

        }
        mTextToSpeech.setLanguage(locale);
    }
}
