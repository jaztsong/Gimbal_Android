package com.example.testproximity;

import java.util.ArrayList;

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class DataProvider extends ContentProvider {
	private static final String pmURI =
			"content://com.example.testproximity.provider.phonemonitor/phonemonitor";
	private static final String dcURI =
			"content://com.example.testproximity.provider.phonemonitor/digitalcommunication";
	public static final Uri PM_URI = Uri.parse(pmURI);
	public static final Uri DC_URI = Uri.parse(dcURI);
	public static final int PM_TYPE = 0;
	public static final int DC_TYPE = 1;

	//The Database
	private static final String TAG = "DataProvider";
	private static final String DATABASE_NAME = "phonemonitor.db";
	private static final int DATABASE_VERSION = 1;
	private static final String PHONEMONITOR_TABLE = "phonemonitor";
	private static final String DIGITALCOMMUNICATION_TABLE = "digitalcommunication";

	//Column Names in phonemonitor table
	public static final String PM_KEY_ID = "_id";
	public static final String PM_KEY_TIME = "time";
	public static final String PM_KEY_TYPE = "type";
	public static final String PM_KEY_NAME = "name";
	public static final String PM_KEY_ADDRESS = "address";
	public static final String PM_KEY_STRENGTH = "strength";

	//Column Indexes in phonemonitor table
	public static final int PM_ID_COLUMN = 0;
	public static final int PM_TIME_COLUMN = 1;
	public static final int PM_TYPE_COLUMN = 2;
	public static final int PM_NAME_COLUMN = 3;
	public static final int PM_ADDRESS_COLUMN = 4;
	public static final int PM_STRENGTH_COLUMN = 5;

	//Column Names in digitalcommunication table
	public static final String DC_KEY_ID = "_id";
	public static final String DC_KEY_TIME = "time";
	public static final String DC_KEY_TYPE = "type";
	public static final String DC_KEY_SENDER = "sender";
	public static final String DC_KEY_RECEIVER = "receiver";
	public static final String DC_KEY_LENGTH = "length";

	//Column Indexes in phonemonitor table
	public static final int DC_ID_COLUMN = 0;
	public static final int DC_TIME_COLUMN = 1;
	public static final int DC_TYPE_COLUMN = 2;
	public static final int DC_SENDER_COLUMN = 3;
	public static final int DC_RECEIVER_COLUMN = 4;
	public static final int DC_LENGTH_COLUMN = 5;

	private int version = DATABASE_VERSION;

	private SQLiteDatabase phonemonitorDB;
	private PhoneMonitorDBHelper dbHelper;
	private HubServerChangeReceiver hubServerChangeReceiver;

	public int getVersion() {
		return version;
	}

	class HubServerChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			dropTable();
			//        	dbHelper.truncateDatabase();
		}
	}

	//Helper class
	private static class PhoneMonitorDBHelper extends SQLiteOpenHelper {
		private static final String PM_DATABASE_CREATE =
				"create table " + PHONEMONITOR_TABLE + "("
						+ PM_KEY_ID + " integer primary key autoincrement,"
						+ PM_KEY_TIME + " INTEGER,"
						+ PM_KEY_TYPE + " TEXT,"
						+ PM_KEY_NAME + " TEXT,"
						+ PM_KEY_ADDRESS + " TEXT,"
						+ PM_KEY_STRENGTH + " INTEGER);";

		private static final String DC_DATABASE_CREATE =
				"create table " + DIGITALCOMMUNICATION_TABLE + "("
						+ DC_KEY_ID + " integer primary key autoincrement,"
						+ DC_KEY_TIME + " INTEGER,"
						+ DC_KEY_TYPE + " TEXT,"
						+ DC_KEY_SENDER + " TEXT,"
						+ DC_KEY_RECEIVER + " TEXT,"
						+ DC_KEY_LENGTH + " INTEGER);";

		public PhoneMonitorDBHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(PM_DATABASE_CREATE);
			db.execSQL(DC_DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + PHONEMONITOR_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + DIGITALCOMMUNICATION_TABLE);
			onCreate(db);
		}

		public void truncateDatabase() {
			SQLiteDatabase db = getWritableDatabase();
			if(db == null)
				return;

			db.execSQL("DROP TABLE IF EXISTS " + PHONEMONITOR_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + DIGITALCOMMUNICATION_TABLE);
			onCreate(db);
		}
	}

	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		String table = null;
		if (uri == PM_URI) {
			table = PHONEMONITOR_TABLE;
		} else if (uri == DC_URI) {
			table = DIGITALCOMMUNICATION_TABLE;
		}
		int count = phonemonitorDB.delete(table, selection, selectionArgs);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized Uri insert(Uri _uri, ContentValues values) {
		String table = _uri.getLastPathSegment();
		long rowID = phonemonitorDB.insert(table, null, values);
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(_uri, rowID);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		} else
			throw new SQLException("Failed insert row into " + _uri);
	}
	@Override
	public synchronized int bulkInsert(Uri _uri, ContentValues[] values) {
		String table = _uri.getLastPathSegment();
		String sql = "INSERT INTO "+ table +" VALUES (?,?,?,?,?,?);";
		SQLiteStatement statement = phonemonitorDB.compileStatement(sql);
		phonemonitorDB.beginTransaction();
		for (int i = 0; i<NetscaleProximityService.MAXNUM_RECORD; i++) {
			ContentValues value = values[i];
			statement.clearBindings();
			statement.bindNull(1);
			statement.bindLong(2,value.getAsLong(PM_KEY_TIME));
			statement.bindString(3, value.getAsString(PM_KEY_TYPE));
			statement.bindString(4, value.getAsString(PM_KEY_NAME));
			statement.bindString(5, value.getAsString(PM_KEY_ADDRESS));
			statement.bindLong(6, value.getAsLong(PM_KEY_STRENGTH));
			statement.execute();
		}
		phonemonitorDB.setTransactionSuccessful();	
		phonemonitorDB.endTransaction();
		return 0;
	} 

	public synchronized void dropTable() {
		Context context = getContext();
		dbHelper.onUpgrade(phonemonitorDB, version, version + 1);
	}

	@Override
	public synchronized boolean onCreate() {
		Context context = getContext();
		dbHelper = new PhoneMonitorDBHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		phonemonitorDB = dbHelper.getWritableDatabase();
		return (phonemonitorDB == null) ? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		//builder.setTables(PHONEMONITOR_TABLE,DIGITALCOMMUNICATION_TABLE);
		if (uri == PM_URI) {
			builder.setTables(PHONEMONITOR_TABLE);
		} else if (uri == DC_URI) {
			builder.setTables(DIGITALCOMMUNICATION_TABLE);
		}


		/*
        String orderBy;
		if (TextUtils.isEmpty(sortOrder))
			orderBy = KEY_TIME;
		else 
			orderBy = sortOrder;
		 */

		Cursor c = builder.query(phonemonitorDB, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}


}
