package com.droidlogic.fragments;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.droidlogic.others.MyFunctions;
import com.droidlogic.others.Track;
import com.droidlogic.pfdeporte.MainMenuActivity;
import com.droidlogic.pfdeporte.R;
import com.droidlogic.service.GPSService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsFragment extends Fragment
{
	private MapView mapView;
	private GoogleMap map;
	private Polyline line;
	private PolylineOptions lineOptions;
	private Spinner spData;
	private static final DecimalFormat format = new DecimalFormat("#.##");;
	private Marker start, end;
	private PowerManager powerManager;
	private PowerManager.WakeLock wakeLock;
	private static final float FIRST_ZOOM = 16f;
	private MainMenuActivity activity;
	private GPSService service;
	private ArrayAdapter<String> adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.mapsfragment, container, false);
		spData = (Spinner) v.findViewById(R.id.spData);
		spData.setAdapter(adapter);

		setHasOptionsMenu(true);
		// Gets the MapView from the XML layout and creates it
		mapView = (MapView) v.findViewById(R.id.mapview);
		mapView.onCreate(savedInstanceState);

		// Gets to GoogleMap from the MapView and does initialization stuff
		map = mapView.getMap();
		// map.getUiSettings().setMyLocationButtonEnabled(false);
		map.setMyLocationEnabled(true);
		// TODO
		map.setOnMapClickListener(new OnMapClickListener()
		{

			@Override
			public void onMapClick(LatLng point)
			{
				if (service != null)
				{
					Time timer = new Time();
					timer.setToNow();
					Location aux = new Location("PFDeporte");
					aux.setLatitude(point.latitude);
					aux.setLongitude(point.longitude);
					aux.setTime(timer.toMillis(false));
					service.addLocation(aux);
				}
			}
		});
		// Needs to call MapsInitializer before doing any CameraUpdateFactory
		// calls
		try
		{
			MapsInitializer.initialize(this.getActivity());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return v;
	}

	public void updateService(MainMenuActivity act)
	{
		if (activity == null)
		{
			activity = act;
		}
		service = activity.getService();
		service.setMapsFragment(this);
		if (activity.isMapFragmentAttached())
		{
			// Distance
			adapter.remove(adapter.getItem(0));
			adapter.insert(
					getResources().getString(R.string.distancia)
							+ " "
							+ format.format(MyFunctions.getDistance(service
									.getLocations())), 0);
			// Time
			Calendar c = Calendar.getInstance();
			adapter.remove(adapter.getItem(1));
			c.setTimeInMillis(MyFunctions.getTime(service.getLocations()));
			adapter.insert(getResources().getString(R.string.tiempo) + " "
					+ String.format("%02d", c.get(Calendar.MINUTE) / 60) + ":"
					+ String.format("%02d", c.get(Calendar.MINUTE)) + ":"
					+ String.format("%02d", c.get(Calendar.SECOND)), 1);
			adapter.notifyDataSetChanged();

		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.mapmenu, menu);
		if (service != null && service.isRecording())
		{
			MenuItem item = menu.findItem(R.id.btPlay);
			item.setIcon(android.R.drawable.ic_media_pause);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.btLayer:
				if (map.getMapType() == GoogleMap.MAP_TYPE_NORMAL)
				{
					map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				}
				else
				{
					map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				}
				return true;
			case R.id.btPlay:
				List<Location> locations = service.getLocations();
				if (service.isRecording())
				{
					item.setIcon(android.R.drawable.ic_media_play);
					if (wakeLock.isHeld())
					{
						wakeLock.release();
					}
					end = map.addMarker(new MarkerOptions().position(
							MyFunctions.locationToLatLng(locations
									.get(locations.size() - 1))).title(
							getResources().getString(R.string.end)));
					end.showInfoWindow();
				}
				else
				{
					item.setIcon(android.R.drawable.ic_media_pause);
					wakeLock.acquire();
					if (line != null)
					{
						line.remove();
					}
					if (start != null)
					{
						start.remove();
					}
					if (end != null)
					{
						end.remove();
					}
				}
				service.playWasPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStop()
	{
		if (wakeLock.isHeld())
		{
			wakeLock.release();
		}
		super.onStop();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		List<String> spinnerArray = new ArrayList<String>();
		spinnerArray.add(getResources().getString(R.string.distancia));
		spinnerArray.add(getResources().getString(R.string.tiempo)
				+ " 00:00:00");
		adapter = new ArrayAdapter<String>(this.getActivity(),
				android.R.layout.simple_spinner_item, spinnerArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		activity = (MainMenuActivity) getActivity();
		powerManager = (PowerManager) getActivity().getSystemService(
				Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				MyFunctions.TAG);
	}

	@Override
	public void onDestroy()
	{
		mapView.onDestroy();
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		mapView.onPause();
		super.onPause();
	}

	@Override
	public void onResume()
	{
		mapView.onResume();
		if (service != null)
		{
			List<Location> locations = service.getLocations();
			if (service.isRecording())
			{
				adapter.remove(adapter.getItem(0));
				adapter.insert(
						getResources().getString(R.string.distancia)
								+ " "
								+ format.format(MyFunctions
										.getDistance(locations)), 0);
				// Time
				Calendar c = Calendar.getInstance();
				adapter.remove(adapter.getItem(1));
				c.setTimeInMillis(MyFunctions.getTime(locations));
				adapter.insert(getResources().getString(R.string.tiempo) + " "
						+ String.format("%02d", c.get(Calendar.MINUTE) / 60)
						+ ":" + String.format("%02d", c.get(Calendar.MINUTE))
						+ ":" + String.format("%02d", c.get(Calendar.SECOND)),
						1);
				adapter.notifyDataSetChanged();
				new myAsyncTask().execute(true);
			}
			else
			{
				adapter.remove(adapter.getItem(0));
				adapter.insert(getResources().getString(R.string.distancia)
						+ " 0", 0);
				Calendar c = Calendar.getInstance();
				adapter.remove(adapter.getItem(1));
				c.setTimeInMillis(MyFunctions.getTime(locations));
				adapter.insert(getResources().getString(R.string.tiempo)
						+ " 00:00:00", 1);
				adapter.notifyDataSetChanged();

			}
		}
		super.onResume();
	}

	@Override
	public void onLowMemory()
	{
		mapView.onLowMemory();
		super.onLowMemory();
	}

	private class myAsyncTask extends AsyncTask<Boolean, Void, Void>
	{

		List<LatLng> locs;
		String distance;
		String time;
		boolean firstTime, finished;

		@Override
		protected void onPostExecute(Void v)
		{
			LatLng startPoint = locs.get(0), endPoint;
			if (firstTime)
			{
				start = map.addMarker(new MarkerOptions().position(startPoint)
						.title(getResources().getString(R.string.start)));
				start.showInfoWindow();
				map.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint,
						FIRST_ZOOM));
			}

			if (finished)
			{
				endPoint = locs.get(locs.size() - 1);
				end = map.addMarker(new MarkerOptions().position(endPoint)
						.title(getResources().getString(R.string.end)));
				end.showInfoWindow();
			}

			adapter.remove(adapter.getItem(0));
			adapter.insert(getResources().getString(R.string.distancia) + " "
					+ distance, 0);
			adapter.remove(adapter.getItem(1));
			adapter.insert(getResources().getString(R.string.tiempo) + " "
					+ time, 1);
			adapter.notifyDataSetChanged();

			lineOptions = new PolylineOptions();
			lineOptions.color(getResources().getColor(R.color.transparentRed));
			lineOptions.addAll(locs);
			if (line!=null)
			{
				line.remove();
			}
			line = map.addPolyline(lineOptions);

			super.onPostExecute(v);
		}

		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Boolean... params)
		{
			List<Location> locations = service.getLocations();
			firstTime = (params.length >= 1) ? params[0]
					: locations.size() == 1;
			finished = (params.length >= 2) ? params[1] : false;
			locs = MyFunctions.locationsToLatLng(locations);
			distance = format.format(MyFunctions.getDistance(locations));
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(MyFunctions.getTime(locations));
			time = String.format("%02d", c.get(Calendar.MINUTE) / 60) + ":"
					+ String.format("%02d", c.get(Calendar.MINUTE)) + ":"
					+ String.format("%02d", c.get(Calendar.SECOND));
			return null;
		}
	}

	public void locationUpdated()
	{
		new myAsyncTask().execute();
	}

	public interface MapListener
	{
		public GPSService getService();
	}

	/**
	 * Shows the complete path of a track passed as parameter
	 * 
	 * @param t
	 *            the track to draw on the map
	 */
	public void showAllTrack(Track t)
	{
		service.setLocations(t.getLocations());
		new myAsyncTask().execute(true, true);
	}
}
