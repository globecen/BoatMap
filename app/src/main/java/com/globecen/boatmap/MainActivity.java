package com.globecen.boatmap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;

import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
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
import org.osmdroid.views.overlay.TilesOverlay;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
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
    private Marker userMarker;
    private Location userLocation;
    private Switch switchAddMarker;
    private static final int REQUEST_CODE_PICK_MBTILES = 2;
    private PopupWindow popupWindow;
    private int[] boatIcons = {
            R.drawable.ic_boat_1,
            R.drawable.ic_boat_2,
            R.drawable.ic_boat_3,
            R.drawable.ic_boat_4,
            R.drawable.ic_boat_5
            // Add more boat icons as needed
    };
    private static final String PREF_SELECTED_ICON_ID = "selected_icon_id";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        mapView = findViewById(R.id.mapView);
        userMarker = new Marker(mapView);

        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int selectedIconId = preferences.getInt(PREF_SELECTED_ICON_ID, R.drawable.ic_boat_1); // Use default icon if no icon is saved
        userMarker.setIcon(getResources().getDrawable(selectedIconId));
        // Initialize tileDir here
        File tileDir = new File(getExternalFilesDir(null), "osmdroid");
        mapView.getOverlays().add(userMarker);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        XYTileSource openSeaMapTileSource = new XYTileSource(
                "OpenSeaMap",
                0,
                18,
                256,
                ".png",
                new String[]{"https://tiles.openseamap.org/seamark/"}
        );
        TilesOverlay openSeaMapTilesOverlay = new TilesOverlay(
                new MapTileProviderBasic(this, openSeaMapTileSource),
                this.getBaseContext()
        );
        mapView.getOverlays().add(openSeaMapTilesOverlay);


        // Set up popup window
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_change_icon, null);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Set up list view in the popup
        LinearLayout iconListContainer = popupView.findViewById(R.id.iconListContainer);

        for (int iconResId : boatIcons) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(iconResId);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Change userMarker icon here based on clicked icon
                    userMarker.setIcon(getResources().getDrawable(iconResId));
                    // Save the selected icon ID to SharedPreferences
                    SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    preferences.edit().putInt(PREF_SELECTED_ICON_ID, iconResId).apply();
                    popupWindow.dismiss(); // Dismiss the popup after selecting an icon
                }
            });
            iconListContainer.addView(imageView);
        }

        // Set up click listener for userMarker
        userMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                // Show popup window near the map view
                popupWindow.showAtLocation(mapView, Gravity.AXIS_CLIP, 0,200);
                return true; // Consume the event
            }
        });




        switchAddMarker = findViewById(R.id.switchAddMarker);
        switchAddMarker.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Handle the switch state change
                if (isChecked) {
                    // Switch is ON, so you can add markers to the map
                } else {
                    // Switch is OFF, so you should not add markers to the map
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

            // Center the map on the user's location when the activity is created
            centerMapOnUserLocation();
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

    // Add this method to center the map on the user's location
    private void centerMapOnUserLocation() {
        if (userLocation != null) {
            GeoPoint userGeoPoint = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
            mapView.getController().setCenter(userGeoPoint);
            mapView.getController().setZoom(18);
        }
    }

    private void switchToOfflineMap() {
        // Set the directory where offline map tiles are stored
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // You may want to be more specific here.
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_PICK_MBTILES);
    }
    private void addMarkerToMap(double latitude, double longitude, float speed) {

            GeoPoint point = new GeoPoint(latitude, longitude);
            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(point);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(getResources().getDrawable(R.drawable.maps_and_flags));
            startMarker.setTitle("X: " + latitude + " Y: " + longitude + " speed (knot): " + speed * 1.94384);
            mapView.getOverlays().add(startMarker);
            mapView.invalidate();
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
        XYTileSource openSeaMapTileSource = new XYTileSource(
                "OpenSeaMap",
                0,
                18,
                256,
                ".png",
                new String[]{"https://tiles.openseamap.org/seamark/"}
        );
        TilesOverlay openSeaMapTilesOverlay = new TilesOverlay(
                new MapTileProviderBasic(this, openSeaMapTileSource),
                this.getBaseContext()
        );
        mapView.getOverlays().add(openSeaMapTilesOverlay);
        // You can also update the map center and zoom level as needed
        mapView.getController().setZoom(18); // Example zoom level
    }
    private void initMapView() {
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        XYTileSource openSeaMapTileSource = new XYTileSource(
                "OpenSeaMap",
                0,
                18,
                256,
                ".png",
                new String[]{"https://tiles.openseamap.org/seamark/"}
        );
        TilesOverlay openSeaMapTilesOverlay = new TilesOverlay(
                new MapTileProviderBasic(this, openSeaMapTileSource),
                this.getBaseContext()
        );
        mapView.getOverlays().add(openSeaMapTilesOverlay);
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
                if (userMarker != null) {
                    userMarker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
                    mapView.invalidate();
                }
                GeoPoint userGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapView.getController().setCenter(userGeoPoint);

                if (switchAddMarker.isChecked()) {
                    addMarkerToMap(location.getLatitude(), location.getLongitude(), location.getSpeed());
                }
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

    private void updateSpeedButtonText() {
        speedButton.setText(String.format("%.2f %s", speedValue, displayInKnots ? "knots" : "km/h"));
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }
    public void onSpeedButtonClick(View view) {
        displayInKnots = !displayInKnots;
        updateSpeedButtonText();
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
