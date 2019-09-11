package com.example.homeautomation;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Half;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity{

    private String destination = "192.168.1.72";
    HashMap<Integer, Integer> images;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        createIDMap();
        setContentView(R.layout.activity_main);
        SeekBar allDimBar = findViewById(R.id.dim_all_bar);
        final Room room = new Room("all", (ImageView) findViewById(R.id.blueprint_overlay), allDimBar);
        setSeekListener(room);
        setButtonListener(room, (ImageButton) findViewById(R.id.power_off), false);
        setButtonListener(room, (ImageButton) findViewById(R.id.power_on), true);
        //startMonitoring(room);
        watchPlex();
    }

    private void createIDMap(){
        images = new HashMap<>();
        images.put(0, R.id.movie_poster_1);
        images.put(1, R.id.movie_poster_2);
        images.put(2, R.id.movie_poster_3);
        images.put(3, R.id.movie_poster_4);
        images.put(4, R.id.movie_poster_5);
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
                        if(movies.size() > 0){
                            for(int i = 0; i < 5; i++){
                                Movie movie = movies.get(i);
                                ImageView image = findViewById(images.get(i));
                                if(image.getTag() == null || !image.getTag().equals(movie.getImage())){
                                    image.setTag(movie.getImage());
                                    Picasso.get().load(movie.getImage()).into(image);
                                }
                            }
                        }

                    }
                }, Movie.getHost(), false).execute("library/sections/1/recentlyAdded/" + Movie.getToken(), "GET");
                handler.postDelayed(this, 5000);
            }
        };
        monitorTask.run();
    }

    private void startMonitoring(final Room room){
        final Handler handler = new Handler();
        final Runnable monitorTask = new Runnable(){
            @Override
            public void run(){
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        int brightness = Integer.valueOf(json.trim());
                        updatePercentage(brightness, 0);
                        room.setBrightness(brightness, false);
                        room.getDimBar().setProgress(brightness);
                    }
                }, destination, true).execute(room.getStatus());
                handler.postDelayed(this, 5000);
            }
        };
        monitorTask.run();
    }

    private void setButtonListener(final Room room, ImageButton button, final boolean on){
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                giveFeedback();
                int brightness = 255;
                if(!on){
                    brightness = 0;
                }
                updatePercentage(brightness, 0);
                new ApiRequest(null, destination, true).execute(room.setBrightness(brightness, false));
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
                giveFeedback();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                giveFeedback();
                new ApiRequest(null, destination, true).execute(room.setBrightness(seekBar.getProgress(), true));
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
