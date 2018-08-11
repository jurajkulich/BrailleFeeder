package com.example.android.braillefeeder.data;

import com.example.android.braillefeeder.remote.NewsService;
import com.example.android.braillefeeder.remote.RetrofitClient;

/**
 * Created by Juraj on 3/19/2018.
 */

public class ApiUtils {

    public static final String BASE_URL = "https://newsapi.org/v2/";

    public static NewsService getNewService() {
        return RetrofitClient.getClient(BASE_URL).create(NewsService.class);
    }
}
