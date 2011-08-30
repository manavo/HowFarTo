package com.manavo.HowFarTo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class main extends MapActivity {
	private MapView mapView;
	
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private hftOverlay itemizedoverlay;
	
	private List<Address> addresses;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        this.mapView = (MapView)this.findViewById(R.id.mapview);
        this.mapView.setBuiltInZoomControls(true);
        
        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.marker);
        itemizedoverlay = new hftOverlay(drawable, this);
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    public void showAddress(Address address) {
    	Integer lat = new Double(address.getLatitude()*1E6).intValue();
		Integer lng = new Double(address.getLongitude()*1E6).intValue();
		
		mapOverlays.clear();
        
        GeoPoint point = new GeoPoint(lat, lng);
        OverlayItem overlayitem = new OverlayItem(point, "Hello!", "I'm in "+address.getAddressLine(0)+"!");
        
        itemizedoverlay.clear();
        itemizedoverlay.addOverlay(overlayitem);

        mapOverlays.add(itemizedoverlay);
        
		mapView.invalidate();
    }
    
    public void searchLocation(View v) {
    	EditText location = (EditText)this.findViewById(R.id.location);
    	//TextView result = (TextView)this.findViewById(R.id.result);
    	
    	String sLocation = location.getText().toString();
    	
    	Geocoder g = new Geocoder(this);
    	try {
			addresses = g.getFromLocationName(sLocation, 5);
			
			if (addresses.size() == 1) {
				showAddress(addresses.get(0));
			} else if (addresses.size() == 0) {
				Toast.makeText(this, "Nothing found!", Toast.LENGTH_LONG).show();
			} else {
				Iterator<Address> i = addresses.iterator();
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	    	Toast.makeText(this, "Could not connect. Please try again!", Toast.LENGTH_SHORT).show();
		}
    }
}