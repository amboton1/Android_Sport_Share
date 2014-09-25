package com.droidlogic.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.droidlogic.database.DBManager;
import com.droidlogic.fragments.MapsFragment;
import com.droidlogic.others.MyFunctions;
import com.droidlogic.others.Track;
import com.droidlogic.pfdeporte.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class GPSService extends Service implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener
{
	private boolean recording = false; // Represents if the app is recording the
										// locations or not
	private List<Location> locations;
	private LocationRequest locationRequest;
	private static final int UPDATE_INTERVAL = 5000;
	private static final long FASTEST_INTERVAL = 1000;
	private static final float MIN_DISTANCE = 3f;
	private LocationClient locationClient;
	private MapsFragment mapFragment;
	private static final boolean DEBUGMODE=false;
	private DBManager dbManager;
	private Activity activity;
	
	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		initialize();
	}

	public void setActivity(Activity a)
	{
		activity=a;
	}

	public class LocalBinder extends Binder
	{
		public GPSService getService()
		{
			// Return this instance of LocalService so clients can call public
			// methods
			return GPSService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult)
	{
		Log.d(MyFunctions.TAG, "[+] Connection failed");
	}

	public void playWasPressed()
	{
		Track track;
		if (recording)
		{
			stopLocationUpdates();
			track=new Track(locations);
			askSaveTrack(track);
		}
		else
		{
			locations = new ArrayList<Location>();
			locationClient.connect();
		}
		recording = !recording;
	}

	public boolean isRecording()
	{
		return recording;
	}

	@Override
	public void onConnected(Bundle arg0)
	{
		Log.d(MyFunctions.TAG, "[+] Connected");
		if (recording)
		{
			locationClient.requestLocationUpdates(locationRequest, this);
		}
	}

	private void initialize()
	{
		locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(UPDATE_INTERVAL);
		locationRequest.setSmallestDisplacement(MIN_DISTANCE);
		locationRequest.setFastestInterval(FASTEST_INTERVAL);
		locationClient = new LocationClient(this, this, this);
		dbManager=new DBManager(this);
	}

	private void stopLocationUpdates()
	{
		// If the client is connected
		if (locationClient.isConnected())
		{
			/*
			 * Remove location updates for a listener. The current Activity is
			 * the listener, so the argument is "this".
			 */
			locationClient.removeLocationUpdates(this);
		}
		/*
		 * After disconnect() is called, the client is considered "dead".
		 */
		locationClient.disconnect();
	}

	@Override
	public void onDisconnected()
	{
		Log.d(MyFunctions.TAG, "[+] Disconnected");
	}

	public void setMapsFragment(MapsFragment m)
	{
		mapFragment = m;
	}

	@Override
	public void onLocationChanged(Location location)
	{
		if (!DEBUGMODE)
		{
			locations.add(location);
			if (mapFragment != null)
			{
				mapFragment.locationUpdated();
			}
		}
	}

	public void addLocation(Location location)
	{
		if (isRecording())
		{
			locations.add(location);
			if (mapFragment != null)
			{
				mapFragment.locationUpdated();
			}
		}
	}
	
	public List<Location> getLocations()
	{
		return locations;
	}
	
	private void askSaveTrack(final Track track)
	{
		Builder about = new AlertDialog.Builder(activity);

		about.setTitle(getResources().getString(R.string.saveTrack));
		about.setCancelable(false);
		about.setPositiveButton(getResources().getString(android.R.string.yes),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface arg0, int arg1)
					{
						
						dbManager.insert(track.exportAsGPX(), track.getStartTime(), track.getEndomondoWorkoutId(), track.getRuntasticWorkoutId());
					}
				});

		about.setNegativeButton(getResources().getString(android.R.string.no),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface arg0, int arg1)
					{
						arg0.dismiss();
					}
				});
		about.show();
	}
	
	public void setLocations(List<Location> locs)
	{
		locations=locs;
	}
}
