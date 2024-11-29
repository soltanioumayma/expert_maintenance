package com.example.test;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

public class OSMMapActivity extends AppCompatActivity {

    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up OSMDroid configuration
        File cacheDir = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(cacheDir);
        Configuration.getInstance().setOsmdroidTileCache(cacheDir);
        Configuration.getInstance().setUserAgentValue("MyOSMApp");

        // Set the content view to the map layout
        setContentView(R.layout.activity_osm_map);

        // Retrieve latitude and longitude from Intent
        double latitude = getIntent().getDoubleExtra("latitude", 0.0);
        double longitude = getIntent().getDoubleExtra("longitude", 0.0);

        // Initialize the MapView
        mapView = findViewById(R.id.osm_map);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15); // Set the zoom level

        // Center the map on the specified location
        GeoPoint startPoint = new GeoPoint(latitude, longitude);
        mapView.getController().setCenter(startPoint);

        // Add a marker to the specified location
        Marker marker = new Marker(mapView);
        marker.setPosition(startPoint);
        marker.setTitle("Selected Location");
        mapView.getOverlays().add(marker);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up the MapView to avoid memory leaks
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
