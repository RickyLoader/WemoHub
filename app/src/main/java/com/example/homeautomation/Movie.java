package com.example.homeautomation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

public class Movie implements Comparable<Movie>{
    private String title;
    private Calendar added;
    private String image;
    private boolean viewed;
    private boolean current;
    private int position;
    private String summary;
    private static String host = "192.168.1.138:32400";
    private static String token = "?X-Plex-Token=CfsgymkTZzteGH78at3f";

    public Movie(String title, Calendar added, String image, boolean viewed, boolean current, String summary){
        this.title = title;
        this.added = added;
        this.image = image;
        this.viewed = viewed;
        this.current = current;
        this.summary = summary;
    }

    public boolean isCurrent(){
        return current;
    }

    public String getSummary(){
        return summary;
    }

    public int getPosition(){
        return position;
    }

    public void setPosition(int position){
        this.position = position;
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
                if(!o.has("year")){
                    continue;
                }
                Calendar date = Calendar.getInstance();
                date.setTimeInMillis(o.getLong("addedAt") * 1000);
                Calendar available = Calendar.getInstance();
                available.setTimeInMillis(System.currentTimeMillis());
                int releaseYear = o.getInt("year");
                boolean current = (available.get(Calendar.YEAR) - releaseYear) == 0;
                boolean viewed = o.has("lastViewedAt");
                Movie movie = new Movie(o.getString("title"), date, o.getString("thumb"), viewed, current, o.getString("summary"));
                movies.add(movie);
            }
        }
        catch(Exception e){
            return movies;
        }
        Collections.sort(movies);
        return movies;
    }

    public boolean isViewed(){
        return viewed;
    }

    public Calendar getAdded(){
        return added;
    }

    @Override
    public int compareTo(Movie movie){
        return movie.getAdded().compareTo(getAdded());
    }
}
