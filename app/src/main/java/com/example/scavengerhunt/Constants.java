package com.example.scavengerhunt;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

public class Constants {

    public static final int GEOFENCE_DELAY_IN_MILLISECONDS = 30*1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 20;
    public static final String RECEIVER_RESULT_DATA_KEY = "Data_Key";
    public static final String CUSTOM_INTENT = "com.example.scavengerhunt.intent.action.TEST";
    private static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";
    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    public static final HashMap<String, LatLng> LANDMARKS = new     HashMap<String, LatLng>();
    static {
        LANDMARKS.put("Home", new LatLng(50.7047745, -120.3734651));

        LANDMARKS.put("OM East", new LatLng(50.671214, -120.361587));

        LANDMARKS.put("OM North", new LatLng(50.671691, -120.362967));

        LANDMARKS.put("OM South", new LatLng(50.670742, -120.362959));
    }
}