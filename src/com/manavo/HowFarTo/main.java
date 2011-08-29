package com.manavo.HowFarTo;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class main extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    public void searchLocation(View v) {
    	EditText location = (EditText)this.findViewById(R.id.location);
    	TextView result = (TextView)this.findViewById(R.id.result);
    	
    	String sLocation = location.getText().toString();
    	
    	Geocoder g = new Geocoder(this);
    	try {
			List<Address> addresses = g.getFromLocationName(sLocation, 5);
			Iterator<Address> i = addresses.iterator();
			Address a;
			String sResult = "";
			while (i.hasNext()) {
				a = i.next();
				sResult += a.getAddressLine(0)+"\n";
			}
			result.setText(sResult);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	    	Toast.makeText(this, "Could not connect. Please try again!", Toast.LENGTH_SHORT).show();
		}
    }
}