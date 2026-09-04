package com.manavo.HowFarTo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class main extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private GoogleMap map;
    private TextView distance;
    private EditText locationText;

    private List<Address> addresses;

    private Address location;
    private LatLng locationPoint;

    private Location lastFix;
    private boolean zoomedToFirstFix = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private LocationLookup lookup;
    private ProgressDialog dialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.distance = this.findViewById(R.id.distance);

        this.locationText = this.findViewById(R.id.location);
        this.locationText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL || (event != null && event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                main.this.searchLocation();
            }
            return false;
        });

        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location fix = locationResult.getLastLocation();
                if (fix == null) {
                    return;
                }
                boolean firstFix = main.this.lastFix == null;
                main.this.lastFix = fix;
                if (firstFix) {
                    main.this.onFirstFix();
                } else {
                    main.this.showDistance();
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        this.map.getUiSettings().setZoomControlsEnabled(true);

        if (this.hasLocationPermission()) {
            this.enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (this.hasLocationPermission()) {
                this.enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission is needed to show how far away things are", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void enableMyLocation() {
        if (this.map != null) {
            this.map.setMyLocationEnabled(true);
        }
        this.startLocationUpdates();
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();
        this.fusedLocationClient.requestLocationUpdates(request, this.locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.hasLocationPermission()) {
            this.startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.fusedLocationClient.removeLocationUpdates(this.locationCallback);
    }

    @Override
    protected void onDestroy() {
        if (this.lookup != null) {
            this.lookup.cancel();
        }
        if (this.dialog != null) {
            this.dialog.dismiss();
        }
        super.onDestroy();
    }

    private void onFirstFix() {
        if (this.map != null && !this.zoomedToFirstFix) {
            this.zoomedToFirstFix = true;
            LatLng myLocation = new LatLng(this.lastFix.getLatitude(), this.lastFix.getLongitude());
            this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12f));
        }
        this.showDistance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        int id = item.getItemId();
        if (id == R.id.about) {
            this.startActivity(new Intent(this, About.class));
            return true;
        } else if (id == R.id.settings) {
            this.startActivity(new Intent(this, Settings.class));
            return true;
        } else if (id == R.id.exit) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Calculate distance between 2 points in Kilometers
    // Based on the haversine formula from here: http://www.movable-type.co.uk/scripts/latlong.html
    private double calculateDistance(LatLng p1, LatLng p2) {
        double lat1 = p1.latitude;
        double lon1 = p1.longitude;
        double lat2 = p2.latitude;
        double lon2 = p2.longitude;

        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public void showAddress(Address address) {
        this.location = address;
        this.locationPoint = new LatLng(address.getLatitude(), address.getLongitude());

        if (this.map != null) {
            this.map.clear();
            this.map.addMarker(new MarkerOptions()
                    .position(this.locationPoint)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                    .title(address.getAddressLine(0) + ", " + address.getCountryCode()));
        }

        this.showDistance();
    }

    private void showDistance() {
        if (this.lastFix == null) {
            Toast.makeText(this, "Waiting for your location", Toast.LENGTH_LONG).show();
        } else if (this.location == null) {
            // Do nothing, we got our location but haven't searched for anything yet
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String showDistanceIn = sp.getString("distanceIn", "km");

            LatLng myLocation = new LatLng(this.lastFix.getLatitude(), this.lastFix.getLongitude());

            double distance = this.calculateDistance(myLocation, this.locationPoint);
            if (!showDistanceIn.equals("km")) {
                // km to miles
                distance *= 0.621371192d;
            }

            String locationText = "About " + main.round(distance, 1) + showDistanceIn;

            float accuracy = this.lastFix.getAccuracy();

            if (accuracy > 0) {
                if (showDistanceIn.equals("km")) {
                    if (accuracy < 500f) {
                        locationText += "(± " + accuracy + "m)";
                    } else {
                        locationText += "(± " + main.round(accuracy / 1000, 2) + "km)";
                    }
                } else {
                    double accuracyInMiles = (accuracy * 0.000621371192d); // meters to miles
                    if (accuracyInMiles > 0.3f) {
                        locationText += "(± " + main.round(accuracyInMiles, 2) + "mi)";
                    } else {
                        locationText += "(± " + main.round(accuracyInMiles * 5280, 2) + "ft)";
                    }
                }
            }

            locationText += " to " + this.location.getAddressLine(0) + ", " + this.location.getCountryCode();
            this.distance.setText(locationText);
            this.distance.setVisibility(View.VISIBLE);

            this.showAllPoints(myLocation);
        }
    }

    public void hideDialog() {
        if (this.dialog != null && this.dialog.isShowing()) {
            this.dialog.dismiss();
        }
    }

    // for when the button is clicked
    public void searchLocation(View v) {
        this.searchLocation();
    }

    public void searchLocation() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.locationText.getWindowToken(), 0);

        String location = this.locationText.getText().toString();

        if (location.length() > 0) {
            this.lookup = new LocationLookup(this);

            this.dialog = new ProgressDialog(this);
            this.dialog.setMessage("Finding the location...");
            this.dialog.setCancelable(true);
            this.dialog.setOnCancelListener(arg0 -> {
                main.this.lookup.cancel();
                Toast.makeText(main.this, "Cancelled", Toast.LENGTH_SHORT).show();
            });
            this.dialog.show();

            this.lookup.execute(location);
        } else {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_LONG).show();
        }
    }

    public void searchLocationError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    public void searchLocationCallback(List<Address> addresses) {
        this.addresses = addresses;

        if (this.addresses.size() == 1) {
            showAddress(this.addresses.get(0));
        } else if (this.addresses.size() == 0) {
            Toast.makeText(this, "Nothing found!", Toast.LENGTH_LONG).show();
        } else {
            Iterator<Address> i = this.addresses.iterator();
            Address a;

            ArrayList<String> options = new ArrayList<String>();
            while (i.hasNext()) {
                a = i.next();
                options.add(a.getAddressLine(0) + ", " + a.getCountryName());
            }
            CharSequence[] cs = options.toArray(new CharSequence[options.size()]);

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setItems(cs, (arg0, index) -> showAddress(main.this.addresses.get(index)));
            dialog.show();
        }
    }

    // Zoom the camera so both my location and the searched location are visible
    private void showAllPoints(LatLng myLocation) {
        if (this.map == null || this.locationPoint == null) {
            return;
        }
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(myLocation)
                .include(this.locationPoint)
                .build();
        this.map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    // function from http://stackoverflow.com/questions/3596023/round-to-2-decimal-places
    public static double round(double unrounded, int precision) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, RoundingMode.UP);
        return rounded.doubleValue();
    }

}
