package com.example.android.braillefeeder.data;

import com.example.android.braillefeeder.data.model.Article;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Juraj on 3/20/2018.
 */

public class ArticleList {

    @SerializedName("articles")
    private List<Article> mArticleList;

    public List<Article> getArticleList() {
        return mArticleList;
    }
}
