package com.example.android.braillefeeder.data;

import com.example.android.braillefeeder.data.remote.NewsService;
import com.example.android.braillefeeder.data.remote.RetrofitClient;

import retrofit2.Retrofit;

/**
 * Created by Juraj on 3/19/2018.
 */

public class ApiUtils {

    public static final String BASE_URL = "https://newsapi.org/v2/";

    public static NewsService getNewService() {
        return RetrofitClient.getClient(BASE_URL).create(NewsService.class);
    }
}
