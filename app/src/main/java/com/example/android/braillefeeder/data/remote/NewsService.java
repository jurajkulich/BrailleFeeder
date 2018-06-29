package com.example.android.braillefeeder.data.remote;

import com.example.android.braillefeeder.data.ArticleList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Juraj on 3/19/2018.
 */

public interface NewsService {

    @GET("top-headlines?country=us")
    Call<ArticleList> getResponse(@Query("apiKey") String apiKey);
}
