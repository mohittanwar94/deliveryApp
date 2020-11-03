package com.ezymd.restaurantapp.delivery.utils;

import java.util.HashMap;
import java.util.Map;

public class BaseRequest {
    public String accessToken;
    public Map<String, String> paramsMap;

    public BaseRequest() {
        paramsMap = new HashMap<>();
        accessToken="";
    }

    public BaseRequest(UserInfo userInfo) {
        accessToken = userInfo.getAccessToken();
        paramsMap = new HashMap<>();
        paramsMap.put("lat", userInfo.getLat());
        paramsMap.put("long", userInfo.getLang());

    }
}
