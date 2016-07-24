package com.app.coolweather.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.coolweather.R;
import com.app.coolweather.db.CoolWeatherDB;
import com.app.coolweather.service.AutoUpdateService;
import com.app.coolweather.util.HttpCallbackListener;
import com.app.coolweather.util.HttpUtil;
import com.app.coolweather.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class WeatherActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 用户获取位置信息
     */
    private LocationManager locationManager;
    private String provider;
    private Location location;
    private LinearLayout weatherInfoLayout;
    /**
     * 用于显示城市名
     */
    private TextView cityNameText;

    /**
     * 用于显示天气描述信息
     */
    private TextView healthText;
    /**
     * 用于显示气温
     */
    private TextView tempText;


    /**
     * 切换城市按钮
     */
    private Button switchCity;
    /**
     * 更新天气按钮
     */
    private Button refreshWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);

        getSupportActionBar().hide();//继承AppCompatActivity隐藏标题栏方法
        // 初始化各控件
        weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
        cityNameText = (TextView) findViewById(R.id.city_name); //城市名

        healthText = (TextView) findViewById(R.id.health_tv);//天气描述
        tempText = (TextView) findViewById(R.id.temp);//最低温度


        switchCity = (Button) findViewById(R.id.switch_city);//切换城市按钮
        refreshWeather = (Button) findViewById(R.id.refresh_weather);//刷新天气按钮
        String countyName = getIntent().getStringExtra("county_name");

        CoolWeatherDB coolWeatherDB = CoolWeatherDB.getInstance(this);

        if (!TextUtils.isEmpty(countyName)) {
            // 有县级代号时就去查询天气
            queryWeatherInfo(countyName);
            weatherInfoLayout.setVisibility(View.INVISIBLE);
            cityNameText.setVisibility(View.INVISIBLE);

        } else {
            // 没有县级代号时就直接显示本地天气

            showWeather();
        }
        switchCity.setOnClickListener(this);
        refreshWeather.setOnClickListener(this);
        /*//实例化广告条
        AdView adView = new AdView(this, AdSize.FIT_SCREEN);
        //获取要嵌入广告条的布局
        LinearLayout adLayout=(LinearLayout)findViewById(R.id.adLayout);
        //将广告条加入到布局中
        adLayout.addView(adView);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            locationManager.removeUpdates(locationListener);
        }
    }

    private void getLocInfo() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providerList = locationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(this, "No Location provider to use", Toast.LENGTH_SHORT).show();
        }

        //权限检验
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        location = locationManager.getLastKnownLocation(provider);

        String address = "http://gc.ditu.aliyun.com/regeocoding?l="+location.getLatitude()+","+location.getLongitude()+"&type=010";
        queryFromServer(address, "addrInfo");
    }



    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_city:
                Intent intent = new Intent(this, ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.refresh_weather:

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String cityName = prefs.getString("city_name", "");
                if (!TextUtils.isEmpty(cityName)) {
                    queryWeatherInfo(cityName);
                }
                break;
            default:
                break;
        }
    }

   /* *//**
     * 查询县级代号所对应的天气代号。
     *//*
    private void queryWeatherCode(String countyCode) {
        String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
        queryFromServer(address, "countyCode");
    }*/

    /**
     * 查询天气代号所对应的天气。
     */
    private void queryWeatherInfo(String addrName) {
        String address = "http://wthrcdn.etouch.cn/weather_mini?city=" + addrName;
        queryFromServer(address, "addrName");
    }

    /**
     * 根据传入的地址和类型去向服务器查询天气代号或者天气信息。
     */
    private void queryFromServer(final String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {
                if ("addrInfo".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        String addrName = getAddrName(response);
                        if(addrName == null){
                            return;
                        }
                        queryWeatherInfo(addrName);
                    }
                } else if ("addrName".equals(type)) {
                    // 处理服务器返回的天气信息
                    Utility.handleWeatherResponse(WeatherActivity.this, response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });*/
            }
        });
    }

    private String getAddrName(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray resultJsonArray = jsonObject.getJSONArray("addrList");
            JSONObject resultJsonObject = resultJsonArray.getJSONObject(0);
            String addrInfo = resultJsonObject.getString("admName");
            String addrName = addrInfo.substring(0,1);
            return addrName;


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从SharedPreferences文件中读取存储的天气信息，并显示到界面上。
     */
    private void showWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        cityNameText.setText( prefs.getString("city_name", ""));
        tempText.setText(prefs.getString("temp", "")+"℃");
        healthText.setText("\t\t"+prefs.getString("health", ""));

        weatherInfoLayout.setVisibility(View.VISIBLE);
        cityNameText.setVisibility(View.VISIBLE);
        showforecast(prefs);





        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    private void showforecast(SharedPreferences prefs) {
        ImageView iv1 = (ImageView) findViewById(R.id.f1_iv);
        TextView tv1_date = (TextView) findViewById(R.id.f1_date);
        TextView tv1_temp = (TextView) findViewById(R.id.f1_temp);
        tv1_date.setText(prefs.getString("ydate",""));
        tv1_temp.setText(prefs.getString("ylow","").substring(3)+"/"+
                prefs.getString("yhigh","").substring(3));
        getIcon(iv1,prefs.getString("type",""));


        ImageView iv2 = (ImageView) findViewById(R.id.f2_iv);
        TextView tv2_date = (TextView) findViewById(R.id.f2_date);
        TextView tv2_temp = (TextView) findViewById(R.id.f2_temp);
        tv2_date.setText(prefs.getString("date1",""));
        tv2_temp.setText(prefs.getString("low1","").substring(3)+"/"+
                prefs.getString("high1","").substring(3));
        getIcon(iv2,prefs.getString("type1",""));

        ImageView iv3 = (ImageView) findViewById(R.id.f3_iv);
        TextView tv3_date = (TextView) findViewById(R.id.f3_date);
        TextView tv3_temp = (TextView) findViewById(R.id.f3_temp);
        tv3_date.setText(prefs.getString("date2",""));
        tv3_temp.setText(prefs.getString("low2","").substring(3)+"/"+
                prefs.getString("high2","").substring(3));
        getIcon(iv3,prefs.getString("type2",""));

        ImageView iv4 = (ImageView) findViewById(R.id.f4_iv);
        TextView tv4_date = (TextView) findViewById(R.id.f4_date);
        TextView tv4_temp = (TextView) findViewById(R.id.f4_temp);
        tv4_date.setText(prefs.getString("date3",""));
        tv4_temp.setText(prefs.getString("low3","").substring(3)+"/"+
                prefs.getString("high3","").substring(3));
        getIcon(iv4,prefs.getString("type3",""));

        ImageView iv5 = (ImageView) findViewById(R.id.f5_iv);
        TextView tv5_date = (TextView) findViewById(R.id.f5_date);
        TextView tv5_temp = (TextView) findViewById(R.id.f5_temp);
        tv5_date.setText(prefs.getString("date4",""));
        tv5_temp.setText(prefs.getString("low4","").substring(3)+"/"+
                prefs.getString("high4","").substring(3));
        getIcon(iv5,prefs.getString("type4",""));
    }


    private void getIcon(ImageView iv,String type){


        switch (type){
            case "晴":
                iv.setImageResource(R.mipmap.w100);
                break;
            case "多云":
                iv.setImageResource(R.mipmap.w101);
                break;
            case "阵雨":
                iv.setImageResource(R.mipmap.w300);
                break;
            case "暴雨":
                iv.setImageResource(R.mipmap.w310);
                break;
            case "雷阵雨":
                iv.setImageResource(R.mipmap.w302);
                break;
            case "小雨":
                iv.setImageResource(R.mipmap.w305);
                break;
            case "中雨":
                iv.setImageResource(R.mipmap.w306);
                break;
            case "大雨":
                iv.setImageResource(R.mipmap.w307);
                break;
            case "晴间多云":
                iv.setImageResource(R.mipmap.w103);
                break;
            case "阴":
                iv.setImageResource(R.mipmap.w104);
                break;
            default:
                iv.setImageResource(R.mipmap.ic_launcher);
                break;
        }
       /* if(type.equals("晴")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("多云")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("阵雨")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("暴雨")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("雷阵雨")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("小雨")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("中雨")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("大雨")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("晴间多云")){
            iv.setImageResource(R.mipmap.100);
        }else if(type.equals("阴")){
            iv.setImageResource(R.mipmap.100);
        }else{
            iv.setImageResource(R.mipmap.100);
        }*/
    }
}
