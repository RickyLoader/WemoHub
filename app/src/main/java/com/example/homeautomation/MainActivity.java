package com.example.homeautomation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Half;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends Activity{

    private String destination = "192.168.1.81";
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
                }, Movie.getHost(), false).execute("library/sections/2/recentlyAdded/" + Movie.getToken(), "GET");
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
                }, "192.168.1.65/wemo/index.php/api", false);
                if(run){
                    checkBrightness.execute(room.getStatus());
                }
                wemoHandler.postDelayed(this, 8000);
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
                }, destination, true).execute(room.updateStatus(on));
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
                //giveFeedback();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                //giveFeedback();
                pauseMonitor();
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        resumeMonitor();
                    }
                }, destination, true).execute(room.setBrightness(seekBar.getProgress(), true));
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

    private void giveFeedback(){
        Vibrator vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(50);
    }

    public void openSettings(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update API Destination");
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View dialogView = layoutInflater.inflate(R.layout.update_endpoint_dialog, null);
        builder.setView(dialogView);
        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.create();
        dialog.show();
        final EditText endpoint = dialog.findViewById(R.id.endpoint);
        endpoint.setText(destination);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                destination = endpoint.getText().toString();
                dialog.dismiss();
            }
        });
    }
}
