package com.example.faruba;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	@SuppressWarnings("static-access")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
/*
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		setContentView(tv);
		
		// WIFI STUFF
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		Log.d("wifiInfo", wifiInfo.toString());
		Log.d("SSID", wifiInfo.getSSID());

		String ssid = wifiInfo.getSSID();
		String supState = wifiInfo.getSupplicantState().toString();
		
		DetailedState detailedSupState = wifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());

		boolean obIpAdd = detailedSupState.toString().equals("OBTAINING_IPADDR");
		
		String macAddr = wifiInfo.getMacAddress();
		
		tv.setText( ssid + '\n'
				+ supState + '\n'
				+ detailedSupState.toString() + '\n'
				+ obIpAdd + '\n'
				+ macAddr + '\n'
				);

		new PokeArubaTask().execute();
//		
//		ApplicableNetworkReceiver rec = new ApplicableNetworkReceiver();
//		
//		IntentFilter intentFilter = new IntentFilter();
//		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
//		registerReceiver(rec, intentFilter);
		
		*/
		
		showUserSettings();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public class ApplicableNetworkReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        final String action = intent.getAction();
	        if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
	            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
	                //do stuff
	        		new PokeArubaTask().execute();
	            	Toast toast = Toast.makeText(getApplicationContext(),
	            			"Poking Aruba", 1);
	    			toast.show();
	            } else {
	                // wifi connection was lost
	            }
	        }
	    }
	}
	
	    
	private String postAuth() throws IOException {

		String urlStr = "https://securelogin.arubanetworks.com/cgi-bin/login";

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(urlStr);

		// Add auth values
		List<NameValuePair> valPairs = new ArrayList<NameValuePair>(4);
		valPairs.add(new BasicNameValuePair("cmd", "login"));
		valPairs.add(new BasicNameValuePair("user", "myuser"));
		valPairs.add(new BasicNameValuePair("password", "mypassword"));
		valPairs.add(new BasicNameValuePair("url", "www.google.com"));
		
//		valPairs.add(new BasicNameValuePair("mac", "78:d6:f0:81:aa:c0"));
//		valPairs.add(new BasicNameValuePair("ip", "10.18.244.246"));
//		valPairs.add(new BasicNameValuePair("essid", "cooper-g"));
		
		httppost.setEntity(new UrlEncodedFormEntity(valPairs));

		// Execute Post Request
		 HttpResponse response = httpclient.execute(httppost);

//		return httppost.getURI().toString();
		return response.toString();
	}

	private class PokeArubaTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {

			try {
				return postAuth();
			} catch (IOException e) {
				return "Aruba is messed up: " + e;
			}
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			Context context = getApplicationContext();
			CharSequence text = result;
			int duration = Toast.LENGTH_LONG;

//			Toast toast = Toast.makeText(context, text, duration);
//			toast.show();
			
			AlertDialog alertDialog1 = new AlertDialog.Builder(
					MainActivity.this).create();
            alertDialog1.setMessage(text);
            alertDialog1.show();
			
		}
	}
	
	
    /** Called when the user clicks the Send button */
    public void gotoSettings(View view) {
        // Do something in response to button
    	Intent intent = new Intent(this, Preferences.class);
//    	EditText editText = (EditText) findViewById(R.id.edit_message);
//    	String message = editText.getText().toString();
//    	intent.putExtra(EXTRA_MESSAGE, message);
    	startActivityForResult(intent, RESULT_SETTINGS);
    	startActivity(intent);
    }
	
    
    private static final int RESULT_SETTINGS = 1;
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
//        	gotoSettings(this.getCurrentFocus());
//            return true;
        	Intent intent = new Intent(this, Preferences.class);
            startActivityForResult(intent, RESULT_SETTINGS);
//            break;
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
 
        switch (requestCode) {
        case RESULT_SETTINGS:
            showUserSettings();
            break;
 
        }
 
    }
    
    private void showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        
        
        StringBuilder builder = new StringBuilder();
 
        builder.append("\n Username: "
                + sharedPrefs.getString("prefUsername", "NULL"));
 
        builder.append("\n Password: "
                + sharedPrefs.getString("prefPassword", "NULL"));
        
        builder.append("\n Send report:"
                + sharedPrefs.getBoolean("prefSendReport", false));
 
        builder.append("\n Sync Frequency: "
                + sharedPrefs.getString("prefSyncFrequency", "NULL"));
 
        TextView settingsTextView = (TextView) findViewById(R.id.textUserSettings);
 
        settingsTextView.setText(builder.toString());
    }
}


