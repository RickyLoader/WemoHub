package com.example.homeautomation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Half;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SeekBar allDimBar = findViewById(R.id.dim_all_bar);
        final Room room = new Room("all", (ImageView) findViewById(R.id.blueprint_overlay),allDimBar);
        setSeekListener(room);
        setButtonListener(room, (ImageButton) findViewById(R.id.power_off), false);
        setButtonListener(room, (ImageButton) findViewById(R.id.power_on), true);
        //startMonitoring(room);
    }

    private void startMonitoring(final Room room){
        new ApiRequest(new ApiRequest.ApiResponse(){
            @Override
            public void response(String json){
                int brightness = Integer.valueOf(json);
                Log.e("dave",""+brightness);
                updatePercentage(brightness,0);
                room.getDimBar().setProgress(brightness);
                startMonitoring(room);
            }
        }).execute(room.getStatus());
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
                new ApiRequest(null).execute(room.setBrightness(brightness, false));
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
                new ApiRequest(null).execute(room.setBrightness(seekBar.getProgress(), true));
            }
        });
    }

    private void updatePercentage(int progress, int min){
        TextView percentIndicator = findViewById(R.id.scroll_percentage);
        int percent = min;
        if(progress>percent){
            double initial = Math.ceil((progress/(255d/100d)));
            percent = (int) initial;
        }
        percentIndicator.setText(percent+"%");
    }

    private void giveFeedback(){
        Vibrator vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(50);
    }
}
