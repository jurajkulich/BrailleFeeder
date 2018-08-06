package com.example.android.braillefeeder;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.android.braillefeeder.data.Article;

/**
 * Created by Juraj on 3/19/2018.
 */

public class TextRead {

    private static final String UTTERANCE_ID
            = "com.example.android.braillefeeder.UTTERANCE_ID";

    TextToSpeech mTextToSpeech;

    public TextRead(Context context, TextToSpeech.OnInitListener onInitListener) {
        mTextToSpeech = new TextToSpeech(context, onInitListener);
    }

    public void speakText(Article article) {
        if( article.getDescription() != null) {
            if (!article.getDescription().isEmpty()) {
                mTextToSpeech.speak(article.getDescription(), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
//                Log.e("AA", article.getDescription());
            }
        }
    }

    public void shutDownSpeaker() {
        mTextToSpeech.stop();
        mTextToSpeech.shutdown();
    }
}
