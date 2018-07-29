package com.example.android.braillefeeder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

//import io.grpc.

/**
 * Created by juraj on 7/28/18.
 */

public class SpeechToText extends Service {

    public interface SpeechToTextListener {
        void onSpeechRecognized(String text, boolean isFinal);
    }

    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;

//    private SpeechGrpc.SpeechStub mApi;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
