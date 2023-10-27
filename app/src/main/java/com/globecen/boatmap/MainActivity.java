package com.globecen.boatmap;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private Marker userLocationMarker;

    private MapView mapView;
    private Button speedButton;
    private double speedValue = 0.0; // Vitesse initiale
    private boolean displayInKnots = true; // Unité initiale (nœuds)
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Button locationButton;

    private Location userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            initMapView();
            initLocationUpdates();
        }

        speedButton = findViewById(R.id.speedButton);
        locationButton = findViewById(R.id.locationButton);
        updateSpeedButtonText();
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userLocation != null) {
                    LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15)); // Zoom to user's location
                }
            }
        });
    }

    private void initMapView() {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    private void initLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Update the speed value based on the new location data
                double speedMetersPerSecond = location.getSpeed(); // Speed in meters per second

                // You can use this method to calculate the speed value in knots or km/h.
                if (displayInKnots) {
                    speedValue = speedMetersPerSecond * 1.94384; // Convertir m/s en nœuds
                } else {
                    speedValue = speedMetersPerSecond * 3.6; // Convertir m/s en km/h
                }

                // Update the user's location
                userLocation = location;

                // Update the user location marker
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                userLocationMarker.setPosition(userLatLng);
                userLocationMarker.setVisible(true);

                updateSpeedButtonText();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Handle status changes, if needed
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Handle provider enabled, if needed
            }

            @Override
            public void onProviderDisabled(String provider) {
                // Handle provider disabled, if needed
            }
        });
    }

    // Gérer le clic sur le bouton
    public void onSpeedButtonClick(View view) {
        // Basculer entre les nœuds et les km/h
        displayInKnots = !displayInKnots;
        updateSpeedButtonText();
    }

    // Mettre à jour le texte sur le bouton de vitesse
    private void updateSpeedButtonText() {
        speedButton.setText(String.format("%.2f %s", speedValue, displayInKnots ? "nœuds" : "km/h"));
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        // Set up the initial map camera position
        LatLng initialPosition = new LatLng(0, 0);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 10));

        // Create a marker for the user's location (initially hidden)
        userLocationMarker = googleMap.addMarker(new MarkerOptions()
                .position(initialPosition)
                .title("My Location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_locate_me)) // Use the custom icon
                .visible(false));  // Initially set to invisible

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
    // Autre code pour gérer les mises à jour de localisation et les données GPS

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initMapView();
                initLocationUpdates();
            }
        }
    }
}
