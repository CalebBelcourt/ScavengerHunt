package com.example.scavengerhunt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    private String s;
    private List<String> sub;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;
    private GeofencingClient geofencingClient;
    private ArrayList geofenceList;
    private PendingIntent geofencePendingIntent;
    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;
    private LocationCallback mLocationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for(Location location:locationResult.getLocations()){
                if(location != null){
                    Log.d("MyLocation", "("+location.getLatitude()+","+location.getLongitude()+")");
                    LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                    Marker m = mMap.addMarker(new MarkerOptions().position(loc).title("Me"));
                    animateMarker(m, loc, true);
                }
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            s = intent.getStringExtra("RESULT");
            //Toast.makeText(MapsActivity.this, s, Toast.LENGTH_SHORT).show();
            Log.d(TAG, s);
            if(s.equals("Dwelling in the geofence: Home")
            || s.equals("Dwelling in the geofence: OM East")
            || s.equals("Dwelling in the geofence: OM North")
            || s.equals("Dwelling in the geofence: OM South")){
                removeGeofencesHandler();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        geofenceList = new ArrayList<Geofence>();

        geofencePendingIntent = null;

        geofencingClient = LocationServices.getGeofencingClient(this);

        populateGeofenceList();

        addGeofencesHandler();


    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofencesHandler() {
        if (!checkPermissions()) {
            mPendingGeofenceTask = PendingGeofenceTask.ADD;
            requestPermissions();
            return;
        }
        startLocationUpdate();
        addGeofences();
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        if (!checkPermissions()) {
            Log.d(TAG, "Insufficient Permission");
            return;
        }

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MapsActivity.this, "Geofence successfully added.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Geofence successfully added.");
                        mPendingGeofenceTask = PendingGeofenceTask.NONE;
                        updateGeofencesAdded(!getGeofencesAdded());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, "Could not add geofence.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Could not add geofence.");
                    }


                });

    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofencesHandler() {
        if (!checkPermissions()) {
            mPendingGeofenceTask = PendingGeofenceTask.REMOVE;
            requestPermissions();
            return;
        }
        removeGeofences();
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void removeGeofences() {
        if (!checkPermissions()) {
            Log.d(TAG, "Insufficient Permission");
            return;
        }

        if(s.equals("Dwelling in the geofence: Home")){
            sub = geofenceRequestIDs.subList(0,1);
        }else if(s.equals("Dwelling in the geofence: OM East")){
            sub = geofenceRequestIDs.subList(1,2);
        }else if(s.equals("Dwelling in the geofence: OM North")){
            sub = geofenceRequestIDs.subList(2,3);
        }else if(s.equals("Dwelling in the geofence: OM South")){
            sub = geofenceRequestIDs.subList(3,4);
        }

        geofencingClient.removeGeofences(sub)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Toast.makeText(MapsActivity.this, "Geofence successfully removed.", Toast.LENGTH_SHORT).show();
                        Toast.makeText(MapsActivity.this, "You got 10 points!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Geofence successfully removed.");
                        mPendingGeofenceTask = PendingGeofenceTask.NONE;
                        updateGeofencesAdded(!getGeofencesAdded());
                        removeDraw();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, "Could not remove geofence.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Could not remove geofence.");
                    }
                });
    }

    private void removeDraw(){
        if(s.equals("Dwelling in the geofence: Home")){
            circle1.remove();
        }else if(s.equals("Dwelling in the geofence: OM East")){
            circle2.remove();
        }else if(s.equals("Dwelling in the geofence: OM North")){
            circle3.remove();
        }else if(s.equals("Dwelling in the geofence: OM South")){
            circle4.remove();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister local broadcast
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register local broadcast
        IntentFilter filter = new IntentFilter(Constants.CUSTOM_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            performPendingGeofenceTask();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeGeofencesHandler();
        stopLocationUpdate();
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     */
    private void performPendingGeofenceTask() {
        if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences();
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            removeGeofences();
        }
    }

    List<String> geofenceRequestIDs = new ArrayList<>();

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER |
                GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.LANDMARKS.entrySet()) {
            geofenceList.add(new Geofence.Builder().setRequestId(entry.getKey())

                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )
                    .setExpirationDuration(-1)
                    .setLoiteringDelay(Constants.GEOFENCE_DELAY_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT |
                            Geofence.GEOFENCE_TRANSITION_DWELL)
                    .build());

            geofenceRequestIDs.add(entry.getKey());
        }
    }

    /**
     * Returns true if geofences were added, otherwise false.
     */
    private boolean getGeofencesAdded() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                Constants.GEOFENCES_ADDED_KEY, false);
    }

    /**
     * Stores whether geofences were added ore removed in SharedPreferences;
     *
     * @param added Whether geofences were added or removed.
     */
    private void updateGeofencesAdded(boolean added) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.GEOFENCES_ADDED_KEY, added)
                .apply();
    }


    /**
     * Return the current state of the permissions needed.
     */

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Toast.makeText(this, "Please grant permission", Toast.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    Circle circle1;
    Circle circle2;
    Circle circle3;
    Circle circle4;


    @Override
    public void onMapReady(GoogleMap googleMap) {

        LatLng loc = new LatLng(50.671240, -120.362420);

        mMap = googleMap;
        mMap.setMinZoomPreference(18);
        mMap.setMaxZoomPreference(20);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));

        for (Map.Entry<String, LatLng> entry : Constants.LANDMARKS.entrySet()){
            LatLng fence = new LatLng(entry.getValue().latitude, entry.getValue().longitude);
            CircleOptions circleOptions = new CircleOptions()
                    .strokeColor(0x5557c4ff)
                    .fillColor(0x5557c4ff)
                    .center(fence)
                    .radius(Constants.GEOFENCE_RADIUS_IN_METERS);
            if(entry.getKey().equals("Home")){
                circle1 = mMap.addCircle(circleOptions);
            }else if(entry.getKey().equals("OM East")){
                circle2 = mMap.addCircle(circleOptions);
            }else if(entry.getKey().equals("OM North")){
                circle3 = mMap.addCircle(circleOptions);
            }else if(entry.getKey().equals("OM South")){
                circle4 = mMap.addCircle(circleOptions);
            }

        }
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdate(){
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    private void stopLocationUpdate(){
        if(mLocationCallback != null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.");
                performPendingGeofenceTask();
            } else {
                Toast.makeText(this, "Need Permission, but denied!", Toast.LENGTH_SHORT).show();
                mPendingGeofenceTask = PendingGeofenceTask.NONE;
            }
        }
    }



    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }
}
