package com.example.testproximity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class PhoneHomeService extends Service {
	private static final int MAX_NUM_RECORDS = 100;
	private int maxId_PM = 0, minId_PM = 0;
	final static String TAG="PhoneHome";
	private BgTask lastLookup = null;



	private class BgTask extends AsyncTask<Void, Void, Void> {
		public BgTask() {
			super();

		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				sendPackets("10.17.235.208",1234);
			} catch (Exception ex) {				
				ex.printStackTrace();
			}
			publishProgress();
			return null;
		}

		protected void onProgressUpdate(Void... params) {
			super.onProgressUpdate(params);
		}

		@Override
		protected void onPostExecute(Void result) {
			stopSelf();
		}
	}
	public void sendPackets(String servIP, int servPort) {
		Socket socket = null;
		long checkinTime;
		Log.i("Phonehome", "Sending**********************yy");

		// generate the check in time
		checkinTime = System.currentTimeMillis() / 1000;

		// get the apk version
		//String sp_version = getUserPreference(MainActivity.VERSION);
		float version = 1.1f;//Float.parseFloat(sp_version);


		// connect with the server
		long time = 0;
		try {
			socket = new Socket(servIP, servPort);
			// PULL START TIME HERE
			time = System.currentTimeMillis() / 1000;
			//OutputStream out = socket.getOutputStream();
			//String msg="Hello Server!!";

			//out.write(msg.getBytes());
			Log.d(TAG,"TESTING SENDING DATA!!");
			//Toast.makeText(context.getApplicationContext(), "Send Data!!", Toast.LENGTH_SHORT).show();
			sendDataToServer(DataProvider.PM_URI, socket, checkinTime, version);


		} catch (Exception ex) {

			ex.printStackTrace();
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	public void sendDataToServer(Uri uri, Socket socket, long checkinTime,
			float version) throws Exception {
		ContentResolver cr;
		Cursor cur;
		int totalRrd; // the number of records in the database
		int pktCnt; // the number of packet generated
		byte[] pkt; // the content of a packet

		long currentTime;


		cr = getContentResolver();
		cur = cr.query (uri, null, null, null, null);
		// the number of records in the database
		totalRrd = cur.getCount(); // the total number of records
		if (totalRrd == 0)
			return;

		// the number of packets need to send .
		pktCnt = (totalRrd % MAX_NUM_RECORDS == 0) ? (totalRrd / MAX_NUM_RECORDS)
				: (totalRrd / MAX_NUM_RECORDS + 1);
		System.out.println(pktCnt + " packets to send. " + MAX_NUM_RECORDS
				+ " records per packet.");

		try {
			if (cur.moveToFirst()) {
				for (int i = 0; i < pktCnt; i++) {
					// generate the packet
					pkt = getPacketData(uri, cur, MAX_NUM_RECORDS);

					Log.i("Phonehome", "Packet:" + i + " out of "+ pktCnt+" ,Size:" + pkt.length);

					currentTime = System.currentTimeMillis() / 1000;
					// send to server
					sendDataPacket(socket, pkt, checkinTime, version);

					// Changed: Nov 25, 2013
					// It would appear that deleting the data after every packet
					// causes a problem when
					// there is a lot of data. For example 5,000 rows. My
					// thought is that maybe the Cursor
					// only loads a few thousand rows at a time (enough to fit
					// in memory) and then hits the
					// database again. Since we've deleted data in the database,
					// the cursor and database are
					// no longer aligned with the same index and we get a Cursor
					// Window error. However, the data already
					// transmitted will have been deleted, and we should pick up
					// where we left off the next time around

					//					if (uri == DataProvider.DC_URI) {
					//						deleteOldData(uri, maxId_DC, minId_DC);
					//					} else if (uri == DataProvider.PM_URI) {
					//						deleteOldData(uri, maxId_PM, minId_PM);
					//					}


				}
			}

			if (uri == DataProvider.PM_URI) {
				deleteOldData(uri, maxId_PM, minId_PM);
			}

		} catch(Exception e){
			// This excpetion is likley caused by sendPacketData
			// This means we did not correctly send the last packet,
			// Get an updated number of packets we sent, minus 1 for the
			// failed packet.  Delete all correctly transmitted packets
			e.printStackTrace(System.err);
			if (uri == DataProvider.PM_URI) {
				maxId_PM = cur.getInt(DataProvider.PM_ID_COLUMN) - 1;
				deleteOldData(uri, maxId_PM, minId_PM);
			}
		}finally {
			cur.close();
		}

	}

	public void sendDataPacket(Socket socket, byte[] data, long checkinTime,
			float version) throws Exception {
		OutputStream out = null;
		InputStream in = null;
		byte[] response;// response from the server
		byte FAIL = '0';

		out = socket.getOutputStream();
		in = socket.getInputStream();
		out.write(String.valueOf(checkinTime).getBytes());
		// out.write(toByteArray((int)version));
		out.write(data);
		
		//out.write(new String("quit").getBytes());
		
		// get response from the server
//		response = new byte[1];
//		in.read(response, 0, 1);
//		Log.i("Phonehome","sendDataPacket:  size = "+data.length);
//		// return success or failure
//		if (response[0] == FAIL) {
//			throw new Exception("Server failed.");
//		}
	}

	private byte[] getPacketData(Uri uri, Cursor cur, int size) {
		String pkt = "";

		int i;
		Log.i("Phonehome", "getPacketData");
		// get the records from the cursor
		for (i = 0; i < size; i++) {
			pkt += getRecord(uri, cur);
			// maintains the maxId retrieve from database
			if (uri == DataProvider.PM_URI) {
				maxId_PM = cur.getInt(DataProvider.PM_ID_COLUMN);
				if (i == 0)
					minId_PM = maxId_PM;
			}
			if (!cur.moveToNext())
				break;

		}

		// System.out.println(pkt);
		return pkt.getBytes();
	}
	// delete data that have been successful transmitted to the server
	private int deleteOldData(Uri uri, long maxId, long minId) {
		ContentResolver cr;
		String where = null;
		String cName;

		cr = getContentResolver();
		// set the query which is to delete all the records with smaller ID
		if (uri == DataProvider.PM_URI) {
			cName = DataProvider.PM_KEY_ID;
		} else {
			cName = DataProvider.DC_KEY_ID;
		}
		where = cName + "<=?";
		// delete data
		return cr.delete(uri, where, new String[] { Long.toString(maxId) });
	}

	private String getRecord(Uri uri, Cursor cur) {
		String record = null;
		if (uri == DataProvider.PM_URI) {
			int id = cur.getInt(DataProvider.PM_ID_COLUMN); // get id
			long time = cur.getLong(DataProvider.PM_TIME_COLUMN); // get time
			String type = cur.getString(DataProvider.PM_TYPE_COLUMN); // get
			// type
			// of
			// the
			// instrument
			String name = cur.getString(DataProvider.PM_NAME_COLUMN); // get the
			// name
			// of
			// the
			// data
			String address = cur.getString(DataProvider.PM_ADDRESS_COLUMN); // get
			// address
			// of
			// the
			// data
			int strength = cur.getInt(DataProvider.PM_STRENGTH_COLUMN); // get
			// the
			// strength
			// of
			// the
			// instrument
			record = Integer.toString(id) + "|" + Long.toString(time) + "|"
					+ type + "|" + name + "|" + address + "|"
					+ Integer.toString(strength) + "\n";
			Log.i("Phonehome", "getRecord"+record);
		}
		return record;
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.i("Phonehome", "onstart boom");
		// Phone home
		doPhonehome();

		return Service.START_NOT_STICKY;
	}

	void doPhonehome() {
		if (lastLookup == null
				|| lastLookup.getStatus().equals(AsyncTask.Status.FINISHED)) {
			Log.d("PhonehomeService","Creating BgTask");
			lastLookup = new BgTask();
			lastLookup.execute((Void[]) null);
		} else {
			Log.w("PhonehomeService", "Phone Home Failed lastlookup? null" + Boolean.toString(lastLookup == null) + lastLookup.getStatus().toString());
		}

	}
	@Override
	public void onDestroy() {
		Log.i("PhonehomeService","Destroy Service.");
		lastLookup = null;
	}


}
