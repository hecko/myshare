package com.kkinder.sharelocation;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.kkinder.sharelocation.R;

public class Sharelocation extends MapActivity {
    private MapView           mapView;
    private MapController     mapController;
    private MyLocationOverlay locationOverlay;
    private LinearLayout      acquiringLayout;
    private Button            sendButton;
    private Button            vipButton;
    private GeoPoint          lastLocation;
    private Dialog            dialog;
    private Boolean           satelliteMode;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (satelliteMode) {
            menu.findItem(R.id.satellite_mode).setVisible(false);
            menu.findItem(R.id.map_mode).setVisible(true);
        } else {
            menu.findItem(R.id.satellite_mode).setVisible(true);
            menu.findItem(R.id.map_mode).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.about:
            dialog = new Dialog(Sharelocation.this);
            
            dialog.setContentView(R.layout.about);
            TextView aboutText = (TextView) dialog.findViewById(R.id.AboutTextView);
            aboutText.setText(Html.fromHtml(getString(R.string.about)));
            aboutText.setMovementMethod(LinkMovementMethod.getInstance());

            dialog.setTitle(R.string.about_title);
            dialog.setCancelable(true);
            
            Button closeButton = (Button) dialog.findViewById(R.id.CloseButton);
            
            closeButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    dialog.hide();
                }
            });
            
            dialog.show();
            return true;
        case R.id.map_mode:
            satelliteMode = false;
            mapView.setSatellite(false);
            return true;
        case R.id.satellite_mode:
            satelliteMode = true;
            mapView.setSatellite(true);
            return true;
        case R.id.preferences:
            Intent settingsActivity = new Intent(getBaseContext(),
                    Preferences.class);
            startActivity(settingsActivity);
            return true;
        case R.id.recenter:
            lastLocation = locationOverlay.getMyLocation();
            if (lastLocation != null) {
            	mapController.setZoom(19);
            	mapController.animateTo(lastLocation);
            }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        SharedPreferences settings = getPreferences(MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("satelliteMode", satelliteMode);

        // Commit the edits!
        editor.commit();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        locationOverlay.disableMyLocation();
        locationOverlay.disableCompass();
    };

    @Override
    protected void onResume() {
        super.onResume();
        locationOverlay.enableMyLocation();
        locationOverlay.enableCompass();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SharedPreferences settings = getPreferences(MODE_WORLD_WRITEABLE);
        satelliteMode = settings.getBoolean("satelliteMode", true);

        // Get all our UI components
        acquiringLayout = (LinearLayout) findViewById(R.id.acquiring_signal_layout);
        sendButton = (Button) findViewById(R.id.SendButton);
	vipButton = (Button) findViewById(R.id.VipButton);
        mapView = (MapView) findViewById(R.id.MapView);

        // Set up mapping
        mapController = mapView.getController();
        mapView.setBuiltInZoomControls(false);
        if (satelliteMode) {
            mapView.setSatellite(true);
        } else {
            mapView.setSatellite(false);
        }
        locationOverlay = new MyLocationOverlay(this, mapView);
        locationOverlay.enableMyLocation();
        locationOverlay.enableCompass();
        mapView.getOverlays().add(locationOverlay);

	// https://developers.google.com/maps/documentation/android/reference/com/google/android/maps/MyLocationOverlay
        // Trigger for acquiring location -- also appears to trigger of
        // location is significantly updated as accuracy increases.
        locationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                if (acquiringLayout.getHandler() != null) {
                    acquiringLayout.getHandler().post(new Runnable() {
                        public void run() {
                            lastLocation = locationOverlay.getMyLocation();
                            if (lastLocation != null) {
                                mapController.animateTo(lastLocation);
                                mapController.setZoom(19);
                                acquiringLayout.setVisibility(View.GONE);
                                sendButton.setVisibility(View.VISIBLE);
				vipButton.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }
        });
        
	// sending information code starts here
        sendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Context context = getApplicationContext();
                
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context); 
                
                if (locationOverlay.getMyLocation() != null) {
                    lastLocation = locationOverlay.getMyLocation(); 
                }
                
		Time now = new Time();
		now.setToNow();
                Intent i = new Intent(android.content.Intent.ACTION_SEND);
                CharSequence location = lastLocation.getLatitudeE6() / 1E6 + "," + lastLocation.getLongitudeE6() / 1E6;
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, settings.getString("message_subject", getString(R.string.share_location_subject)));
                i.putExtra(Intent.EXTRA_TEXT, settings.getString("message_body", getString(R.string.share_location_body)) 
                		+ " http://maps.google.com/maps?q=loc:"
                        	+ location + " at " + now.toString());

		Log.v("MyShare:","Posielam udaje pre " + location);

		//send data via choosen service
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.share_title)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(context, getString(R.string.no_way_to_share), Toast.LENGTH_LONG).show();
                }
            }
        });
	
	//sending information to VIP person
	vipButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
		Log.d("MyShare:","Bol stlaceny VIP button.");

                Context context = getApplicationContext();

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

                if (locationOverlay.getMyLocation() != null) {
                    lastLocation = locationOverlay.getMyLocation();
                }

                Intent i = new Intent(android.content.Intent.ACTION_SEND);
                CharSequence location = lastLocation.getLatitudeE6() / 1E6 + "," + lastLocation.getLongitudeE6() / 1E6;
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, settings.getString("message_subject", getString(R.string.share_location_subject)));
                i.putExtra(Intent.EXTRA_TEXT, settings.getString("message_body", getString(R.string.share_location_body))
                                + " http://maps.google.com/maps?q=loc:"
                                + location); 

		//send data to remote server
		try {
			Log.e("MyShare:","Volam HTTP GET kod...");
			URL url = new URL("http://www.moirelabs.com/myshare/?pos=" + location);
			url.getContent();
			Toast.makeText(context, "Data has been sent (" + location + ")", Toast.LENGTH_LONG).show();
		} catch(java.net.MalformedURLException e) {
			Log.e("MyShare:MalformedURL","Chyba:" + e);
			Toast.makeText(context, "Data was NOT SENT - Error.", Toast.LENGTH_LONG).show();
		} catch(java.io.IOException e) { 
			Log.e("MyShare:IO Exception","Chyba:" + e);
			Toast.makeText(context, "Data was NOT SENT - Error.", Toast.LENGTH_LONG).show();
		}
	   }
        });
    }

    @Override
    protected boolean isRouteDisplayed() {
        // Required to override; do nothing.
        return false;
    }
}
