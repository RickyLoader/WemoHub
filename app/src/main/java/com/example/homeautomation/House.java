package com.example.homeautomation;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class House{

    private HashMap<String, Room> rooms;
    private HashMap<String, ArrayList<Room>> presets;
    private SeekBar masterControl;
    private ImageButton masterOn;
    private ImageButton masterOff;
    private boolean run = true;
    private String control = "192.168.1.81/wemo/index.php/api/";
    private String monitor = "192.168.1.65/wemo/index.php/api/";

    public House(HashMap<String, Room> rooms, SeekBar masterControl, ImageButton masterOn, ImageButton masterOff){
        this.rooms = rooms;
        this.masterControl = masterControl;
        this.masterOn = masterOn;
        this.masterOff = masterOff;

        setButtonListener(masterOn, true);
        setButtonListener(masterOff, false);
        setSeekListener();
        for(Room room: rooms.values()){
            room.addHouse(this);
        }
    }

    private void setButtonListener(ImageButton button, final boolean on){
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                pauseMonitor();
                new ApiRequest(new ApiRequest.ApiResponse(){
                    @Override
                    public void response(String json){
                        resumeMonitor();
                    }
                }, control).execute(updateMasterStatus(on));
            }
        });
    }

    private String[] updateMasterStatus(boolean on){
        String status = "on";
        int brightness = 255;
        if(!on){
            status = "off";
            brightness = 0;
        }
        for(Room room : rooms.values()){
            room.updateDim(brightness);
            room.updatePercentage(brightness, 0);
        }
        masterControl.setProgress(brightness);
        return new String[]{"control/status/all/" + status, "GET"};
    }

    public void startMonitoring(){
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
                            //Log.e("dave", json);
                            for(int i = 0; i < bulbs.length(); i++){
                                JSONObject bulb = bulbs.getJSONObject(i);
                                Room room = rooms.get(bulb.getString("name"));
                                int brightness = Integer.valueOf(bulb.getString("dim"));
                                room.updatePercentage(brightness, 0);
                                room.updateDim(brightness);
                            }
                        }
                        catch(JSONException e){
                            Log.e("dave", e.getMessage());
                        }
                    }
                }, monitor);
                if(run){
                    checkBrightness.execute(getMasterStatus());
                }
                wemoHandler.postDelayed(this, 5000);
            }
        };
        wemoMonitorTask.run();
    }

    public String[] getMasterStatus(){
        return new String[]{"info/all", "GET"};
    }

    private void updateMasterPercentage(int progress, int min){
        for(Room room : rooms.values()){
            room.updatePercentage(progress, min);
        }
    }

    private String[] setMasterBrightness(int brightness, boolean dim){
        if(dim && brightness == 0){
            brightness = 1;
        }
        for(Room room : rooms.values()){
            room.updateDim(brightness);
        }
        String body = "{\"device\":\"all\",\"brightness\":" + brightness + "}";
        return new String[]{"control/brightness/update", "UPDATE", body};
    }

    private void setSeekListener(){
        masterControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                if(fromUser){
                    updateMasterPercentage(progress, 1);
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
                }, control).execute(setMasterBrightness(seekBar.getProgress(), true));
            }
        });
    }

    public void pauseMonitor(){
        run = false;
    }

    public void resumeMonitor(){
        run = true;
    }

    public void addPreset(ImageButton button, final Room[] presetOptions){
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                pauseMonitor();
                for(Room presetRoom : presetOptions){
                    Room room = rooms.get(presetRoom.getName());
                    ApiRequest update = new ApiRequest(new ApiRequest.ApiResponse(){
                        @Override
                        public void response(String json){
                            resumeMonitor();
                        }
                    }, control);

                    String[] ops;
                    if(!presetRoom.getPresetStatus()){
                        ops = room.updateStatus(false);
                    }
                    else{
                        ops = room.setBrightness(presetRoom.getPresetBrightness(), false);
                    }


                    update.execute(ops);
                }
            }
        });
    }
}
