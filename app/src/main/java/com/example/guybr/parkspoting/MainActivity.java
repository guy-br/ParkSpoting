package com.example.guybr.parkspoting;

/**
 * Created by guybr on 4/7/2015.
 */
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;


public class MainActivity extends FragmentActivity
        implements
        ResultCallback<Status>,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {


    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";


    //protected LocationManager locationManager;
    private double latitude = 0;
    private double longitude = 0;
    private double dstLongitude = 0;
    private double dstLatitude = 0;
    private ServerRequest sr;
    //ProgressDialog dialog;
    GoogleMap googleMap;
    ImageButton centerLocationBtn, postParkBtn, findParkBtn;
    private static final String TAG = "myApp";
    private Location userLocation;
    protected GoogleApiClient mGoogleApiClient;
    protected String mLastUpdateTime;
    protected LocationRequest mLocationRequest;
    protected Boolean mRequestingLocationUpdates;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final Intent myIntent = getIntent();
        final String token = myIntent.getStringExtra("firstKeyName");
        setContentView(R.layout.activity_main);
        sr = new ServerRequest();
        final Animation animShake = AnimationUtils.loadAnimation(this, R.anim.btn_shake);

        buildGoogleApiClient();
        createLocationRequest();
        mGoogleApiClient.connect();

        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 1, 10, this);

        //userLocation = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
        //if (userLocation != null) {
        //    latitude = userLocation.getLatitude();
        //    longitude = userLocation.getLongitude();
        //}

        centerLocationBtn = (ImageButton) findViewById(R.id.centerLocationBtn);
        postParkBtn = (ImageButton) findViewById(R.id.postParkBtn);
        findParkBtn = (ImageButton) findViewById(R.id.findParkBtn);

        findParkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                v.startAnimation(animShake);

                Intent intent = new Intent(getApplicationContext(), PopUpActivity.class);
                startActivityForResult(intent, 1);

                //startActivity(new Intent(MainActivity.this, PopUpActivity.class));

                overridePendingTransition(R.anim.anime_in, R.anim.anime_out);

                //userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                //if (userLocation != null) {
                //    longitude = userLocation.getLongitude();
                //    latitude = userLocation.getLatitude();
                //}

            }
            //}
        });

        postParkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                v.startAnimation(animShake);
                //userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                //if (userLocation != null) {
                //    longitude = userLocation.getLongitude();
                //    latitude = userLocation.getLatitude();
                //}

                List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                pairs.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
                pairs.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
                pairs.add(new BasicNameValuePair("token", token));
                JSONObject json = sr.getJSON("http://10.0.0.5:8080/location", pairs);
            }
        });

        centerLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(animShake);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.5f));
            }
        });

/*        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 10000,
                    1, this);

        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 10000,
                    1, this);
        } else {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), "Enable Location", Toast.LENGTH_LONG).show();
        }*/
        createMapView();
        addMyLocationMarker();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                int userChoice = data.getIntExtra("UserChoice", 0);
                if(userChoice != 0)
                {
                    if(userChoice == 1)
                    {
                        dstLatitude = data.getDoubleExtra("AddressLat", 0);
                        dstLongitude = data.getDoubleExtra("AddressLon", 0);

                        mGeofenceList = new ArrayList<Geofence>();

                        mGeofenceList.add(new Geofence.Builder()
                                // Set the request ID of the geofence. This is a string to identify this
                                // geofence.
                                .setRequestId("parkingGeoFence")

                                .setCircularRegion(
                                        dstLatitude,
                                        dstLongitude,
                                        1000
                                )
                                .setExpirationDuration(1000 * 3600)
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                .build());

                        if (!mGoogleApiClient.isConnected()) {
                            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            LocationServices.GeofencingApi.addGeofences(
                                    mGoogleApiClient,
                                    // The GeofenceRequest object.
                                    getGeofencingRequest(),
                                    // A pending intent that that is reused when calling removeGeofences(). This
                                    // pending intent is used to generate an intent when a matched geofence
                                    // transition is observed.
                                    getGeofencePendingIntent()
                            ).setResultCallback(this); // Result processed in onResult().
                        } catch (SecurityException securityException) {
                            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                            //logSecurityException(securityException);
                        }
                    }
                    else if(userChoice == 2)
                    {

                    }
                    else if(userChoice == 3)
                    {
                        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
                        pairs.add(new BasicNameValuePair("longitude", Double.toString(longitude)));
                        pairs.add(new BasicNameValuePair("latitude", Double.toString(latitude)));
                        ServerRequest srv = new ServerRequest();
                        JSONObject json = srv.getJSON("http://10.0.0.5:8080/findParkings", pairs);
                        Log.v(TAG, "Holy Shit Balls");

                        JSONArray parkings = null;
                        try {
                            parkings = json.getJSONArray("parkings");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(parkings == null)
                        {
                            String noParkingMsg = "No Parking were found near your location.";
                            Toast.makeText(this, noParkingMsg, Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
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
                    }
                }
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

/*
    private void AnalyzeUserChoice(Intent userIntent)
    {

    }
*/

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        intent.putExtra("AddressLat", latitude);
        intent.putExtra("AddressLon", longitude);

        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            mGeofencePendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mGeofencePendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            return mGeofencePendingIntent;
        }


        mGeofencePendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mGeofencePendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    LocationListener gpsLC = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            makeToast();
            handleNewLocation(location);
        }
    };

    private void makeToast()
    {
        String msg = "Location: " + latitude + " , " + longitude;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        // Create the LocationRequest object
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10 * 1000);        // 10 seconds, in milliseconds
        mLocationRequest.setFastestInterval(1 * 1000); // 1 second, in milliseconds
        mLocationRequest.setSmallestDisplacement(10);
    }

    /**
     * Initialises the mapview
     */
    private void createMapView() {
        /**
         * Catch the null pointer exception that
         * may be thrown when initialising the map
         */
        try {
            if (null == googleMap) {
                googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                        R.id.mapView)).getMap();
                if (null != googleMap) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.5f));

                }

                /**
                 * If the map is still null after attempted initialisation,
                 * show an error to the user
                 */
                if (null == googleMap) {
                    Toast.makeText(getApplicationContext(),
                            "Error creating map", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (NullPointerException exception) {
            Log.e("mapApp", exception.toString());
        }
    }


    /**
     * Adds a marker on the map for every parking that is
     * within X radius from user.
     */
    private void addParkingMarker(ParkingSpot[] i_parkArr) {
        if (googleMap != null) {
            for (int i = 0; i < i_parkArr.length; i++) {
                googleMap.addMarker(new MarkerOptions()
                                .position(new LatLng(i_parkArr[i].m_Lat, i_parkArr[i].m_Lon))
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
    private void addMyLocationMarker() {
        /** Make sure that the map has been initialised **/
        if (null != googleMap) {
            LatLng latLng = new LatLng(latitude, longitude);
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(latitude, longitude))
                            .title("My Location")
                            .draggable(false)
            );
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            //String msg = "Location: " + latitude + " , " + longitude;
            //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

 /*   @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }*/

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
        userLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (userLocation == null) {
            //this.longitude = userLocation.getLongitude();
            //this.latitude = userLocation.getLatitude();
            startLocationUpdates();
        }
        else
        {
            handleNewLocation(userLocation);
        }
        //startLocationUpdates();
    }

    private void handleNewLocation(Location location) {

        Log.d(TAG, location.toString());
        userLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        addMyLocationMarker();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, gpsLC);
    }

/*    @Override
    public void onLocationChanged(Location location) {
        //userLocation = location;
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //latitude = location.getLatitude();
        //longitude = location.getLongitude();
        //addMyLocationMarker();
        String msg = "Location: " + latitude + " , " + longitude;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        handleNewLocation(location);
    }*/

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResult(Status status) {

    }

    private class ParkingSpot {
        public double m_Lat;
        public double m_Lon;

        public ParkingSpot(double i_lat, double i_lon) {
            this.m_Lat = i_lat;
            this.m_Lon = i_lon;
        }
    }



    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
        /*    if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }*/

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                userLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
            addMyLocationMarker();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        //setUpMapIfNeeded();
        mGoogleApiClient.connect();
        //startLocationUpdates();
    }

    @Override
    protected void onPause() {

        super.onPause();
/*        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, gpsLC);
            mGoogleApiClient.disconnect();
        }*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
}

/*    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }*/

/*    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }*/

/*    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }*/

/*    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, userLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }*/


/*
    @Override
    public void onConnected(Bundle bundle) {
        userLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (userLocation != null) {
            this.latitude = userLocation.getLatitude();
            this.longitude = userLocation.getLongitude();
        }
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
*/




/*    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }*/
/*    @Override
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
    }*/

