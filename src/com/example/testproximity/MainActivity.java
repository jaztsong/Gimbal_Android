package com.example.testproximity;

import java.util.LinkedHashMap;

import com.example.testproximity.TransmitterAttributes;
import com.example.testproximity.TransmitterListAdapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;

public class MainActivity extends Activity{
	final private static String Debug = "activity Debug";

    private Switch enableProximitySwitch;
    private static final String PROXIMITY_SERVICE_ENABLED_KEY = "proximity.service.enabled";
    private final LinkedHashMap<String, TransmitterAttributes> transmitters = new LinkedHashMap<String, TransmitterAttributes>();
    private TransmitterListAdapter adapter;
    static final int MSG_SAY_UPDATE = 1;
    static final int MSG_SAY_DEPART = 2;
    private ListView myList;
    final static String VERSION = "version";
    

    private BroadcastReceiver mUIMessageReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent i) {
    		setBackground("false");
    	}
    };
    
    private BroadcastReceiver mUpdateMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
        	final Intent intent = i;
        	Thread background = new Thread(new Runnable() {
				@Override
				public void run() {
			        String name = intent.getStringExtra("Name");
			        TransmitterAttributes attributes = new TransmitterAttributes();
			        attributes.setBattery(intent.getIntExtra("Battery", 0));
			        attributes.setIdentifier(intent.getStringExtra("Identifier"));
			        attributes.setName(name);
			        attributes.setTemperature(intent.getIntExtra("Temperature", 0));
			        attributes.setRssi(intent.getIntExtra("Rssi", 0));
			        attributes.setDepart(false);
			        transmitters.put(name, attributes);
			        Log.d(Debug,"Received Msg-update!!!");
			        addDevice(transmitters);
		            //  ... react to local broadcast message
				}
			});
			background.start();
        }
    };
    
    private BroadcastReceiver mDepartMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {      
        	final Intent intent = i;
        	Thread background = new Thread(new Runnable() {
				@Override
				public void run() {
					String name = intent.getStringExtra("Name");
			        TransmitterAttributes attributes = new TransmitterAttributes();
			        attributes.setName(name);
			        attributes.setDepart(true);
			        transmitters.put(name, attributes);
			        Log.d(Debug,"Received Msg-Depart!!!");
			        addDevice(transmitters);
		            //  ... react to local broadcast message
				}
			});
			background.start();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Debug,"onCreate invoked!!!!!");
        setContentView(R.layout.activity_main);
        enableProximitySwitch = (Switch) findViewById(R.id.enableProximitySwitch);
        myList=(ListView)findViewById(android.R.id.list);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mUIMessageReceiver, new IntentFilter("UI_ON"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mUpdateMessageReceiver, new IntentFilter("PROXIMITY_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDepartMessageReceiver, new IntentFilter("PROXIMITY_DEPART"));
        saveUserPreferrence(VERSION, "1.1");
        String proximityServiceEnabled = getUserPreference(PROXIMITY_SERVICE_ENABLED_KEY);
        if (proximityServiceEnabled != null ) {
        	enableProximitySwitch.setChecked(Boolean.valueOf(proximityServiceEnabled));
//        	if(Boolean.valueOf(proximityServiceEnabled)){
//        		Intent i = new Intent(getApplicationContext(), NetscaleProximityService.class);
//        		startService(i);
//        		setBackground("false");
//        	}
        }else{
        	saveUserPreferrence(PROXIMITY_SERVICE_ENABLED_KEY, String.valueOf(false));
        	enableProximitySwitch.setChecked(false);
        }

        
  
        enableProximitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    saveUserPreferrence(PROXIMITY_SERVICE_ENABLED_KEY, String.valueOf(true));
            		Intent i = new Intent(getApplicationContext(), NetscaleProximityService.class);
            		startService(i);
                }
                else {
                	saveUserPreferrence(PROXIMITY_SERVICE_ENABLED_KEY, String.valueOf(false));
                	Intent i = new Intent(getApplicationContext(), NetscaleProximityService.class);
                	stopService(i);
                }
            }

        });
            
    }
    public void goActivate(View v){
    	Intent intent = new Intent(this, ActivateBeaconActivity.class);
    	startActivity(intent);
    }


    private void saveUserPreferrence(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private String getUserPreference(String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Debug,"onResume invoked!!!!!");
        setBackground("false");

        adapter = new TransmitterListAdapter(this, this);
        myList.setAdapter(adapter);
        ImageButton imageButtonRefresh = (ImageButton) findViewById(R.id.imageButton_refresh);

        imageButtonRefresh.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				adapter.removeTransmitters();
            	adapter.notifyDataSetChanged();
			}
        
        });
    }

    private void setBackground(String s) {
    	final String string = s;
    	Thread t = new Thread(new Runnable(){
    		@Override
    		public void run() {
    			Intent b = new Intent("Background_Update");       
    			b.putExtra("background", string);       
    			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(b);
    		}
    	});
    	t.start();
    }


	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d(Debug,"onDestroy invoked!!!!!");
    	setBackground("true");
    }
    @Override
    protected void onPause() {
    	super.onPause();
    	Log.d(Debug,"onPause invoked!!!!!");
    	//setBackground("true");

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    protected synchronized void addDevice(final LinkedHashMap<String, TransmitterAttributes> entries) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	adapter.addTransmitters(entries);
            	adapter.notifyDataSetChanged();
            }
        });
    }
    
}

