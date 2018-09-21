package com.example.android.braillefeeder;

import android.speech.tts.Voice;
import android.util.Log;

import com.example.android.braillefeeder.data.model.ArticleSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VoiceControl {

    private static final String TAG = VoiceControl.class.getSimpleName();

    public interface VoiceControlListener {
        void onRecognizeCommand(ArticleSettings articleSettings);
        void onHelpCommand();
        void onLocaleChangeCommand();
        void onNextArticleCommand();
        void onPreviousArticleCommand();
    }

    private static VoiceControlListener mVoiceControlListener;

    public VoiceControl(VoiceControlListener voiceControlListener) {
        mVoiceControlListener = voiceControlListener;
    }

    public static void recognizeCommand(String commands) {
        String command = commands.toLowerCase();
        ArticleSettings articleSettings = new ArticleSettings();
        List<String> commandList = new ArrayList<>(Arrays.asList(command.split(" ")));
        if( command.contains("help")) {
            mVoiceControlListener.onHelpCommand();
        }
        if( command.contains("switch") && command.contains("slovak")) {
            mVoiceControlListener.onLocaleChangeCommand();
        }
        if( command.contains("next") && command.contains("article")) {
            mVoiceControlListener.onNextArticleCommand();
        }
        if( command.contains("previous") && command.contains("article")) {
            mVoiceControlListener.onPreviousArticleCommand();
        }
        if(commandList.contains("country")) {
            articleSettings.setCountry(commandList.get(commandList.indexOf("country") + 1));
            Log.d(TAG, articleSettings.getCountry());
        }
        if(commandList.contains("source")) {
            articleSettings.setSource(commandList.get(commandList.indexOf("source") + 1));
            Log.d(TAG, articleSettings.getSource());
        }
        if(commandList.contains("category")) {
            articleSettings.setCategory(commandList.get(commandList.indexOf("category") + 1));
            Log.d(TAG, articleSettings.getCategory());
        }
        if(commandList.contains("about")) {
            articleSettings.setAbout(commandList.get(commandList.indexOf("about") + 1));
            Log.d(TAG, articleSettings.getAbout());
        }
        if(commandList.contains("language")) {
            articleSettings.setLanguage(commandList.get(commandList.indexOf("language") + 1));
            Log.d(TAG, articleSettings.getLanguage());
        }
        mVoiceControlListener.onRecognizeCommand(articleSettings);
    }
}