package com.kkinder.sharelocation;

import java.net.URL;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.Time;
import android.util.Log;
import android.location.GpsStatus;

public class GpsInfo extends Activity {

	private static final String LOG = "MyShare";
	private LocationManager mgr;
	private TextView txt;
	private Button vipButton;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.gpsinfo);
		txt=(TextView)findViewById(R.id.GpsInfoText);
		vipButton = (Button) findViewById(R.id.vipButton);
		
		Log.e(LOG,"Oncreate function start...");

		mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
	
		final String provider = mgr.getBestProvider(criteria, true);
		Log.e(LOG,"LocationManager started...provider: " + provider);
		
		mgr.requestLocationUpdates(provider,1000,0,onLocationChange);
		Log.e(LOG,"Registered for updates...");
	    
		
	    
		vipButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
		Log.d("MyShare:","Bol stlaceny VIP button.");

		Location tmp_loc = mgr.getLastKnownLocation(provider);
		double lon = tmp_loc.getLongitude();
		double lat = tmp_loc.getLatitude();
		
		String location = lat + "," + lon;
		
		//send data to remote server
		try {
			Log.e("MyShare:","Volam HTTP GET kod...");
			URL url = new URL("http://www.moirelabs.com/myshare/?pos=" + location);
			url.getContent();
			Toast.makeText(getBaseContext(), "Data has been sent (" + location + ")", Toast.LENGTH_LONG).show();
		} catch(java.net.MalformedURLException e) {
			Log.e("MyShare:MalformedURL","Chyba:" + e);
			Toast.makeText(getBaseContext(), "Data was NOT SENT - Error.", Toast.LENGTH_LONG).show();
		} catch(java.io.IOException e) { 
			Log.e("MyShare:IO Exception","Chyba:" + e);
			Toast.makeText(getBaseContext(), "Data was NOT SENT - Error.", Toast.LENGTH_LONG).show();
		}
	   }
        });
		
	    txt.setText("Ready...waiting for location provider fix.");
		
	}

	  @Override
	  public void onResume() {
	    super.onResume();
	  }
	
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    //locmanager.removeUpdates(this);
	}

	 LocationListener onLocationChange=new LocationListener() {
		    public void onLocationChanged(Location location) {
		      updateText(location);
		    }
		    
		    public void onProviderDisabled(String provider) {
		      // required for interface, not used
		    }
		    
		    public void onProviderEnabled(String provider) {
		      // required for interface, not used
		    }
		    
		    public void onStatusChanged(String provider, int status,
		                                  Bundle extras) {
		      // required for interface, not used
		    }
		  };
	
		  
	public void updateText(Location loc) {
		Time now = new Time();
		now.setToNow();
		Log.e(LOG,"Trying to parse information...");
		double lon = loc.getLongitude();
		double lat = loc.getLatitude();
		float accuracy = loc.getAccuracy();
		float bearing = loc.getBearing();
		Log.e(LOG,"Information parsed");
		txt=(TextView)findViewById(R.id.GpsInfoText);
		txt.setText("Lon: " + lon + "\nLat: " + lat + "\nAccuracy: " 
				+ (int) accuracy + "m\nBearing: " + bearing + "\n" + now.toString());
		Log.e(LOG,"Position parsed: " + lon + "," + lat + "," + accuracy + "," + bearing);
	}
}



