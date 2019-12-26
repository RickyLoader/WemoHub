package com.example.homeautomation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity{

    private SparseIntArray images;

    private float beginX;
    private float endX;
    private boolean listener = false;

    private House house;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        createImageMap();
        setContentView(R.layout.activity_main);

        final Room room = new Room("Room", (ImageView) findViewById(R.id.blueprint_overlay_room), (TextView) findViewById(R.id.room_percentage));
        final Room office = new Room("Office", (ImageView) findViewById(R.id.blueprint_overlay_office), (TextView) findViewById(R.id.office_percentage));

        HashMap<String, Room> rooms = new HashMap<>();
        rooms.put(room.getName(), room);
        rooms.put(office.getName(), office);

        house = new House(
                rooms,
                (SeekBar) findViewById(R.id.dim_all_bar),
                (ImageButton) findViewById(R.id.power_on),
                (ImageButton) findViewById(R.id.power_off)
                );

        house.startMonitoring();
        house.addPreset(
                (ImageButton) findViewById(R.id.night_preset),
                new Room[]{
                     new Room("Office",1),
                     new Room("Room",1)
                }
                );
        house.addPreset(
                (ImageButton) findViewById(R.id.movie_preset),
                new Room[]{
                        new Room("Office",0),
                        new Room("Room",1)
                }
        );

        watchPlex();
    }

    private void createImageMap(){
        images = new SparseIntArray();
        images.put(0, R.id.movie_poster_1);
        images.put(1, R.id.movie_poster_2);
        images.put(2, R.id.movie_poster_3);
        images.put(3, R.id.movie_poster_4);
        images.put(4, R.id.movie_poster_5);
    }

    private int findPosition(Drawable d){
        for(int i = 0; i < images.size(); i++){
            int index = images.keyAt(i);
            ImageView image = findViewById(images.get(index));
            if(image.getDrawable() == d){
                return index;
            }
        }
        return -1;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addSwipeListener(final ImageView big){
        big.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent){
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        beginX = motionEvent.getX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        endX = motionEvent.getX();
                        float dir = beginX - endX;
                        int index = findPosition(big.getDrawable());
                        if(dir == 0){
                            big.setVisibility(View.GONE);
                        }
                        else{
                            if(dir > 0){
                                index++;
                            }
                            else{
                                index--;
                            }
                            if(index >= 0 && index < images.size()){
                                ImageView next = findViewById(images.get(index));
                                big.setImageDrawable(next.getDrawable());
                            }
                        }
                }
                return false;
            }
        });
    }

    public void posterClick(View poster){
        ImageView big = findViewById(R.id.big_poster);
        ImageView small = (ImageView) poster;

        big.setImageDrawable(small.getDrawable());
        big.setVisibility(View.VISIBLE);
        if(!listener){
            addSwipeListener(big);
            listener = true;
        }
    }

    private void watchPlex(){
        final Handler handler = new Handler();
        final Runnable monitorTask = new Runnable(){
            @Override
            public void run(){
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        ArrayList<Movie> movies = Movie.jsonToMovies(json);
                        int count = 0;
                        int index = 0;
                        if(movies.size() > 0){
                            while(count < 5){
                                if(index == movies.size() - 1){
                                    return;
                                }
                                Movie movie = movies.get(index);
                                if(!movie.isViewed()){
                                    ImageView image = findViewById(images.get(count));
                                    if(image.getTag() == null || !image.getTag().equals(movie.getImage())){
                                        image.setTag(movie.getImage());
                                        Picasso.get().load(movie.getImage()).into(image);
                                    }
                                    count++;
                                }
                                index++;
                            }
                        }

                    }
                }, Movie.getHost()).execute("/library/sections/2/recentlyAdded/" + Movie.getToken(), "GET");
                handler.postDelayed(this, 10000);
            }
        };
        monitorTask.run();
    }
}
