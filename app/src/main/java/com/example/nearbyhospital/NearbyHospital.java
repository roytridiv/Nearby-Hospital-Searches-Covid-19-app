package com.example.nearbyhospital;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NearbyHospital extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    int PROXIMITY_RADIUS = 10000;

    String currentLatitude , currentLongitude , to_lat , to_lng = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_hospital);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        Intent intent = getIntent();
       currentLatitude = intent.getStringExtra("current_lat");
        currentLongitude = intent.getStringExtra("current_lng");

        Log.d("debug", "------------- Make route e ki paitesi--------------" + currentLatitude+" , "+currentLongitude);



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

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LatLng l = marker.getPosition();

                String name = marker.getTitle();

                Log.d("debug","------------- location er naam ki ----------------"+ name);

                to_lat = l.latitude +"" ;
                to_lng = l.longitude+"" ;

                if(currentLatitude.equals("") ||
                        currentLongitude.equals("") ||
                        to_lat.equals("") ||
                        to_lng.equals("") ||
                        currentLongitude == null ||
                        currentLatitude == null ||
                        to_lat == null ||
                        to_lng == null){

                    Toast.makeText(NearbyHospital.this , "Invalid location , please press the red button to get current location" , Toast.LENGTH_LONG).show();


                }else {


                    AlertDialog.Builder builder1 = new AlertDialog.Builder(NearbyHospital.this);
                    builder1.setMessage("Hospital Name & Location:\n\n"+name);
                    builder1.setCancelable(true);

                    builder1.setPositiveButton(
                            "Show Route",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {


                                    if(isGoogleMapsInstalled()){
                                        Uri gmmIntentUri = Uri.parse("google.navigation:q="+to_lat+","+to_lng);
                                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                        mapIntent.setPackage("com.google.android.apps.maps");
                                        startActivity(mapIntent);
                                        //finish();
                                    }else{
                                        Intent intent = new Intent(NearbyHospital.this, MakeRoute.class);
                                    intent.putExtra("current_lat", currentLatitude);
                                    intent.putExtra("current_lng", currentLongitude);
                                    intent.putExtra("destination_lat", to_lat);
                                    intent.putExtra("destination_lng", to_lng);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                        finish();
                                    }






                                    dialog.cancel();
                                }
                            });

                    builder1.setNegativeButton(
                            "Back",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alert11 = builder1.create();
                    alert11.show();


                }

                return true ;
            }
        });


//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


       // String url = getDirectionsUrl(new LatLng(23.866874, 90.404575));
        String url = getDirectionsUrl(new LatLng(Double.parseDouble(currentLatitude), Double.parseDouble(currentLongitude)));
        //String url = getDirectionsUrl(new LatLng(23.747546005998082, 23.747546005998082), new LatLng(23.866874, 90.404575));

        // DownloadTask downloadTask = new DownloadTask();
        // MakeRoute.TaskRequestDirection downloadTask = new MakeRoute.TaskRequestDirection();


        TaskRequestDirection taskRequestDirection = new TaskRequestDirection();



        // Start downloading json data from Google Directions API
        taskRequestDirection.execute(url);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) this)
                .addOnConnectionFailedListener((GoogleApiClient.OnConnectionFailedListener) NearbyHospital.this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public class TaskRequestDirection extends AsyncTask<Object , String , String> {

        String googlePlacesData;
        GoogleMap mMap;
        String url;

//        @Override
//        protected String doInBackground(String... url) {
//
//            String data = "";
//
//            try {
//                data = downloadUrl(url[0]);
//            } catch (Exception e) {
//                Log.d("debug", e.toString());
//            }
//            return data;
//        }

        @Override
        protected String doInBackground(Object... objects) {

            Log.d("debug", objects.length+" , "+objects.toString());


            // mMap = (GoogleMap)objects[0];
            url = (String)objects[0];

            //create an object of download URL class

            //DownloadURL downloadURL = new DownloadURL();
            try {
                googlePlacesData = downloadUrl(url);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return googlePlacesData;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // MakeRoute.ParserTask parserTask = new MakeRoute.ParserTask();

            List<HashMap<String , String>> nearbyPlaceList = null;

            DataParser parser = new DataParser();
            nearbyPlaceList = parser.parse(s);

            showNearbyPlaces(nearbyPlaceList);
        }
    }

    private String getDirectionsUrl(LatLng origin) {

        double lat = origin.latitude;
        double lng = origin.longitude;


        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+lat+","+lng+"&radius=10000&type=hospital&key=AIzaSyBkCNdgswUBcGOk6Yq9Jy0KR--_EUMPaTA");




        return googlePlaceUrl.toString();

    }

    private void showNearbyPlaces(List<HashMap<String,String>> nearbyPlaceList)
    {
        for(int i=0;i<nearbyPlaceList.size();i++)
        {
            //show all the places in the list
            //we are going to create marker options

            MarkerOptions markerOptions = new MarkerOptions();
            HashMap<String,String> googlePlace = nearbyPlaceList.get(i);

            String placeName = googlePlace.get("place_name");
            String vicinity = googlePlace.get("vicinity");
            double lat = Double.parseDouble(googlePlace.get("lat"));
            double lng = Double.parseDouble(googlePlace.get("lng"));

            LatLng latLng = new LatLng(lat,lng);

            markerOptions.position(latLng);
            markerOptions.title(placeName + " : " + vicinity);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.hospitalicon));


            mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(currentLatitude), Double.parseDouble(currentLongitude)), 12));
            //mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
        }
    }





    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        Log.d("debug" , "----------------------- url response ------------------ "+ data);
        return data;
    }


    ///////////////////////////////////////////////////////////////////////////

    public class DataParser {

        private HashMap<String , String> getPlace(JSONObject googlePlaceJson)
        {
            HashMap<String , String> googlePlacesMap = new HashMap<>();

            //store all the parameters using String

            String placeName = "-NA-";
            String vicinity = "-NA-";
            String latitude = "";
            String longitude = "";
            String reference = "";

            try {

                if (!googlePlaceJson.isNull("name")) {
                    placeName = googlePlaceJson.getString("name");
                }

                if (!googlePlaceJson.isNull("vicinity")) {
                    vicinity = googlePlaceJson.getString("vicinity");
                }

                latitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lat");
                longitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lng");

                reference = googlePlaceJson.getString("reference");

                googlePlacesMap.put("place_name" , placeName);
                googlePlacesMap.put("vicinity" , vicinity);
                googlePlacesMap.put("lat" , latitude);
                googlePlacesMap.put("lng" , longitude);
                googlePlacesMap.put("reference" , reference);

            } catch(JSONException e) {
                e.printStackTrace();

            }

            return googlePlacesMap;

            //to store one place we are using a HashMap
        }


        //to store all the places create a list of HashMap
        private List<HashMap<String,String>> getPlaces(JSONArray jsonArray)
        {

            //getPlace returns a HashMap for each place
            //getPlaces() creates a list of HashMaps

            int count = jsonArray.length();
            List<HashMap<String,String>> placesList = new ArrayList<>();
            HashMap<String,String> placeMap = null; //to store each place we fetch

            for(int i=0 ; i<count;i++) {

                //use getPlace method to fetch one place
                //then , add it to list of hashmap

                try {
                    placeMap = getPlace((JSONObject) jsonArray.get(i));
                    placesList.add(placeMap);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            return placesList;
        }

        //call this parse method whenever you create Data Parser
        //it will parse the JSON data n send it to getPlaces method
        //getPlaces method takes the JSONArray
        //will call getPlace method to fetch each element for each place and store it in a list
        //return the list to parse method

        public List<HashMap<String,String>> parse(String jsonData)
        {
            JSONArray jsonArray = null;
            JSONObject jsonObject;

            try {
                jsonObject = new JSONObject(jsonData);
                jsonArray = jsonObject.getJSONArray("results");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return getPlaces(jsonArray);
        }
    }


    /////////////////////////////////////////////////////////////////////













    public class DirectionsJSONParser {
        /**
         * Receives a JSONObject and returns a list of lists containing latitude and
         * longitude
         */


        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;
            JSONObject jDistance = null;
            JSONObject jDuration = null;

            try {

                jRoutes = jObject.getJSONArray("routes");

                /** Traversing all routes */
                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");

                    List<HashMap<String, String>> path = new ArrayList<HashMap<String, String>>();

                    /** Traversing all legs */
                    for (int j = 0; j < jLegs.length(); j++) {

                        /** Getting distance from the json data */
                        jDistance = ((JSONObject) jLegs.get(j))
                                .getJSONObject("distance");
                        HashMap<String, String> hmDistance = new HashMap<String, String>();
                        //dis = jDistance.getString("value");
                        hmDistance.put("distance", jDistance.getString("text"));

                        /** Getting duration from the json data */
                        jDuration = ((JSONObject) jLegs.get(j))
                                .getJSONObject("duration");
                        HashMap<String, String> hmDuration = new HashMap<String, String>();
                        hmDuration.put("duration", jDuration.getString("text"));

                        /** Adding distance object to the path */
                        path.add(hmDistance);

                        /** Adding duration object to the path */
                        path.add(hmDuration);

                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps
                                    .get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            // l = list;

                            /** Traversing all points */
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat",
                                        Double.toString(((LatLng) list.get(l)).latitude));
                                hm.put("lng",
                                        Double.toString(((LatLng) list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                    }
                    routes.add(path);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }

            return routes;
        }


        /**
         * Method to decode polyline points Courtesy :
         * jeffreysambells.com/2010/05/27
         * /decoding-polylines-from-google-maps-direction-api-with-java
         */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }

    }

    public void zoomRoute(GoogleMap googleMap, List<LatLng> lstLatLngRoute) {
        boolean hasPoints = false;
        Double maxLat = null, minLat = null, minLon = null, maxLon = null;

        List<LatLng> pts = lstLatLngRoute;
        for (LatLng coordinate : pts) {

            maxLat = maxLat != null ? Math.max(coordinate.latitude, maxLat) : coordinate.latitude;
            minLat = minLat != null ? Math.min(coordinate.latitude, minLat) : coordinate.latitude;

            maxLon = maxLon != null ? Math.max(coordinate.longitude, maxLon) : coordinate.longitude;
            minLon = minLon != null ? Math.min(coordinate.longitude, minLon) : coordinate.longitude;

            hasPoints = true;
        }

        Log.d("debug" , " Max --->"+ maxLat + " , "+maxLon + " , MIN----->"+ minLat+" , "+minLon);

        if (hasPoints) {
            LatLngBounds latLngBounds = new LatLngBounds(  new LatLng(minLat, minLon), new LatLng(maxLat, maxLon));

            float zoomLevel = 0.0f;

            float f = Float.parseFloat("8.00");

            if (f < 1128) { zoomLevel = 15.5f; }
            else if ((f > 1128) && (f < 2256)) { zoomLevel = 14.5f; }
            else if ((f > 2256) && (f < 4513)) { zoomLevel = 13.5f; }
            else if ((f > 4513) && (f < 9027)) { zoomLevel = 12.5f; }
            else if ((f > 9027) && (f < 18055)) { zoomLevel = 11.5f; }
            else if ((f > 18055) && (f < 36111)) { zoomLevel = 10.5f; }
            else if ((f > 36111) && (f < 722223)){ zoomLevel = 9.5f ; }



            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngBounds.getCenter(), zoomLevel));
            googleMap.getMinZoomLevel();
        }



    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(NearbyHospital.this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }


    public boolean isPackageExisted(String targetPackage){
        PackageManager pm=getPackageManager();
        try {
            PackageInfo info =pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public boolean isGoogleMapsInstalled()
    {
        try
        {
            ApplicationInfo info = getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0 );
            return true;
        }
        catch(PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

}