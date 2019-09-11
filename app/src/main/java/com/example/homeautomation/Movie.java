package com.example.homeautomation;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class Movie implements Comparable<Movie>{
    private String title;
    private Date added;
    private String image;
    private static String host = "192.168.1.138:32400";
    private static String token = "?X-Plex-Token=CfsgymkTZzteGH78at3f";

    public Movie(String title, Date added, String image){
        this.title = title;
        this.added = added;
        this.image = image;
    }

    public String getTitle(){
        return title;
    }

    public String getImage(){
        return "http://" + host + image + token;
    }

    public static String getHost(){
        return host;
    }

    public static String getToken(){
        return token;
    }

    public static ArrayList<Movie> jsonToMovies(String json){
        ArrayList<Movie> movies = new ArrayList<>();
        try{
            JSONArray arr = new JSONObject(json).getJSONObject("MediaContainer").getJSONArray("Metadata");
            for(int i = 0; i < arr.length(); i++){
                JSONObject o = arr.getJSONObject(i);
                Date date = new Date();
                date.setTime(Long.valueOf(o.getString("addedAt")) * 1000);
                Movie movie = new Movie(o.getString("title"), date, o.getString("thumb"));
                movies.add(movie);
            }
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        Collections.sort(movies);
        return movies;
    }

    public void getSummary(){
        Log.e("dave", title + " - " + "added at " + added);
    }

    public Date getAdded(){
        return added;
    }

    @Override
    public int compareTo(Movie movie){
        return movie.getAdded().compareTo(added);
    }
}
