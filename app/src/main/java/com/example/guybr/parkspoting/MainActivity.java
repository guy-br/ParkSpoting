package com.example.guybr.parkspoting;

/**
 * Created by guybr on 4/7/2015.
 */
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
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements LocationListener {
    protected LocationManager locationManager;
    private double latitude = 0;
    private double longitude = 0;
    private ServerRequest sr;
    ProgressDialog dialog;
    GoogleMap googleMap;
    ImageButton centerLocationBtn;
    Button sendToServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent myIntent = getIntent();
        final String token = myIntent.getStringExtra("firstKeyName");
        setContentView(R.layout.activity_main);
        sr = new ServerRequest();
        sendToServer = (Button)findViewById(R.id.sendToServer);
        dialog = new ProgressDialog(MainActivity.this);
        dialog.show();
        dialog.setMessage("Getting Coordinates");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location userLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
        latitude = userLocation.getLatitude();
        longitude = userLocation.getLongitude();
        centerLocationBtn = (ImageButton)findViewById(R.id.centerLocationBtn);

        sendToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View V) {

                Location myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                longitude = myLocation.getLongitude();
                latitude = myLocation.getLatitude();
                List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                pairs.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
                pairs.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
                pairs.add(new BasicNameValuePair("token", token));
                JSONObject json = sr.getJSON("http://10.0.0.5:8080/location", pairs);
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
        addMarker();
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        dialog.show();
        latitude = location.getLatitude();
        longitude = location.getLongitude();
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
     * Adds a marker to the map
     */
    private void addMarker(){

        /** Make sure that the map has been initialised **/
        if(null != googleMap){
            googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latitude, longitude))
                            .title("Marker")
                            .draggable(true)
            );
        }
    }
}
