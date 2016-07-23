package com.app.coolweather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.app.coolweather.db.CoolWeatherDB;
import com.app.coolweather.model.City;
import com.app.coolweather.model.County;
import com.app.coolweather.model.Province;
import com.app.coolweather.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by ydc on 2016/6/20.
 */
public class Utility {


    /**
     * 解析和处理服务器返回的省级数据
     */
    public synchronized static boolean handleProvincesResponse(
            CoolWeatherDB coolWeatherDB, String response) {
        if (!TextUtils.isEmpty(response)) {
            String[] allProvinces = response.split(",");
            if (allProvinces != null && allProvinces.length > 0) {
                for (String p : allProvinces) {
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
                    // 将解析出来的数据存储到Province表
                    coolWeatherDB.saveProvince(province);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     */
    public static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB,
                                               String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCities = response.split(",");
            if (allCities != null && allCities.length > 0) {
                for (String c : allCities) {
                    String[] array = c.split("\\|");
                    City city = new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);
                    // 将解析出来的数据存储到City表
                    coolWeatherDB.saveCity(city);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的县级数据
     */
    public static boolean handleCountiesResponse(CoolWeatherDB coolWeatherDB,
                                                 String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCounties = response.split(",");
            if (allCounties != null && allCounties.length > 0) {
                for (String c : allCounties) {
                    String[] array = c.split("\\|");
                    County country = new County();
                    country.setCountyCode(array[0]);
                    country.setCountyName(array[1]);
                    country.setCityId(cityId);
                    // 将解析出来的数据存储到County表
                    coolWeatherDB.saveCounty(country);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析服务器返回的JSON数据，并将解析出的数据存储到本地。
     */
    public static void handleWeatherResponse(Context context, String response) {
        List<Weather> list = new ArrayList<Weather>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject data = jsonObject.getJSONObject("data");
            String temp = data.getString("wendu");
            String health = data.getString("ganmao");
            String cityName = data.getString("city");

            JSONObject data1 = data.getJSONObject("yesterday");
            String yhigh = data1.getString("high");
            String ylow = data1.getString("low");
            String fl = data1.getString("fl");
            String fx = data1.getString("fx");
            String type = data1.getString("type");
            String date = data1.getString("date");


            JSONArray forecastArray = data.getJSONArray("forecast");
            for(int i = 0;i < forecastArray.length();i++ ){
                Weather w = new Weather();
                w.setHigh(forecastArray.getJSONObject(i).getString("high"));
                w.setLow(forecastArray.getJSONObject(i).getString("low"));
                w.setDate(forecastArray.getJSONObject(i).getString("date"));
                w.setWindDirection(forecastArray.getJSONObject(i).getString("fengxiang"));
                w.setWindPower(forecastArray.getJSONObject(i).getString("fengli"));
                w.setType(forecastArray.getJSONObject(i).getString("type"));
                list.add(w);
            }

            saveWeatherInfo(context, cityName,temp,health,list,yhigh,ylow,fl,fx,type,date);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将服务器返回的所有天气信息存储到SharedPreferences文件中。
     */
    public static void saveWeatherInfo(Context context, String cityName,
                                       String temp,String health,List<Weather> list,
                                       String yhigh,String ylow,String fl,String fx,String type,
                                       String date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("temp", temp);
        editor.putString("health", health);

        for(int i=0;i < list.size();i++){
            editor.putString("high" + i,list.get(i).getHigh());
            editor.putString("low" + i,list.get(i).getLow());
            editor.putString("date"+ i,list.get(i).getDate());
            editor.putString("fengxiang"+ i,list.get(i).getWindDirection());
            editor.putString("fengli"+ i,list.get(i).getWindPower());
            editor.putString("type"+i,list.get(i).getType());


        }

        editor.putString("yhigh",yhigh);
        editor.putString("ylow",ylow);
        editor.putString("fl",fl);
        editor.putString("fx",fx);
        editor.putString("type",type);
        editor.putString("ydate",date);

        editor.commit();
    }
}
