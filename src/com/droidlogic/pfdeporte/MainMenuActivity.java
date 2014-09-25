package com.droidlogic.pfdeporte;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.droidlogic.fragments.MapsFragment;
import com.droidlogic.fragments.RoutesFragment;
import com.droidlogic.fragments.SettingsFragment;
import com.droidlogic.others.MyFunctions;
import com.droidlogic.others.Track;
import com.droidlogic.service.GPSService;
import com.droidlogic.service.GPSService.LocalBinder;

public class MainMenuActivity extends Activity implements MapsFragment.MapListener
{
	private FragmentManager fragmentManager;
	private Fragment currentFragment;
	private DrawerLayout drawerLayout;
	private ListView drawerList;
	private static String[] names;
	private ActionBarDrawerToggle drawerToggle;
	private GPSService service;
	private boolean boundToService = false;
	private MapsFragment mapFragment;
	private RoutesFragment routesFragment;
	private SettingsFragment settingsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainmenu_activity);
		startService(new Intent(this, GPSService.class));
		SharedPreferences sp = getSharedPreferences(MyFunctions.PREFS_NAME, 0);
		names = new String[3];
		names[0] = getResources().getString(R.string.rutas);
		names[1] = getResources().getString(R.string.mapa);
		names[2] = getResources().getString(R.string.opciones);
		fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		mapFragment=new MapsFragment();
		routesFragment=new RoutesFragment();
		settingsFragment=new SettingsFragment();
		
		if (sp.getBoolean("loggedRuntastic", false) || sp.getBoolean("loggedEndomondo", false))
		{
			currentFragment = mapFragment;
		}
		else
		{
			currentFragment=settingsFragment;
		}
		
		fragmentTransaction.replace(R.id.mainfragment, currentFragment);
		fragmentTransaction.commit();
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close)
		{

			@Override
			public void onDrawerClosed(View drawerView)
			{
				super.onDrawerClosed(drawerView);
				getActionBar().setTitle(R.string.app_name);
			}

			@Override
			public void onDrawerOpened(View drawerView)
			{
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle(R.string.menu);
			}

		};
		drawerLayout.setDrawerListener(drawerToggle);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		// Set the adapter for the list view
		drawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, names));
		// Set the list's click listener
		drawerList.setOnItemClickListener(new DrawerItemClickListener());
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event
		if (drawerToggle.onOptionsItemSelected(item))
		{
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	private class DrawerItemClickListener implements OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id)
		{
			switch (position)
			{
				case 0:
					if (!(currentFragment instanceof RoutesFragment))
					{
						currentFragment = routesFragment;
						fragmentManager.beginTransaction()
								.replace(R.id.mainfragment, currentFragment)
								.commit();
						drawerList.setItemChecked(position, true);
						setTitle(names[position]);
					}
					break;
				case 1:
					if (!(currentFragment instanceof MapsFragment))
					{
						currentFragment = mapFragment;
						fragmentManager.beginTransaction()
								.replace(R.id.mainfragment, currentFragment)
								.commit();
						drawerList.setItemChecked(position, true);
						setTitle(names[position]);
					}
					break;
				case 2:
				default:
					if (!(currentFragment instanceof SettingsFragment))
					{
						currentFragment = settingsFragment;
						fragmentManager.beginTransaction()
								.replace(R.id.mainfragment, currentFragment)
								.commit();
						drawerList.setItemChecked(position, true);
						setTitle(names[position]);
					}
					break;
			}
			drawerLayout.closeDrawer(drawerList);
		}
	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder)
		{
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			LocalBinder localBinder = (LocalBinder) binder;
			service = localBinder.getService();
			boundToService = true;
			mapFragment.updateService(MainMenuActivity.this);
			service.setActivity(MainMenuActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			boundToService = false;
		}
	};

	@Override
	public GPSService getService()
	{
		return service;
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unbindService(mConnection);
	}

	@Override
	protected void onStop()
	{
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		bindService(new Intent(this, GPSService.class), 
				mConnection, Context.BIND_AUTO_CREATE);
	}
	
	public void showTrack(Track t)
	{
		currentFragment = mapFragment;
		fragmentManager.beginTransaction()
				.replace(R.id.mainfragment, currentFragment)
				.commit();
		drawerList.setItemChecked(1, true);
		setTitle(names[1]);
		mapFragment.showAllTrack(t);
	}
	
	public boolean isMapFragmentAttached()
	{
		return (currentFragment instanceof MapsFragment);
	}
}
