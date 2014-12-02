package com.example.testproximity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.gimbal.logging.GimbalLogConfig;
import com.gimbal.logging.GimbalLogLevel;
import com.gimbal.proximity.Proximity;
import com.gimbal.proximity.ProximityError;
import com.gimbal.proximity.ProximityFactory;
import com.gimbal.proximity.ProximityListener;
import com.gimbal.proximity.ProximityOptions;
import com.gimbal.proximity.Visit;
import com.gimbal.proximity.VisitListener;
import com.gimbal.proximity.VisitManager;


public class NetscaleProximityService extends Service implements ProximityListener, VisitListener {
	final private static String Debug = "Service Debug";
	private VisitManager visitManager;
	private ProximityOptions options = new ProximityOptions();
	final private static String APP_ID="XXXX";
	final private static String APP_SECRET = "XXXX";
	private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	private boolean mbackground = true;
	final static int MSG_SAY_BACK = 1;
	final static int MSG_SAY_FORE = 2;
	private boolean isRunning = false;
	private Thread bgThread;
	private NotificationManager mNM;
	public static int MAXNUM_RECORD = 1000;
	private ContentValues[] record_buffer = new ContentValues[MAXNUM_RECORD];
	private int buffer_count = 0;

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mbackground = Boolean.valueOf(intent.getStringExtra("background"));
		}
	};
	@Override
	public void serviceStarted()
	{
		// this will be invoked if the service has successfully started
		// bluetooth scanning will be started at this point
		Toast.makeText(getApplicationContext(),"Start Proximity Service" 
				,Toast.LENGTH_LONG).show();
		Log.d("Proximity","Proximity Service successfully started!");
	}

	@Override
	public void startServiceFailed(int errorCode, String message)
	{
		// this will be called if the service has failed to start
		String logMsg = String.format("Proximity Service failed with error code %d, message: %s!", errorCode, message);
		Log.d("Proximity", logMsg);
		//check for the error Code for Bluetooth status check
		if (errorCode == ProximityError.PROXIMITY_BLUETOOTH_IS_OFF.getCode()) {
			Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			turnOn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(turnOn);
			Toast.makeText(getApplicationContext(),"Turned on" 
					,Toast.LENGTH_SHORT).show();
		}
	}

	private void showNotification(String name, String state) {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		int myID = 0;
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.ic_launcher_web)
		.setContentTitle(name)
		.setContentText(state);
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, MainActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		mBuilder.setContentIntent(resultPendingIntent);


		// mId allows you to update the notification later on.
		mBuilder.setWhen(System.currentTimeMillis());
		if(state == "Departure"){
			myID = 1;
			mBuilder.setDefaults( Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS);
		}
		else if (state == "Arrival"){
			myID = 2;
			mBuilder.setDefaults( Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS);
		}
		else if (state == "Update"){
			myID = 3;
			mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
		}
		mNM.notify(myID, mBuilder.build());
	}
	@Override
	public void didArrive(Visit visit) {
		// TODO Auto-generated method stub
		//logshow.append("Arrival of "+visit.getTransmitter().getName() +visit.getLastUpdateTime().toString()+"\n");
		Log.d(Debug,"Arrival happened!!!!!");
		if(!mbackground){
			//
		}
		else{
			//showNotification(visit.getTransmitter().getName(),"Arrival");
		}

	}

	@Override
	public void receivedSighting(Visit v, Date updateTime, Integer Rssi) {
		Log.d(Debug,"Visit Event happened!!!!! Is in background? " + mbackground);
		if(!mbackground){
			final Visit visit = v;
			final int RSSI = Rssi.intValue();
			bgThread = new Thread(new Runnable() {
				@Override
				public void run() {
					Intent b = new Intent("PROXIMITY_UPDATE");

					b.putExtra("Name", visit.getTransmitter().getName());
					b.putExtra("Identifier", visit.getTransmitter().getIdentifier());
					b.putExtra("Rssi",RSSI);
					if(visit.getTransmitter().getBattery() == null|visit.getTransmitter().getTemperature() == null){
						b.putExtra("Battery", 0);
						b.putExtra("Temperature", 0);
					}
					else{
						b.putExtra("Battery", visit.getTransmitter().getBattery().intValue());
						b.putExtra("Temperature", visit.getTransmitter().getTemperature().intValue());
					}

					b.putExtra("Depart",false);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(b);
				}
			});
			bgThread.start();
		}
		else{
			//showNotification(v.getTransmitter().getName()," RSSI: "+Rssi);
		}
		addNewPMData_buffered("Proximity",System.currentTimeMillis(),Rssi.shortValue(),v.getTransmitter().getIdentifier(),"RSSI");


	}

	@Override
	public void didDepart(Visit v) {
		Log.d(Debug,"Departure happened!!!!! Is in background? " + mbackground);
		if(!mbackground){
			final Visit visit = v;
			bgThread = new Thread(new Runnable() {
				@Override
				public void run() {
					Intent b = new Intent("PROXIMITY_DEPART");

					b.putExtra("Name", visit.getTransmitter().getName());       
					b.putExtra("Depart",false);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(b);
				}
			});
			bgThread.start();
		}
		else{
			//showNotification(v.getTransmitter().getName(),"Departure");
		}


	}


    public void addNewPMData_buffered(String type, long time, short strength, String addr, String name) {
        
    	ContentValues values = new ContentValues();

        values.put(DataProvider.PM_KEY_TIME, time);
        values.put(DataProvider.PM_KEY_NAME, name);
        values.put(DataProvider.PM_KEY_ADDRESS, addr);
        values.put(DataProvider.PM_KEY_STRENGTH, strength);
        values.put(DataProvider.PM_KEY_TYPE, type);
        //this.getContentResolver().insert(DataProvider.PM_URI, values);
        record_buffer[buffer_count] = values;
        Log.i("Bulk Insert","The Buffer count is "+buffer_count+"\n");
        buffer_count++;
        if(buffer_count>=MAXNUM_RECORD){
        	this.getContentResolver(). bulkInsert(DataProvider.PM_URI, record_buffer);
        	for(int i=0;i<MAXNUM_RECORD;i++){
        		record_buffer[i]=null;
        		buffer_count = 0;
        	}
        }
    }
    
    
    
	public void inquiryUI(){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				Intent b = new Intent("UI_ON");    
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(b);
			}
		});
		t.start();

	}
	public void ini_proximity(){
		Proximity.initialize(getApplicationContext(),
				APP_ID,
				APP_SECRET);
		GimbalLogConfig.setLogLevel(GimbalLogLevel.DEBUG);
		Proximity.optimizeWithApplicationLifecycle(getApplication());
	}
	public void startProximityService() {


		//ini_proximity();
		visitManager = ProximityFactory.getInstance().createVisitManager();
		visitManager.setVisitListener(this);

		options.setOption(ProximityOptions.VisitOptionSignalStrengthWindowKey,
				ProximityOptions.VisitOptionSignalStrengthWindowNone);
		options.setOption(ProximityOptions.VisitOptionForegroundDepartureIntervalInSecondsKey,
				5);
		options.setOption(ProximityOptions.VisitOptionBackgroundDepartureIntervalInSecondsKey,
				5);
		options.setOption(ProximityOptions.VisitOptionArrivalRSSIKey,
				-60);
		options.setOption(ProximityOptions.VisitOptionDepartureRSSIKey,
				-90);

		Proximity.startService((ProximityListener) this);
		visitManager.start();
		visitManager.startWithOptions(options);

	}
	public void stopProximityService() {
		Proximity.stopService();
		visitManager.stop();
	}
	public void SetAlarm(Context context)
	{
		//context=ctx;
		AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context,PhoneHomeService.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
		//Log.d("Alarm","Set Alarm Good.");
		//Toast.makeText(context, "Alarm Set OK!", Toast.LENGTH_LONG).show(); 
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000*60*60*24, pi); // Millisec * Second * Minute
	}

	public void CancelAlarm(Context context)
	{
		Intent intent = new Intent(context,PhoneHomeService.class);
		PendingIntent sender = PendingIntent.getService(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
	@Override
	public void onCreate() {
		// The service is being created
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		ini_proximity();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver, new IntentFilter("Background_Update"));
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The service is starting, due to a call to startService()
		if(!isRunning){
			startProximityService();
			SetAlarm(getApplicationContext());
			inquiryUI();
			isRunning = true;
		}
		return 1;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub

		//startProximityService();
		//mbound = true;
		return null;
	}
	@Override
	public void onDestroy() {
		// The service is no longer used and is being destroyed
		stopProximityService();
		isRunning = false;
		mBtAdapter.cancelDiscovery();
		CancelAlarm(getApplicationContext());
	}

}
