package com.droidlogic.database;

import android.provider.BaseColumns;

public class TracksContract
{
	// To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public TracksContract() {}
    
    protected static final String TEXT_TYPE = " TEXT";
    protected static final String DATE_TYPE = " DATE";
	protected static final String COMMA_SEP = ",";
	protected static final String SQL_CREATE_ENTRIES =
	    "CREATE TABLE " + TrackColumns.TABLE_NAME + " (" +
	    TrackColumns._ID + " INTEGER PRIMARY KEY," +
	    TrackColumns.COLUMN_DATA + TEXT_TYPE + COMMA_SEP +
	    TrackColumns.COLUMN_STARTTIME + DATE_TYPE + COMMA_SEP +
	    TrackColumns.COLUMN_ENDOMONDOID + TEXT_TYPE + COMMA_SEP +
	    TrackColumns.COLUMN_RUNTASTICID + TEXT_TYPE +
	     // Any other options for the CREATE command
	    " )";

	protected static final String SQL_DELETE_ENTRIES =
	    "DROP TABLE IF EXISTS " + TrackColumns.TABLE_NAME;
    
    /* Inner class that defines the table contents */
    public static abstract class TrackColumns implements BaseColumns 
    {
        public static final String TABLE_NAME = "tracks",
        		COLUMN_DATA="data",
        COLUMN_STARTTIME="start",
        COLUMN_ENDOMONDOID="endomondoID",
        COLUMN_RUNTASTICID="runtasticID";
    }
}
