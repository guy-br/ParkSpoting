package com.example.guybr.parkspoting;

/**
 * Created by guybr on 4/7/2015.
 */
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.*;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends Activity implements LocationListener {
    protected LocationManager locationManager;
    private double latitude = 0;
    private double longitude = 0;
    private ServerRequest sr;
    ProgressDialog dialog;
    GoogleMap googleMap;
    ImageButton centerLocationBtn, postParkBtn, findParkBtn;
    //TextView t1 = (TextView)findViewById(R.id.textView);
    //TextView t2 = (TextView)findViewById(R.id.textView2);
    private static final String TAG = "myApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent myIntent = getIntent();
        final String token = myIntent.getStringExtra("firstKeyName");
        setContentView(R.layout.activity_main);
        sr = new ServerRequest();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 1, 1, this);

        Location userLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
        //t1.setText(userLocation.toString());
        latitude = userLocation.getLatitude();
        longitude = userLocation.getLongitude();
        centerLocationBtn = (ImageButton)findViewById(R.id.centerLocationBtn);
        postParkBtn = (ImageButton)findViewById(R.id.postParkBtn);
        findParkBtn = (ImageButton)findViewById(R.id.findParkBtn);

        findParkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                longitude = myLocation.getLongitude();
                latitude = myLocation.getLatitude();
                List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                pairs.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
                pairs.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
                ServerRequest srv = new ServerRequest();
                JSONObject json = srv.getJSON("http://10.100.102.13:8080/findParkings", pairs);
                Log.v(TAG, "Holy Shit Balls");

                //if (json.isNull("longitude")) {
                    //Print to screen TOOODODODODODODOD
                //    Log.v(TAG, "null you fucker");
                //}
                //else {
                    JSONArray parkings = null;
                    try {
                        parkings = json.getJSONArray("parkings");
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ParkingSpot[] parkinArr = new ParkingSpot[parkings.length()];
                    for (int i = 0; i < parkings.length(); i++) {
                        try {
                            JSONArray[] locationParking = new JSONArray[parkings.length()];
                            locationParking[i] = parkings.getJSONObject(i).getJSONArray("location");
                            parkinArr[i] = new ParkingSpot(locationParking[i].getDouble(1), locationParking[i].getDouble(0));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.v(TAG, json.toString());
                    }
                createMapView();
                addParkingMarker(parkinArr);
                }
            //}
        });

        postParkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {

                Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                longitude = myLocation.getLongitude();
                latitude = myLocation.getLatitude();
                List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                pairs.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
                pairs.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
                pairs.add(new BasicNameValuePair("token", token));
                JSONObject json = sr.getJSON("http://10.100.102.13:8080/location", pairs);
            }
        });

        centerLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                googleMap.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude) , 15.5f) );
            }
        });

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 10000,
            1, this);

        }

        else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 10000,
                    1, this);
        }
        else
        {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "Enable Location", Toast.LENGTH_LONG).show();
        }
        createMapView();
        addMyLocationMarker();
    }

    @Override
    public void onLocationChanged(Location location) {
        dialog.show();
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        addMyLocationMarker();
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    /**
     * Initialises the mapview
     */
    private void createMapView(){
        /**
         * Catch the null pointer exception that
         * may be thrown when initialising the map
         */
        try {
            if(null == googleMap){
                googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                        R.id.mapView)).getMap();
                if(null != googleMap)
                {
                    googleMap.moveCamera( CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude) , 15.5f) );

                }

                /**
                 * If the map is still null after attempted initialisation,
                 * show an error to the user
                 */
                if(null == googleMap) {
                    Toast.makeText(getApplicationContext(),
                            "Error creating map",Toast.LENGTH_SHORT).show();
                }
            }
        } catch (NullPointerException exception){
            Log.e("mapApp", exception.toString());
        }
    }


    /**
     * Adds a marker on the map for every parking that is
     * within X radius from user.
     */
    private void addParkingMarker(ParkingSpot[] i_parkArr) {
        if(googleMap != null)
        {
            for(int i = 0; i < i_parkArr.length; i++)
            {
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(i_parkArr[i].m_Lat , i_parkArr[i].m_Lon))
                        .title("Parking Spot")
                        .draggable(false)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.parking_marker))
                        );
            }
        }
    }

    /**
     * Adds the user location marker to the map
     */
    private void addMyLocationMarker(){

        /** Make sure that the map has been initialised **/
        if(null != googleMap){
            googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latitude, longitude))
                            .title("My Location")
                            .draggable(false)
            );
        }
    }

    private class ParkingSpot {

        public double m_Lat;
        public double m_Lon;

        public ParkingSpot(double i_lat, double i_lon)
        {
            this.m_Lat = i_lat;
            this.m_Lon = i_lon;
        }
    }
}
