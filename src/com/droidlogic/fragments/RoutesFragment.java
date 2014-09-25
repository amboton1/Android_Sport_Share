package com.droidlogic.fragments;

import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.droidlogic.adapter.RoutesAdapter;
import com.droidlogic.database.DBManager;
import com.droidlogic.others.DownloadedTracks;
import com.droidlogic.others.EndomondoFunctions;
import com.droidlogic.others.MyFunctions;
import com.droidlogic.others.RuntasticFunctions;
import com.droidlogic.others.Track;
import com.droidlogic.pfdeporte.MainMenuActivity;
import com.droidlogic.pfdeporte.R;

public class RoutesFragment extends Fragment
{

	private List<Track> routes;
	private ProgressDialog progressDialog;
	private ListView listView;
	private RoutesAdapter adapter;
	private MainMenuActivity activity;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		View v=inflater.inflate(R.layout.routesfragment, container, false);
		listView=(ListView)v.findViewById(R.id.lstRoutes);
		listView.setAdapter(adapter);
		setHasOptionsMenu(true);
		return v;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		activity=(MainMenuActivity)getActivity();
		progressDialog=ProgressDialog.show(activity, 
				getResources().getString(R.string.loadingTitle), 
				getResources().getString(R.string.loadingMsg));
		adapter=new RoutesAdapter(activity);
	}

	
	@Override
	public void onResume()
	{
		super.onResume();
		new LoadTask().execute();
	}

	private void finishLoading()
	{
		adapter.setTracks(routes);
		progressDialog.dismiss();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.btUpdate:
				new LoadTask().execute();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.routesmenu, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	class LoadTask extends AsyncTask<Void, Void, Integer>
	{
		private DBManager dbManager;
		private List<Track> trackList=new ArrayList<Track>();
		
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			progressDialog.show();
			dbManager=new DBManager(activity);
		}

		@Override
		protected void onPostExecute(Integer val)
		{
			super.onPostExecute(val);
			switch(val)
			{
				case -2:
					Toast.makeText(activity, activity.getResources().getString(R.string.dataError)
							+ " Endomondo, Runtastic", Toast.LENGTH_SHORT).show();
					break;
				case -3:
					Toast.makeText(activity, activity.getResources().getString(R.string.dataError)
							+ " Endomondo", Toast.LENGTH_SHORT).show();
					break;
				case -4:
					Toast.makeText(activity, activity.getResources()
							.getString(R.string.dataError)+" Runtastic", Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
			routes=trackList;
			finishLoading();
		}

		@Override
		protected Integer doInBackground(Void... params)
		{
			SharedPreferences sp=activity.getSharedPreferences(MyFunctions.PREFS_NAME, 0);
			int res;
			List<Track> local=dbManager.getTracks(), endomondo=null, runtastic=null;
			DownloadedTracks dt=null;
			boolean loggedEndomondo=sp.getBoolean("loggedEndomondo", false), loggedRuntastic=sp.getBoolean("loggedRuntastic", false);
			if (loggedEndomondo)
			{
				dt=EndomondoFunctions.getEndomondoTracks(activity, local, dbManager);
				endomondo=dt.getTracks();
				res=(dt.isOK())?0:-1;
				dt=RuntasticFunctions.downloadRuntasticWorkouts(activity, endomondo, dbManager);
				runtastic=dt.getTracks();
				if (res==-1)
				{
					if (dt.isOK())
					{
						res=-3;//-3 means Endomondo is wrong
					}
					else
					{
						res=-2; // -2 means both of them are wrong
					}
				}
				else
				{
					if (dt.isOK())
					{
						res=0; //0 means all is ok
					}
					else
					{
						res=-4; // -4 means runtastic is wrong
					}
				}
			}
			else
			{
				res=-3;
				if (loggedRuntastic)
				{
					dt=RuntasticFunctions.downloadRuntasticWorkouts(activity, local, dbManager);
					runtastic=dt.getTracks();
					if (!dt.isOK())
					{
						res=-2;
					}
				}
				else
				{
					res=-2;
					runtastic=local;
				}
			}
			trackList.addAll(runtastic);
			return res;
		}
	}
	
}
