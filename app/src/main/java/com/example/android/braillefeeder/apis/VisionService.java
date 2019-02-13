package com.example.android.braillefeeder.apis;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.example.android.braillefeeder.MainActivity;
import com.example.android.braillefeeder.R;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VisionService {

    private static final String TAG = "VisionService";
    private static final String CLOUD_VISION_API_KEY = "AIzaSyC8brMCQq96z5INuEzCvmH0DYKUUFETizg";

    public interface VisionServiceListener {
        void onVisionCompleted(String result);
    }

    private static VisionServiceListener mVisionServiceListener;
    private static Context mContext;

    public VisionService(Context context, VisionServiceListener listener) {
        mContext = context;
        mVisionServiceListener = listener;
    }

    private Vision.Images.Annotate prepareAnnotationRequest(final Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
            @Override
            protected void initializeVisionRequest(VisionRequest<?> request) throws IOException {
                    super.initializeVisionRequest(request);
            }
        };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);
        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            com.google.api.services.vision.v1.model.Image base64EncodedImage = new com.google.api.services.vision.v1.model.Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("LABEL_DETECTION");
                labelDetection.setMaxResults(5);
                add(labelDetection);
                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                add(textDetection);
            }});

            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotate = vision.images().annotate(batchAnnotateImagesRequest);
        annotate.setDisableGZipContent(true);

        return annotate;
    }

    private static class VisionTask extends AsyncTask<Object, Void, String> {
        private Vision.Images.Annotate mAnnotateRequest;

        VisionTask(Vision.Images.Annotate annotate) {
            mAnnotateRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... objects) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mAnnotateRequest.execute();
                return getResponse(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }

        @Override
        protected void onPostExecute(String s) {
                    mVisionServiceListener.onVisionCompleted(s);
        }
    }

    public void callCloudVision(final Bitmap bitmap) {
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new VisionTask(prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
        }
    }

    private static String getResponse(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder(mContext.getString(R.string.vision_answer));

        List<EntityAnnotation> textAnnotations = response.getResponses().get(0).getTextAnnotations();
        List<EntityAnnotation> labelAnnotations = response.getResponses().get(0).getLabelAnnotations();
        if (labelAnnotations != null) {
            StringBuilder labels = new StringBuilder();
            for (EntityAnnotation label : labelAnnotations) {
               labels.append(String.format(Locale.US, "%s", label.getDescription()));
               labels.append(",");
            }
            if( MainActivity.getLocale().equals("sk")) {
//                Log.d("VisionService", "translating labels: " + MainActivity.getLocale());
                message.append(translateLabels(labels.toString()));
            } else {
//                Log.d("VisionService", "translating labels: " + MainActivity.getLocale());
                message.append(labels.toString());
            }
        } else {
            message.append(mContext.getString(R.string.vision_nothing));
        }

        if (textAnnotations != null) {
            message.append(mContext.getString(R.string.vision_text));
            message.append("\n");

            for (EntityAnnotation label : textAnnotations) {
                 message.append(String.format(Locale.US, "%.3f: %s", label.getScore(), label.getDescription()));
            }
        } else {
            message.append(mContext.getString(R.string.vision_no_text));
        }
        return message.toString();
    }

    public static String translateLabels(String labels) {
        OkHttpClient client = new OkHttpClient();
        HttpUrl url = HttpUrl.parse("https://translation.googleapis.com/language/translate/v2").newBuilder()
                .addQueryParameter("key", "AIzaSyC8brMCQq96z5INuEzCvmH0DYKUUFETizg")
                .addQueryParameter("q", labels)
                .addQueryParameter("target", "sk")
                .addQueryParameter("source", "en")
                .build();
        Log.d("traslateLabels", labels);
        Log.d("traslateLabels", url.toString());
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            try {
                JSONObject json = new JSONObject(response.body().string());
                String respon = json.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
                return respon;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
