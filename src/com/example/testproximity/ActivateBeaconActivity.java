package com.example.testproximity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.InputStream;

public class ActivateBeaconActivity extends Activity {
	private static String TAG = "ActivateBeacon";
	private EditText editText;
	private TextView result_text;
	
	public void viewBeacons(View v){
		AsyncHttpClient client = new AsyncHttpClient();
		client.addHeader("Content-type", "application/json");
		client.addHeader("AUTHORIZATION", "Token token=4f52915781dd4f300fc74f306f25d2ee");
		client.get("https://manager.gimbal.com/api/beacons" ,new AsyncHttpResponseHandler() {
			// When the response returned by REST has Http response code '200'
			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] response) {
				// Hide Progress Dialog

				try {
					String JSONString = new String(response, "UTF-8");

					// JSON Object
					JSONArray objs = new JSONArray(JSONString);
					for(int i = 0; i < objs.length();i++){
						Log.d(TAG,JSONString+ "JSONObject is null? "+objs.getJSONObject(i));
						result_text.append(objs.getJSONObject(i).toString());
					}

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
					e.printStackTrace();

				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// When the response returned by REST has Http response code other than '200'
			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

				// When Http response code is '404'
				if(statusCode == 404){
					Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				} 
				// When Http response code is '500'
				else if(statusCode == 500){
					Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
				} 
				// When Http response code other than 404, 500
				else if(statusCode == 401){
					Toast.makeText(getApplicationContext(), "Unauthorized", Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]", Toast.LENGTH_LONG).show();
				}
			}
		});

	}
	public void Activate(View v){
		AsyncHttpClient client = new AsyncHttpClient();
		client.addHeader("Content-type", "application/json");
		client.addHeader("AUTHORIZATION", "Token token=4f52915781dd4f300fc74f306f25d2ee");
		RequestParams params = new RequestParams();
		params.put("factory_id", "2J9X-Q98SR");
		params.put("name", "Netscale 2");
		params.put("latitude","");
		params.put("longitude","");
		params.put("config_id","");
		params.put("visibility","private");
		client.post("https://manager.gimbal.com/api/beacons", params,new AsyncHttpResponseHandler() {
			// When the response returned by REST has Http response code '200'
			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] response) {
				// Hide Progress Dialog

				try {
					String JSONString = new String(response, "UTF-8");

					// JSON Object
					JSONObject obj = new JSONObject(JSONString);
					Log.d(TAG,JSONString+ "JSONObject is null? "+obj.get("name").toString());
					result_text.setText(obj.toString());

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
					e.printStackTrace();

				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// When the response returned by REST has Http response code other than '200'
			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

				// When Http response code is '404'
				if(statusCode == 404){
					Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				} 
				// When Http response code is '500'
				else if(statusCode == 500){
					Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
				} 
				// When Http response code other than 404, 500
				else if(statusCode == 401){
					Toast.makeText(getApplicationContext(), "Unauthorized", Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]", Toast.LENGTH_LONG).show();
				}
			}
		});

	}
	public void DeActivate(View v){
		AsyncHttpClient client = new AsyncHttpClient();
		client.addHeader("Content-type", "application/json");
		client.addHeader("AUTHORIZATION", "Token token=4f52915781dd4f300fc74f306f25d2ee");
		client.delete("https://manager.gimbal.com/api/beacons/2J9X-Q98SR",new AsyncHttpResponseHandler() {
			// When the response returned by REST has Http response code '200'
			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] response) {
				// Hide Progress Dialog

				try {
					String JSONString = new String(response, "UTF-8");

					// JSON Object
					JSONObject obj = new JSONObject(JSONString);
					Log.d(TAG,JSONString+ "JSONObject is null? "+obj.get("name").toString());
					result_text.setText(obj.toString()+"\n");

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
					e.printStackTrace();

				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// When the response returned by REST has Http response code other than '200'
			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {

				// When Http response code is '404'
				if(statusCode == 404){
					Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
				} 
				// When Http response code is '500'
				else if(statusCode == 500){
					Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
				} 
				// When Http response code other than 404, 500
				else if(statusCode == 401){
					Toast.makeText(getApplicationContext(), "Unauthorized", Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]", Toast.LENGTH_LONG).show();
				}
			}
		});

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activate_beacon);
		editText = (EditText) findViewById(R.id.editText1);
		result_text = (TextView) findViewById(R.id.textView1);
		//		editText.addTextChangedListener(new TextWatcher(){
		//			@Override
		//			public void afterTextChanged(Editable text) {     
		//
		//
		//			    if (text.length() == 3) {
		//			        text.append('-');
		//			    }
		//			}
		//
		//			@Override
		//			public void beforeTextChanged(CharSequence s, int start, int count,
		//					int after) {
		//				// TODO Auto-generated method stub
		//				
		//			}
		//
		//			@Override
		//			public void onTextChanged(CharSequence s, int start, int before,
		//					int count) {
		//				// TODO Auto-generated method stub
		//				
		//			}
		//		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activate_beacon, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
