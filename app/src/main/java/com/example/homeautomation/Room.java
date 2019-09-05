package com.example.homeautomation;

import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

public class Room{

    private String name;
    private ImageView dim;
    private SeekBar dimBar;

    public Room(String name, ImageView dim, SeekBar dimBar){
        this.name = name;
        this.dim = dim;
        this.dimBar = dimBar;
    }

    public String getName(){
        return name;
    }

    public SeekBar getDimBar(){
        return dimBar;
    }

    private void updateDim(int brightness){
        float opacity = Math.abs(0.90f - ((float) brightness / 255));
        dim.setAlpha(opacity);
        if(dimBar.getProgress()!=brightness){
            dimBar.setProgress(brightness);
        }
    }

    public String[] setBrightness(int brightness, boolean dim){
        updateDim(brightness);
        if(dim && brightness==0){
            brightness = 1;
        }
        String body = "{\"device\":\"" + name + "\",\"brightness\":" + brightness + "}";
        return new String[]{"brightness/update", "UPDATE", body};
    }

    public String[] updateStatus(boolean on){
        String status = "on";
        int brightness = 255;
        if(!on){
            status = "off";
            brightness = 0;
        }
        updateDim(brightness);
        return new String[]{status + "/" + name, "GET"};
    }

    public String[] getStatus(){
        return new String[]{"status/" + name, "GET"};
    }
}
