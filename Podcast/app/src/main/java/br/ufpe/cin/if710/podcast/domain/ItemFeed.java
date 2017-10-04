package br.ufpe.cin.if710.podcast.domain;

import android.content.ContentValues;

import java.io.Serializable;

import br.ufpe.cin.if710.podcast.db.PodcastDBHelper;

public class ItemFeed implements Serializable{
    private final String title;
    private final String link;
    private final String pubDate;
    private final String description;
    private final String downloadLink;


    public ItemFeed(String title, String link, String pubDate, String description, String downloadLink) {
        this.title = title;
        this.link = link;
        this.pubDate = pubDate;
        this.description = description;
        this.downloadLink = downloadLink;
    }

    public ItemFeed(ContentValues cv){
        this.title = (String)cv.get(PodcastDBHelper.EPISODE_TITLE);
        this.link = (String)cv.get(PodcastDBHelper.EPISODE_LINK);
        this.pubDate = (String)cv.get(PodcastDBHelper.EPISODE_DATE);
        this.description = (String)cv.get(PodcastDBHelper.EPISODE_DESC);
        this.downloadLink = (String)cv.get(PodcastDBHelper.EPISODE_DOWNLOAD_LINK);
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getPubDate() {
        return pubDate;
    }

    public String getDescription() {
        return description;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    /**converte objeto do tipo itemfeed para ser compatível com o db*/
    public ContentValues toContentValues(){
        ContentValues cv = new ContentValues();
        cv.put(PodcastDBHelper.EPISODE_DATE, this.pubDate);
        cv.put(PodcastDBHelper.EPISODE_DESC, this.description );
        cv.put(PodcastDBHelper.EPISODE_DOWNLOAD_LINK, this.downloadLink);
        cv.put(PodcastDBHelper.EPISODE_LINK, this.link);
        cv.put(PodcastDBHelper.EPISODE_TITLE, this.title);
        cv.put(PodcastDBHelper.EPISODE_FILE_URI, "");
        return cv;
    }
    @Override
    public String toString() {
        return title;
    }
}