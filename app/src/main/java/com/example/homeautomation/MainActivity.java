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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;


public class MainActivity extends Activity{

    private String control = "192.168.1.81/wemo/index.php/api/";
    private String monitor = "192.168.1.65/wemo/index.php/api/";

    private SparseIntArray images;

    private boolean run = true;
    private float beginX;
    private float endX;
    private boolean listener = false;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        createImageMap();
        setContentView(R.layout.activity_main);
        SeekBar allDimBar = findViewById(R.id.dim_all_bar);
        final Room room = new Room("all", (ImageView) findViewById(R.id.blueprint_overlay), allDimBar);
        setSeekListener(room);
        setButtonListener(room, (ImageButton) findViewById(R.id.power_off), false);
        setButtonListener(room, (ImageButton) findViewById(R.id.power_on), true);
        setPresetListener(room, (ImageButton) findViewById(R.id.night_preset), "night", 1);
        setPresetListener(room, (ImageButton) findViewById(R.id.movie_preset), "movie", 1);
        startMonitoring(room);
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

    private void startMonitoring(final Room room){
        final Handler wemoHandler = new Handler();
        final Runnable wemoMonitorTask = new Runnable(){
            @Override
            public void run(){
                ApiRequest checkBrightness = new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        if(!run || json == null || json.isEmpty()){
                            return;
                        }
                        try{
                            JSONArray bulbs = new JSONArray(json);
                            int brightness = Math.max(
                                    Integer.valueOf(bulbs.getJSONObject(0).getString("dim")),
                                    Integer.valueOf(bulbs.getJSONObject(1).getString("dim"))
                            );
                            updatePercentage(brightness, 0);
                            room.updateDim(brightness);
                        }
                        catch(JSONException e){
                        }
                    }
                }, monitor);
                if(run){
                    checkBrightness.execute(room.getStatus());
                }
                wemoHandler.postDelayed(this, 5000);
            }
        };
        wemoMonitorTask.run();
    }

    private void pauseMonitor(){
        run = false;
    }

    private void resumeMonitor(){
        run = true;
    }

    private void setButtonListener(final Room room, ImageButton button, final boolean on){
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                pauseMonitor();
                int brightness = 255;
                if(!on){
                    brightness = 0;
                }
                updatePercentage(brightness, 0);
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        resumeMonitor();
                    }
                }, control).execute(room.updateStatus(on));
            }
        });
    }

    private void setPresetListener(final Room room, final ImageButton button, final String preset, final int brightness){
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                pauseMonitor();
                updatePercentage(brightness, 1);
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        resumeMonitor();
                    }
                }, control).execute(room.activatePreset(preset, brightness));
            }
        });
    }

    private void setSeekListener(final Room room){
        room.getDimBar().setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if(fromUser){
                    updatePercentage(progress, 1);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar){
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                pauseMonitor();
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        resumeMonitor();
                    }
                }, control).execute(room.setBrightness(seekBar.getProgress(), true));
            }
        });
    }

    private void updatePercentage(int progress, int min){
        TextView percentIndicator = findViewById(R.id.scroll_percentage);
        int percent = min;
        if(progress > percent){
            double initial = Math.ceil((progress / (255d / 100d)));
            percent = (int) initial;
        }
        percentIndicator.setText(percent + "%");
    }
}
