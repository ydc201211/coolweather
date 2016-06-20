package com.app.coolweather.util;

/**
 * Created by ydc on 2016/6/20.
 */
public interface HttpCallbackListener {
    void onFinish(String response);

    void onError(Exception e);
}
