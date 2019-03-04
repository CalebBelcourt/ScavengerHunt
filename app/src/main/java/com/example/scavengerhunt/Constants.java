package com.example.scavengerhunt;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

public class Constants {

    public static final int GEOFENCE_EXPIRATION_IN_MILLISECONDS = 10000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 20;

    public static final HashMap<String, LatLng> LANDMARKS = new     HashMap<String, LatLng>();
    static {
        LANDMARKS.put("OM East", new LatLng(50.671214, -120.361587));

        LANDMARKS.put("OM North", new LatLng(50.671691, -120.362967));

        LANDMARKS.put("OM South", new LatLng(50.670742, -120.362959));
    }
}