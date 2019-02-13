package com.example.android.braillefeeder.hardwareconnections;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by juraj on 7/26/18.
 */

/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Modifications copyright (C) 2019 Juraj Kulich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class SpeechRecorder {

    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000, 11025, 22050, 44100};

    // pre nase ucely staci prijimat signal MONO - jednokanalovy
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int SPEECH_TIMOUT_MILLIS = 1200;
    private static final int MAX_SPEECH_LENGTH = 5 * 1000;

    public static abstract class SpeechRecorderCallback {

        // vola sa pri spusteni nahravania
        public void onRecordStarted() {}

        // vola sa pri nahravani
        public void onRecordListening(byte[] data, int size) {}

        // vola sa pri dokonceni nahravania
        public void onRecordEnded() {}
    }

    private SpeechRecorderCallback mSpeechRecorderCallback;

    private AudioRecord mAudioRecord;

    private long mLastVoiceHeardMillis = Long.MAX_VALUE;
    private long mVoiceStartedMillis;

    private byte[] mBuffer;
    private Thread mThread;

    private final Object mLock = new Object();

    public SpeechRecorder(SpeechRecorderCallback callback) {
        mSpeechRecorderCallback = callback;
    }

    public void startRecorder() {
        stopRecorder();
        mAudioRecord = createAudioRecord();
        if( mAudioRecord == null) {
            throw new RuntimeException("Cannot instantiate SpeechRecorder");
        }

        mAudioRecord.startRecording();
        mThread =new Thread(new ProcessSpeech());
        mThread.start();
    }

    public void stopRecorder() {
        synchronized (mLock) {
            dismiss();
            if( mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
            if( mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
            mBuffer = null;
        }
    }

    public void dismiss() {
        if( mLastVoiceHeardMillis != Long.MAX_VALUE) {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mSpeechRecorderCallback.onRecordEnded();
        }
    }

    public int getSampleRate() {
        if(mAudioRecord != null) {
            return mAudioRecord.getSampleRate();
        }
        return 0;
    }

    private AudioRecord createAudioRecord() {
        for(int sampleRate : SAMPLE_RATE_CANDIDATES) {
            final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT);
            if( sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }

            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT, sizeInBytes);
            if( audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                mBuffer = new byte[sizeInBytes];
                return audioRecord;
            } else {
                audioRecord.release();
            }
        }
        return null;
    }

    private class ProcessSpeech implements Runnable {

        @Override
        public void run() {
            while(true) {
                synchronized (mLock) {
                    if( Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    final int size = mAudioRecord.read(mBuffer, 0, mBuffer.length);
                    final long now = System.currentTimeMillis();
                    if( isHearingVoice(mBuffer, size)) {
                        if( mLastVoiceHeardMillis == Long.MAX_VALUE) {
                            mVoiceStartedMillis = now;
                            mSpeechRecorderCallback.onRecordStarted();
                        }
                        mSpeechRecorderCallback.onRecordListening(mBuffer, size);
                        mLastVoiceHeardMillis = now;
                        if( now - mVoiceStartedMillis > MAX_SPEECH_LENGTH) {
                            endRecorder();
                        }
                    } else if( mLastVoiceHeardMillis != Long.MAX_VALUE) {
                        mSpeechRecorderCallback.onRecordListening(mBuffer, size);
                        if( now - mLastVoiceHeardMillis > SPEECH_TIMOUT_MILLIS) {
                            endRecorder();
                        }
                    }
                }
            }
        }
    }

    private void endRecorder() {
        mLastVoiceHeardMillis = Long.MAX_VALUE;
        mSpeechRecorderCallback.onRecordEnded();
    }

    private boolean isHearingVoice(byte[] buffer, int size) {
        for( int i = 0; i < size - 1; i++) {
            int s = buffer[i+1];
            if( s < 0) {
                s *= -1;
            }
            s <<= 8;
            s += Math.abs(buffer[i]);
            if( s > 8000) {
                return true;
            }
        }
        return false;
    }
}
