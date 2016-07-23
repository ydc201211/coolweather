package com.app.coolweather.model;

/**
 * Created by ydc on 2016/7/20.
 */
public class Weather {
    private String windDirection;
    private String windPower;
    private String high;
    private String low;
    private String type;
    private String date;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public String getWindPower() {
        return windPower;
    }

    public void setWindPower(String windPower) {
        this.windPower = windPower;
    }

    public String getHigh() {
        return high;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    public String getLow() {
        return low;
    }

    public void setLow(String low) {
        this.low = low;
    }



    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


}
