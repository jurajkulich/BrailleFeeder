package com.example.android.braillefeeder.data.dao;

import com.example.android.braillefeeder.data.model.Article;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface ArticleDao {

    @Insert
    void insert(Article article);

    @Query("DELETE FROM articles_table")
    void deleteAll();

    @Query("SELECT * FROM articles_table")
    List<Article> getAllArticles();
}
