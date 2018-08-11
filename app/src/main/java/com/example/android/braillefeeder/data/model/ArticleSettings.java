package com.example.android.braillefeeder.data.model;

public class ArticleSettings {

    private String country;
    private String source;
    private String category;
    private String about;
    private String language;

    public ArticleSettings() {}
    public ArticleSettings(String country, String source, String category, String about, String language) {
        this.country = country;
        this.source = source;
        this.category = category;
        this.about = about;
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
