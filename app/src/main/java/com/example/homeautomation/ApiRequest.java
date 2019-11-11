package com.example.homeautomation;

import android.os.AsyncTask;
import android.util.Log;


import java.net.URL;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiRequest extends AsyncTask<String, Void, String>{

    public interface ApiResponse{
        void response(String json);
    }

    private ApiResponse apiResponse;
    private String destination;

    public ApiRequest(ApiResponse apiResponse, String destination){
        this.apiResponse = apiResponse;
        this.destination = destination;
    }

    @Override
    protected void onPreExecute(){
    }

    @Override
    protected String doInBackground(String... args){
        try{
            OkHttpClient client = new OkHttpClient();
            Response response;

            String apiRequest = args[0];
            String method = args[1];
            String location = "http://" + destination + apiRequest;

            URL url = new URL(location);
            Request.Builder builder = new Request.Builder().url(url).addHeader("Accept", "application/json");

            switch(method){
                case "UPDATE":
                    String query = args[2];
                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"), query
                    );
                    builder.post(body);
                    break;
                default:
                    break;
            }
            response = client.newCall(builder.build()).execute();
            if(response.body() != null){
                return response.body().string();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String data){
        if(apiResponse != null){
            apiResponse.response(data);
        }
    }
}
