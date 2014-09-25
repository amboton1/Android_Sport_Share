package com.droidlogic.adapter;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.droidlogic.database.DBManager;
import com.droidlogic.others.EndomondoFunctions;
import com.droidlogic.others.RuntasticFunctions;
import com.droidlogic.others.Track;
import com.droidlogic.pfdeporte.MainMenuActivity;
import com.droidlogic.pfdeporte.R;

public class RoutesAdapter extends BaseAdapter
{

	private List<Track> routes;
	private static MainMenuActivity context;
	private static DBManager dbManager;

	public RoutesAdapter(MainMenuActivity ctx, List<Track> rts)
	{
		context = ctx;
		routes = rts;
		dbManager = new DBManager(context);
	}

	public RoutesAdapter(MainMenuActivity ctx)
	{
		context = ctx;
		routes = new ArrayList<Track>();
		dbManager = new DBManager(context);
	}

	@Override
	public int getCount()
	{
		return routes.size();
	}

	@Override
	public Object getItem(int position)
	{
		return routes.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}
		
	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		final ViewHolder holder;
		final Track aux = routes.get(position);
		
		if (convertView == null)
		{
			LayoutInflater inflater = LayoutInflater.from(context);
			convertView = inflater.inflate(R.layout.route_row, null);
			holder = new ViewHolder();
			holder.titulo = (TextView) convertView.findViewById(R.id.txtName);
			holder.endomondo = (CheckBox) convertView
					.findViewById(R.id.chkEndomondo);
			holder.runtastic = (CheckBox) convertView
					.findViewById(R.id.chkRuntastic);
			holder.local = (CheckBox) convertView.findViewById(R.id.chkLocal);
			convertView.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}

		holder.titulo.setText(aux.getTitle());
		holder.endomondo.setChecked(aux.isUploadedToEndomondo());
		holder.runtastic.setChecked(aux.isUploadedToRuntastic());
		holder.local.setChecked(aux.isInLocalStorage());
		
		OnClickListener listener = new OnClickListener()
		{	
			@Override
			public void onClick(View v)
			{
				switch (v.getId())
				{
					case R.id.chkEndomondo:
						if (holder.endomondo.isChecked())
						{
							new EndomondoUploadTask().execute(aux);
							aux.setUploadedToEndomondo(true);
						}
						else
						{
							new EndomondoRemoveTask().execute(aux);
							aux.setUploadedToEndomondo(false);
							if (!holder.local.isChecked() && !holder.runtastic.isChecked())
							{
								routes.remove(position);
								notifyDataSetChanged();
							}
						}
						break;
					case R.id.chkLocal:
						if (holder.local.isChecked())
						{
							new InsertTask().execute(aux);
							aux.setInLocalStorage(true);
						}
						else
						{
							dbManager.remove(aux.getID());
							if (!holder.endomondo.isChecked() && !holder.runtastic.isChecked())
							{
								routes.remove(position);
								notifyDataSetChanged();
							}
							aux.setInLocalStorage(false);
						}
						break;
					case R.id.chkRuntastic:
						if (holder.runtastic.isChecked())
						{
							new RuntasticUploadTask().execute(aux);
							aux.setUploadedToRuntastic(true);
						}
						else
						{
							new RuntasticRemoveTask().execute(aux);
							aux.setUploadedToRuntastic(false);
							if (!holder.local.isChecked() && !holder.endomondo.isChecked())
							{
								routes.remove(position);
								notifyDataSetChanged();
							}
						}
						break;
				}
				
			}
			
		};
		
		holder.endomondo.setOnClickListener(listener);
		holder.runtastic.setOnClickListener(listener);
		holder.local.setOnClickListener(listener);
		
		convertView.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v)
			{
				Track t=routes.get(position);
				context.showTrack(t);
			}
			
		});
		return convertView;
	}

	static class ViewHolder
	{
		TextView titulo;
		CheckBox endomondo, runtastic, local;
	}

	public void clear()
	{
		routes.clear();
	}
//
//	public void addTrack(Track trk)
//	{
//		routes.add(trk);
//		notifyDataSetChanged();
//	}

	public void setTracks(List<Track> trks)
	{
		routes.clear();
		routes.addAll(trks);
		notifyDataSetChanged();
	}
	
	private static class InsertTask extends AsyncTask<Track, Void, Void>
	{
		@Override
		protected Void doInBackground(Track... params)
		{
			Track aux=params[0];
			dbManager.insert(aux.exportAsGPX(), aux.getStartTime(), aux.getEndomondoWorkoutId(), aux.getRuntasticWorkoutId());
			return null;
		}
		
	}
	
	private static class EndomondoRemoveTask extends AsyncTask<Track, Void, Void>
	{
		@Override
		protected Void doInBackground(Track... params)
		{
			Track t=params[0];
			EndomondoFunctions.removeEndomondoWorkout(context, t.getEndomondoWorkoutId());
			return null;
		}
	}
	
	private static class EndomondoUploadTask extends AsyncTask<Track, Void, Void>
	{
		@Override
		protected Void doInBackground(Track... params)
		{
			Track t=params[0];
			EndomondoFunctions.uploadEndomondoWorkout(context, t);
			return null;
		}
	}
	
	private static class RuntasticRemoveTask extends AsyncTask<Track, Void, Void>
	{
		@Override
		protected Void doInBackground(Track... params)
		{
			Track t=params[0];
			RuntasticFunctions.deleteTrackRuntastic(context, t.getRuntasticWorkoutId());
			return null;
		}
	}
	
	private static class RuntasticUploadTask extends AsyncTask<Track, Void, Void>
	{
		@Override
		protected Void doInBackground(Track... params)
		{
			Track t=params[0];
			String trackId=RuntasticFunctions.uploadRuntasticWorkout(context, t);
			if (trackId!=null)
			{
				t.setRuntasticWorkoutId(trackId);
			}
			return null;
		}
	}
}
