package com.example.homeautomation;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class Room{

    private String name;
    private ImageView dim;
    private TextView dimPercent;
    private int presetBrightness;
    private boolean presetStatus;
    private String control = "192.168.1.81/wemo/index.php/api/";
    private House house;

    public Room(String name, ImageView dim, TextView dimPercent){
        this.name = name;
        this.dim = dim;
        this.dimPercent = dimPercent;
        addToggleListener();
    }

    public void addHouse(House house){
        this.house = house;
    }

    private void addToggleListener(){
        dim.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                int percent = Integer.valueOf(dimPercent.getText().toString().replace("%", ""));
                boolean on = percent != 0;
                house.pauseMonitor();
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        house.resumeMonitor();
                    }
                }, control).execute(updateStatus(!on));
            }
        });
    }

    public Room(String name, int brightness){
        if(brightness == 0){
            this.presetStatus = false;
        }
        else{
            this.presetStatus = true;
        }
        this.presetBrightness = brightness;
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public int getPresetBrightness(){
        return presetBrightness;
    }

    public void updatePercentage(int progress, int min){
        int percent = min;
        if(progress > percent){
            double initial = Math.ceil((progress / (255d / 100d)));
            percent = (int) initial;
        }
        dimPercent.setText(percent + "%");
    }

    public String[] updateStatus(boolean on){
        String status = "on";
        int brightness = 255;
        if(!on){
            status = "off";
            brightness = 0;
        }
        updateDim(brightness);
        updatePercentage(brightness, 0);
        return new String[]{"control/status/" + name + "/" + status, "GET"};
    }

    public boolean getPresetStatus(){
        return presetStatus;
    }

    public void updateDim(int brightness){
        float opacity = Math.abs(0.90f - ((float) brightness / 255));
        dim.setAlpha(opacity);
    }

    public String[] setBrightness(int brightness, boolean dim){
        updateDim(brightness);
        if(dim && brightness == 0){
            brightness = 1;
        }
        updatePercentage(brightness, 1);
        String body = "{\"device\":\"" + name + "\",\"brightness\":" + brightness + "}";
        return new String[]{"control/brightness/update", "UPDATE", body};
    }
}
