package com.example.android.braillefeeder.utils;

import android.util.Log;

import com.example.android.braillefeeder.data.model.ArticleSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class VoiceControl {

    private static final String TAG = VoiceControl.class.getSimpleName();

    public interface VoiceControlListener {
        void onRecognizeCommand(ArticleSettings articleSettings);
        void onHelpCommand();
        void onLocaleChangeCommand(String locale);
        void onNextArticleCommand();
        void onPreviousArticleCommand();
        void onTakePhotoCommand();
        void onCommandNotFound();
        void onSaveArticleCommand();
        void onLoadSavedArticleCommand();
        void onVolumeSettingCommand();
        void onVolumePercentSettingCommand(float percent);
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
        else if( command.contains("switch")) {
            if(command.contains("slovak")) {
                mVoiceControlListener.onLocaleChangeCommand("sk");
            } else if( command.contains("english")) {
                mVoiceControlListener.onLocaleChangeCommand(Locale.getDefault().toString());
            } else {
                mVoiceControlListener.onCommandNotFound();
            }
        }
        else if( command.contains("next") && command.contains("article")) {
            mVoiceControlListener.onNextArticleCommand();
        }
        else if( command.contains("previous") && command.contains("article")) {
            mVoiceControlListener.onPreviousArticleCommand();
        }
        else if(commandList.contains("country")) {
            articleSettings.setCountry(commandList.get(commandList.indexOf("country") + 1));
            mVoiceControlListener.onRecognizeCommand(articleSettings);
            Log.d(TAG, articleSettings.getCountry());
        }
        else if(commandList.contains("source")) {
            articleSettings.setSource(commandList.get(commandList.indexOf("source") + 1));
            mVoiceControlListener.onRecognizeCommand(articleSettings);
            Log.d(TAG, articleSettings.getSource());
        }
        else if(commandList.contains("category")) {
            articleSettings.setCategory(commandList.get(commandList.indexOf("category") + 1));
            mVoiceControlListener.onRecognizeCommand(articleSettings);
            Log.d(TAG, articleSettings.getCategory());
        }
        else if(commandList.contains("about")) {
            articleSettings.setAbout(commandList.get(commandList.indexOf("about") + 1));
            mVoiceControlListener.onRecognizeCommand(articleSettings);
            Log.d(TAG, articleSettings.getAbout());
        }
        else if(commandList.contains("language")) {
            articleSettings.setLanguage(commandList.get(commandList.indexOf("language") + 1));
            mVoiceControlListener.onRecognizeCommand(articleSettings);
            Log.d(TAG, articleSettings.getLanguage());
        }
        else if(commandList.contains("take") && commandList.contains("photo")) {
            mVoiceControlListener.onTakePhotoCommand();
            Log.d(TAG, "TakePhoto");
        }
        else if(commandList.contains("save") && commandList.contains("article")) {
            mVoiceControlListener.onSaveArticleCommand();
            Log.d(TAG, "SaveArticle");
        }
        else if((commandList.contains("show") ||commandList.contains("load"))
                && commandList.contains("saved") && commandList.contains("articles")) {
            mVoiceControlListener.onLoadSavedArticleCommand();
            Log.d(TAG, "onLoadSavedArticles");
        }
        else if(commandList.contains("volume")) {
            if( commandList.contains("up")) {
                mVoiceControlListener.onVolumeSettingCommand();
            } else if( commandList.contains("down")) {
                mVoiceControlListener.onVolumeSettingCommand();
            } else if( commandList.contains("to")) {
                String percent = commandList.get(commandList.indexOf("to") + 1);
                float per = Integer.parseInt(percent.substring(0, percent.length()-1));
                if( per >= 0 && per <= 100) {
                    mVoiceControlListener.onVolumePercentSettingCommand(Float.valueOf(per) / 100);
                } else {
                    mVoiceControlListener.onVolumePercentSettingCommand(0.5f);
                }
            }
            Log.d(TAG, "onVolumeSetting");
        }
        else
            mVoiceControlListener.onCommandNotFound();
    }
}
