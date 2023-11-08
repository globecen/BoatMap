package com.globecen.boatmap;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import org.osmdroid.views.overlay.Marker;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;


import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.Switch;

import java.io.File;

public class MainActivity extends AppCompatActivity implements IRegisterReceiver {

    private MapView mapView;
    private Button speedButton;
    private double speedValue = 0.0;
    private boolean displayInKnots = true;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Button locationButton;
    private Location userLocation;
    private Switch switchMapSource; // Add this line
    private static final int REQUEST_CODE_PICK_MBTILES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        mapView = findViewById(R.id.mapView);

        // Initialize tileDir here
        File tileDir = new File(getExternalFilesDir(null), "osmdroid");

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);

        switchMapSource = findViewById(R.id.switchMapSource); // Initialize the switch
        switchMapSource.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Use offline map source
                    switchToOfflineMap();
                } else {
                    // Use online map source
                    switchToOnlineMap();
                }
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

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
                    GeoPoint userGeoPoint = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
                    mapView.getController().setCenter(userGeoPoint);
                    mapView.getController().setZoom(18);
                }
            }
        });
    }

    private void switchToOfflineMap() {
        // Set the directory where offline map tiles are stored
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // You may want to be more specific here.
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_PICK_MBTILES);
    }
    private void addMarkerToMap(double latitude, double longitude, float speed) {
        GeoPoint point = new GeoPoint(latitude, longitude); // Create a point with the specified coordinates
        Marker startMarker = new Marker(mapView); // Create a new Marker object
        startMarker.setPosition(point); // Set the marker's position to the point
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); // Set the anchor point of the marker icon
        startMarker.setIcon(getResources().getDrawable(R.drawable.ic_locate_me)); // Set your custom icon here
        startMarker.setTitle("X: " +latitude+ " Y: " +longitude + "     speed (knot): "+speed*1.94384); // Optionally set a title for the marker

        mapView.getOverlays().add(startMarker); // Add the marker to the map overlays
        mapView.invalidate(); // Refresh the map to display the marker
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_MBTILES && resultCode == RESULT_OK) {
            if (data != null) {
                Uri selectedFile = data.getData();
                loadMBTiles(selectedFile); // This method will handle loading of MBTiles
            }
        }
    }
    private void loadMBTiles(Uri fileUri) {
        try {
            // You'll need to implement the logic to convert Uri to actual file path if needed.
            File mbTileFile = new File(fileUri.getPath());

            // Use an MBTilesTileSource, assuming you have such a class that extends BitmapTileSourceBase.
            ITileSource tileSource = new MBTilesTileSource(this, mbTileFile.getName(), 0, 19, 256, ".png");
            mapView.setTileSource(tileSource);

            // Update the map center and zoom level as needed
            mapView.getController().setZoom(18); // Example zoom level
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load MBTiles file", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    private void switchToOnlineMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getTileProvider().clearTileCache(); // Clear the tile cache

        // You can also update the map center and zoom level as needed
        mapView.getController().setZoom(18); // Example zoom level
    }
    private void initMapView() {
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
    }

    private void initLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mapView.getController().setZoom(18);
        mapView.getController().setCenter(new GeoPoint(0, 0));

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double speedMetersPerSecond = location.getSpeed();

                if (displayInKnots) {
                    speedValue = speedMetersPerSecond * 1.94384;
                } else {
                    speedValue = speedMetersPerSecond * 3.6;
                }

                userLocation = location;
                addMarkerToMap(location.getLatitude(), location.getLongitude(), location.getSpeed());
                GeoPoint userGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapView.getController().setCenter(userGeoPoint);

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

    public void onSpeedButtonClick(View view) {
        displayInKnots = !displayInKnots;
        updateSpeedButtonText();
    }

    private void updateSpeedButtonText() {
        speedButton.setText(String.format("%.2f %s", speedValue, displayInKnots ? "knots" : "km/h"));
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
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initMapView();
                initLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied. Unable to access your location.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
