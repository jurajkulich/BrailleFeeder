package com.example.android.braillefeeder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechGrpc;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;

/**
 * Created by juraj on 7/28/18.
 */

public class SpeechToText extends Service {

    public interface SpeechToTextListener {
        void onSpeechRecognized(String text, boolean isFinal);
    }

    private static final String TAG = "SpeechToText";

    // Konstanty pre pristup k Shared preferece pamati
    private static final String PREFERENCES = "shared_preferences";
    private static final String PREFERENCES_TOKEN_VALUE = "preferences_token_value";
    private static final String PREFERENCES_TOKEN_EXPIRATION = "preferences_token_expiration";

    private final SpeechBinder mBinder = new SpeechBinder();
    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");
    private static final String HOSTNAME = "speech.googleapis.com";
    private static final int PORT = 443;

    private static final int TOKEN_EXPIRATION_TOLERANCE = 30*60*1000;
    private static final int TOKEN_FETCH_DELAY = 60*1000;

    private volatile AccessTokenTask mAccessTokenTask;

    private final ArrayList<SpeechToTextListener> mListeners = new ArrayList<>();
    private SpeechGrpc.SpeechStub mApi;
    private static Handler mHandler;

    private final StreamObserver<StreamingRecognizeResponse> mStreamObserver
            = new StreamObserver<StreamingRecognizeResponse>() {
        @Override
        public void onNext(StreamingRecognizeResponse value) {
            Log.d("StreamObserver", "onNext" );
            String text = null;
            boolean isFinal = false;

            if( value.getResultsCount() > 0) {
                final StreamingRecognitionResult result = value.getResults(0);
                isFinal = result.getIsFinal();
                if( result.getAlternativesCount() > 0) {
                    final SpeechRecognitionAlternative alternatives = result.getAlternatives(0);
                    text = alternatives.getTranscript();
                }
            }
            if( text != null) {
                for( SpeechToTextListener listener : mListeners) {
                    listener.onSpeechRecognized(text, isFinal);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }

        @Override
        public void onCompleted() {

        }
    };

    private StreamObserver<StreamingRecognizeRequest> mRequestObserver;

    public static SpeechToText from(IBinder binder) {
        Log.e("SpeechToText", "fromIBinder");
        return ((SpeechBinder) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("SpeechToText", "onCreate");
        mHandler = new Handler();
        fetchAccessToken();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler = null;

        if( mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mApi = null;
        }
    }


    private class SpeechBinder extends Binder {
        SpeechToText getService() {
            return SpeechToText.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private String getDefaultLanguageCode() {
        final Locale locale = Locale.getDefault();
        final StringBuilder language = new StringBuilder(locale.getLanguage());
        final String country = locale.getCountry();
        if (!TextUtils.isEmpty(country)) {
            language.append("-");
            language.append(country);
        }
        return language.toString();
    }

    public void addListener( SpeechToTextListener listener) {
        mListeners.add(listener);
    }

    public void removeListener( SpeechToTextListener listener) {
        mListeners.remove(listener);
    }

    private void fetchAccessToken() {
        if( mAccessTokenTask != null) {
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    public void startRecognizing(int sampleRate) {
        Log.e(TAG, "Started recognizing.");
        if( mApi == null) {
            Log.e(TAG, "API not ready. Ignoring the request.");
            return;
        }

        mRequestObserver = mApi.streamingRecognize(mStreamObserver);
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(StreamingRecognitionConfig.newBuilder()
                        .setConfig(RecognitionConfig.newBuilder()
                                .setLanguageCode(getDefaultLanguageCode())
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .build())
                        .setInterimResults(true)
                        .setSingleUtterance(true)
                        .build())
                .build());
    }

    public void recognize(byte[] data, int size) {
        if (mRequestObserver == null) {
            return;
        }
//        Log.d(TAG, "Call the streaming recognition API");
        // Call the streaming recognition API
        mRequestObserver.onNext(StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build());
    }

    public void finishRecognizing() {
        if (mRequestObserver == null) {
            return;
        }
        mRequestObserver.onCompleted();
        mRequestObserver = null;
    }

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(Void... voids) {
            final SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
            String tokenValue  = sharedPreferences.getString(PREFERENCES_TOKEN_VALUE, null);
            long tokenExpiration = sharedPreferences.getLong(PREFERENCES_TOKEN_EXPIRATION, -1);

            if (tokenValue != null && tokenExpiration > 0) {
                if (tokenExpiration > System.currentTimeMillis() + TOKEN_EXPIRATION_TOLERANCE) {
                    return new AccessToken(tokenValue, new Date(tokenExpiration));
                }
            }

            final InputStream inputStream = getResources().openRawResource(R.raw.credentials);
            try {
                final GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream).createScoped(SCOPE);
                final AccessToken accessToken = credentials.refreshAccessToken();

                sharedPreferences.edit()
                        .putString(PREFERENCES_TOKEN_VALUE, accessToken.getTokenValue())
                        .putLong(PREFERENCES_TOKEN_EXPIRATION, accessToken.getExpirationTime().getTime())
                        .apply();
                return accessToken;
            } catch ( IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
            mAccessTokenTask = null;
            final ManagedChannel channel = new OkHttpChannelProvider()
                    .builderForAddress(HOSTNAME, PORT)
                    .nameResolverFactory(new DnsNameResolverProvider())
                    .intercept(new GoogleCredentialsInterceptor(
                            GoogleCredentials.create(accessToken).createScoped(SCOPE)))
                    .build();
            mApi = SpeechGrpc.newStub(channel);

            if( mHandler != null) {
                mHandler.postDelayed(mFetchAccessTokenRunnable, Math.max(
                        accessToken.getExpirationTime().getTime() - System.currentTimeMillis() -
                        TOKEN_FETCH_DELAY, TOKEN_EXPIRATION_TOLERANCE));
            }
        }
    }

    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
        }
    };

    private static class GoogleCredentialsInterceptor implements ClientInterceptor {

        private final Credentials credentials;
        private Metadata metadata;
        private Map<String, List<String>> lastMetadata;

        GoogleCredentialsInterceptor(Credentials credential) {
             credentials = credential;
        }


        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                final MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, final Channel next) {
            return new ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                protected void checkedStart(Listener<RespT> responseListener, Metadata headers) throws Exception {
                    Metadata cachedSaved;
                    URI uri = serviceUri(next, method);
                    synchronized (this) {
                        Map<String, List<String>> latestMetadata = getRequestMetadata(uri);
                        if (lastMetadata == null || lastMetadata != latestMetadata) {
                            lastMetadata = latestMetadata;
                            metadata = toHeaders(lastMetadata);
                        }
                        cachedSaved = metadata;
                    }
                    headers.merge(cachedSaved);
                    delegate().start(responseListener, headers);
                }
            };
        }

        private URI serviceUri(Channel channel, MethodDescriptor<?, ?> method)
                throws StatusException {
            String authority = channel.authority();
            if (authority == null) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Channel has no authority")
                        .asException();
            }
            // Always use HTTPS, by definition.
            final String scheme = "https";
            final int defaultPort = 443;
            String path = "/" + MethodDescriptor.extractFullServiceName(method.getFullMethodName());
            URI uri;
            try {
                uri = new URI(scheme, authority, path, null, null);
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI for auth")
                        .withCause(e).asException();
            }
            // The default port must not be present. Alternative ports should be present.
            if (uri.getPort() == defaultPort) {
                uri = removePort(uri);
            }
            return uri;
        }

        private URI removePort(URI uri) throws StatusException {
            try {
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), -1 /* port */,
                        uri.getPath(), uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException e) {
                throw Status.UNAUTHENTICATED
                        .withDescription("Unable to construct service URI after removing port")
                        .withCause(e).asException();
            }
        }

        private Map<String, List<String>> getRequestMetadata(URI uri) throws StatusException {
            try {
                return credentials.getRequestMetadata(uri);
            } catch (IOException e) {
                throw Status.UNAUTHENTICATED.withCause(e).asException();
            }
        }

        private static Metadata toHeaders(Map<String, List<String>> metadata) {
            Metadata headers = new Metadata();
            if (metadata != null) {
                for (String key : metadata.keySet()) {
                    Metadata.Key<String> headerKey = Metadata.Key.of(
                            key, Metadata.ASCII_STRING_MARSHALLER);
                    for (String value : metadata.get(key)) {
                        headers.put(headerKey, value);
                    }
                }
            }
            return headers;
        }
    }
}
