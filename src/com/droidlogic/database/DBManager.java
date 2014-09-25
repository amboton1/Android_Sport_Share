package com.droidlogic.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.droidlogic.database.TracksContract.TrackColumns;
import com.droidlogic.others.Track;

public class DBManager
{
	private SQLiteHelper helper;
	private Context context;

	public DBManager(Context ct)
	{
		context = ct;
		helper = new SQLiteHelper(context);
	}

	public void update(Long id, String data, String start, String endoId, String runtId)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(TrackColumns.COLUMN_DATA, data);
		values.put(TrackColumns.COLUMN_STARTTIME, start);
		values.put(TrackColumns.COLUMN_ENDOMONDOID, endoId);
		values.put(TrackColumns.COLUMN_RUNTASTICID, runtId);
		// Which row to update, based on the ID
		String selection = TrackColumns._ID + " LIKE ?";
		String[] selectionArgs = { Long.toString(id) };

		// int count =
		db.update(TrackColumns.TABLE_NAME, values, selection, selectionArgs);
		db.close();
	}
	
	public void updateServersIdOnly(Long id, String endoId, String runtId)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(TrackColumns.COLUMN_ENDOMONDOID, endoId);
		values.put(TrackColumns.COLUMN_RUNTASTICID, runtId);
		// Which row to update, based on the ID
		String selection = TrackColumns._ID + " LIKE ?";
		String[] selectionArgs = { Long.toString(id) };

		// int count =
		db.update(TrackColumns.TABLE_NAME, values, selection, selectionArgs);
		db.close();
	}

	public long insert(String data, String start, String endoId, String runtId)
	{
		long res;
		// Gets the data repository in write mode
		SQLiteDatabase db = helper.getWritableDatabase();

		// Create a new map of values, where column names are the keys
		ContentValues values = new ContentValues();
		values.put(TrackColumns.COLUMN_STARTTIME, start);
		values.put(TrackColumns.COLUMN_DATA, data);
		values.put(TrackColumns.COLUMN_ENDOMONDOID, endoId);
		values.put(TrackColumns.COLUMN_RUNTASTICID, runtId);
		// Insert the new row, returning the primary key value of the new row
		// long newRowId =
		res = db.insert(TrackColumns.TABLE_NAME, null, values);
		db.close();
		return res;
	}

	public void remove(Long id)
	{
		// Gets the data repository in write mode
		SQLiteDatabase db = helper.getWritableDatabase();
		// Define 'where' part of query.
		String selection = TrackColumns._ID + " LIKE ?";
		// Specify arguments in placeholder order.
		String[] selectionArgs = { String.valueOf(id) };
		// Issue SQL statement.
		db.delete(TrackColumns.TABLE_NAME, selection, selectionArgs);
		db.close();
	}
	
	public SQLiteDatabase getReadableDatabase()
	{
		return helper.getReadableDatabase();
	}
	
	public SQLiteDatabase getWritableDatabase()
	{
		return helper.getWritableDatabase();
	}
	
	public List<Track> getTracks()
	{
		String projection[]={TrackColumns._ID, TrackColumns.COLUMN_DATA, TrackColumns.COLUMN_ENDOMONDOID, TrackColumns.COLUMN_RUNTASTICID}, 
				sortOrder = TrackColumns.COLUMN_STARTTIME + " DESC";
		Cursor c=helper.getReadableDatabase().query(TrackColumns.TABLE_NAME,
				projection, null, null, null, null, sortOrder);
		List<Track> tracks=new ArrayList<Track>();
		Track aux;
		while(c.moveToNext())
		{
			aux=new Track(c.getLong(0), c.getString(1));
			aux.setEndomondoWorkoutId(c.getString(2));
			aux.setRuntasticWorkoutId(c.getString(3));
			aux.setInLocalStorage(true);
			tracks.add(aux);
		}
		return tracks;
	}
}