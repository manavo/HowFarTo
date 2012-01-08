package com.manavo.HowFarTo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class main extends MapActivity {
	private MapView mapView;
	private TextView distance;
	private EditText locationText;
	
	private List<Overlay> mapOverlays;
	private hftOverlay itemizedoverlay;
	
	private List<Address> addresses;
	
	private FixedMyLocationOverlay myLocationOverlay;
	
	private Address location;
	private GeoPoint locationPoint;
	
	private Handler hRefresh;
	
	private LocationLookup lookup;
	private ProgressDialog dialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        this.distance = (TextView)this.findViewById(R.id.distance);
        
        this.locationText = (EditText)this.findViewById(R.id.location);
        this.locationText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // do the search. no return false, since we want the default action to happen anyway (hide keyboard)
                	main.this.searchLocation();
                }
                return false;
            }
        });
        
        this.mapView = (MapView)this.findViewById(R.id.mapview);
        this.mapView.setBuiltInZoomControls(true);
        
        this.mapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.marker);
        this.itemizedoverlay = new hftOverlay(drawable, this);
        
        this.hRefresh = new Handler(){
	        @Override
	        public void handleMessage(Message msg) {
				main.this.mapView.getController().animateTo(main.this.myLocationOverlay.getMyLocation());
				main.this.showDistance();
	        }
        };
        
        this.myLocationOverlay = new FixedMyLocationOverlay(this, this.mapView);
		this.myLocationOverlay.enableMyLocation();
		this.myLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				main.this.hRefresh.sendEmptyMessage(0);
			}
		});
		
        this.addMyLocation();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	this.myLocationOverlay.enableMyLocation();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	this.myLocationOverlay.disableMyLocation();
    }
    
    private void addMyLocation() {
		this.mapOverlays.add(this.myLocationOverlay);
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
        switch (item.getItemId()) {
        case R.id.about:
            this.startActivity(new Intent(this, About.class));
            return true;
        case R.id.settings:
        	this.startActivity(new Intent(this, Settings.class));
        	return true;
        case R.id.exit:
            this.finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // Calculate distance between 2 points in Kilometers
    // Based on the haversine formula from here: http://www.movable-type.co.uk/scripts/latlong.html
    private double calculateDistance(GeoPoint p1, GeoPoint p2) {
    	double lat1 = p1.getLatitudeE6()/1E6;
    	double lon1 = p1.getLongitudeE6()/1E6;
    	double lat2 = p2.getLatitudeE6()/1E6;
    	double lon2 = p2.getLongitudeE6()/1E6;
    	
    	double R = 6371; // km
    	double dLat = Math.toRadians(lat2-lat1);
    	double dLon = Math.toRadians(lon2-lon1);
    	
    	lat1 = Math.toRadians(lat1);
    	lat2 = Math.toRadians(lat2);

    	double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
    	double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
    	double d = R * c;
    	
    	return d;
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    public void showAddress(Address address) {
    	this.location = address;
    	
    	Integer lat = new Double(address.getLatitude()*1E6).intValue();
		Integer lng = new Double(address.getLongitude()*1E6).intValue();
		
		this.mapOverlays.clear();
        this.addMyLocation();
        
        this.locationPoint = new GeoPoint(lat, lng);
        OverlayItem overlayitem = new OverlayItem(this.locationPoint, null, address.getAddressLine(0)+", "+address.getCountryCode());
        
        this.itemizedoverlay.clear();
        this.itemizedoverlay.addOverlay(overlayitem);

        this.mapOverlays.add(this.itemizedoverlay);
        
		this.mapView.invalidate();
		
		this.showDistance();
    }
    
    private void showDistance() {
    	GeoPoint myLocation = this.myLocationOverlay.getMyLocation();
    	if (myLocation == null) {
    		Toast.makeText(this, "Waiting for your location", Toast.LENGTH_LONG).show();
    	} else if (this.location == null) {
    		// Do nothing, we got our location but haven't searched for anything yet
    	} else {
    	    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
	    	String showDistanceIn = sp.getString("distanceIn", "km");
	    	
	    	Double distance = this.calculateDistance(myLocation, this.locationPoint);
	    	if (showDistanceIn.equals("km") == false) {
	    		// km to miles
	    		distance *= 0.621371192d;
	    	}
	    	
        	String locationText = "About " + main.round(distance, 1) + showDistanceIn;
	    	
        	float accuracy = this.myLocationOverlay.getLastFix().getAccuracy();
        	
        	if (accuracy > 0) {
    	    	if (showDistanceIn.equals("km") == true) {
	        		if (accuracy < 500f) {
	        			locationText += "(� " + accuracy + "m)";
	        		} else {
	        			locationText += "(� " + main.round(accuracy/1000, 2) + "km)";
	        		}
    	    	} else {
    	    		double accuracyInMiles = (accuracy * 0.000621371192d); // meters to miles
    	    		if (accuracyInMiles > 0.3f) {
    	    			locationText += "(� " + main.round(accuracyInMiles, 2) + "mi)";
    	    		} else {
    	    			locationText += "(� " + main.round(accuracyInMiles * 5280, 2) + "ft)";
    	    		}
    	    	}
        	}
	    	
        	locationText += " to " + this.location.getAddressLine(0) + ", " + this.location.getCountryCode();
        	this.distance.setText(locationText); 
        	this.distance.setVisibility(View.VISIBLE);
        	
    		this.showAllOverlays();
    	}
    }
    
    public void hideDialog() {
    	this.dialog.hide();
    }
    
    // for when the button is clicked
    public void searchLocation(View v) {
    	this.searchLocation();
    }
    
    public void searchLocation() {
    	String location = this.locationText.getText().toString();
    	
    	if (location.length() > 0) {
	    	this.lookup = new LocationLookup(this);
	    	
			this.dialog = new ProgressDialog(this);
			this.dialog.setMessage("Finding the location...");
			this.dialog.setCancelable(true);
			this.dialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface arg0) {
					main.this.lookup.cancel(true);
					Toast.makeText(main.this, "Cancelled", Toast.LENGTH_SHORT).show();
				}
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
			dialog.setItems(cs, new OnClickListener() {
				public void onClick(DialogInterface arg0, int index) {
					showAddress(main.this.addresses.get(index));
				}
			});
			dialog.show();
		}
    }
    
    // Code based on http://stackoverflow.com/questions/5241487/android-mapview-setting-zoom-automatically-until-all-itemizedoverlays-are-visib
    // Adjusted to take into account the my location overlay as well
    private void showAllOverlays() {
    	int minLat = Integer.MAX_VALUE;
    	int maxLat = Integer.MIN_VALUE;
    	int minLon = Integer.MAX_VALUE;
    	int maxLon = Integer.MIN_VALUE;
    	
    	GeoPoint p;
    	
    	for (OverlayItem item : this.itemizedoverlay.getItems()) {
    		p = item.getPoint();
    		
    		int lat = p.getLatitudeE6();
    		int lon = p.getLongitudeE6();

    		maxLat = Math.max(lat, maxLat);
    		minLat = Math.min(lat, minLat);
    		maxLon = Math.max(lon, maxLon);
    		minLon = Math.min(lon, minLon);
    	}

		maxLat = Math.max(this.myLocationOverlay.getMyLocation().getLatitudeE6(), maxLat);
		minLat = Math.min(this.myLocationOverlay.getMyLocation().getLatitudeE6(), minLat);
		maxLon = Math.max(this.myLocationOverlay.getMyLocation().getLongitudeE6(), maxLon);
		minLon = Math.min(this.myLocationOverlay.getMyLocation().getLongitudeE6(), minLon);
		
    	this.mapView.getController().zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
    	this.mapView.getController().animateTo(new GeoPoint( (maxLat + minLat)/2, (maxLon + minLon)/2 )); 
    }
    
    // function from http://stackoverflow.com/questions/3596023/round-to-2-decimal-places
    public static double round(double unrounded, int precision) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, BigDecimal.ROUND_UP);
        return rounded.doubleValue();
    }

}