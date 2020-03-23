package com.example.nearbyhospital;

import androidx.annotation.MainThread;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;

import java.io.IOException;
import java.util.List;

import javax.crypto.MacSpi;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    List<Address> addresses = null;

    private static final String KEY_LOCATION = "location";

    private FusedLocationProviderClient fusedLocationProviderClient;
    LatLng latLng, current;
    String loc = "" ;
    String from_lat , from_lng , to_lat , to_lng = "";
    boolean current_flag =true;
    Button search_hospitals;
    ImageView current_location ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        search_hospitals = findViewById(R.id.searchHospitals);
        current_location = findViewById(R.id.myLocation);


        Places.initialize(getApplicationContext(), "AIzaSyDq5gYTlfiBuszmn2IrwQ7T0vxgIBn3Qac");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        if(internetOn()){
            if(ok()){
                init();
            }
        }

        search_hospitals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, NearbyHospital.class);

                if(current == null){
                        Toast.makeText(MapsActivity.this , "Current location not found , please press the red button to get current location" , Toast.LENGTH_LONG).show();
                }else{
                    Log.d("debug", "------------- Make route e ki Pathaitesi --------------" + current.latitude+" , "+current.longitude);
                    intent.putExtra("current_lat", current.latitude+"" );
                    intent.putExtra("current_lng", current.longitude+"");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }

            }
        });

        current_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                init();
            }
        });


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(internetOn()){
            if(ok()){
                init();
            }
        }

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));



        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


    }



    public String setLocation(LatLng l) {

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        try {
            final String k ;

            addresses = geocoder.getFromLocation(l.latitude, l.longitude, 1);

            if (addresses.size() != 0) {
                Address obj = addresses.get(0);
                String add = obj.getAddressLine(0);
                // add = add + " , " + obj.getCountryName() + " , " + obj.getCountryCode() + " , " + obj.getAdminArea();
                // from.setText(add);

                loc = add+"";

            } else {

                Log.d("debug", "-----------kono location i paynai :/ ----------");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Log.d("debug", "----------------The size is ---------------------" + addresses.size());

        return  loc;
    }


    private void init() {

        Log.d("debug", "---------------------- init call korsi ----------------------");

        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("debug", "----------------------permission er request kortese --------------------");
            requestPermission();
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    Log.d("debug", "--------------location found------------------");

                    current = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(current).title("You are here"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
                    float zoomLevel = 16.0f;
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, zoomLevel));
                    mMap.getMaxZoomLevel();
                    setLocation(current);

                    from_lat = current.latitude+"" ;
                    from_lng = current.longitude+"";
                    current_flag = false ;

                }else{
                    Log.d("debug", "--------------location not found again calling init ------------------");
                    current_flag = true ;
                    init();
                }
            }


        });



    }

    public boolean ok() {

        boolean res = false;
        Log.d("debug", "okok");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapsActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            Log.d("debug", "its working");
            res = true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            System.out.println("n");
            Log.d("debug", "error ");
            res = false;

        } else {
            Toast.makeText(MapsActivity.this, "Can not make a map", Toast.LENGTH_SHORT).show();
        }
        return res;
    }


    public void requestPermission() {
        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        Log.d("debug ", "------------------ getting permission  --------------------");
    }

    private boolean internetOn(){
        boolean have_wifi = false ;
        boolean have_data = false;

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();

        for(NetworkInfo info:networkInfos){
            if (info.getTypeName().equalsIgnoreCase("WIFI")){
                if(info.isConnected()){
                    have_wifi=true;
                }

            }
            if (info.getTypeName().equalsIgnoreCase("MOBILE")){

                if (info.isConnected()){
                    have_data = true;
                }


            }
        }
        return have_wifi || have_data ;
    }
}
