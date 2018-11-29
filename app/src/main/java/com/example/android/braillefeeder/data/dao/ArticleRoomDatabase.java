package com.example.android.braillefeeder.data.dao;

import android.content.Context;

import com.example.android.braillefeeder.data.model.Article;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Article.class}, version = 1, exportSchema = false)
public abstract class ArticleRoomDatabase extends RoomDatabase {

    private static volatile ArticleRoomDatabase INSTANCE;
    public abstract ArticleDao mArticleDao();

    public static ArticleRoomDatabase getDatabase(final Context context) {
        if( INSTANCE == null) {
            synchronized (ArticleRoomDatabase.class) {
                INSTANCE = Room.databaseBuilder(context, ArticleRoomDatabase.class, "articles_database").build();
            }
        }
        return INSTANCE;
    }
}
