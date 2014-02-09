package com.faruba;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class FAruba extends Activity {
	private static final String TAG = FAruba.class.getSimpleName();
	private static final String BASE_URL = "https://securelogin.arubanetworks.com/cgi-bin/login";
	private static final String PREF_USERNAME = "com.example.faruba.preferences.username";
	private static final String PREF_PASSWORD = "com.example.faruba.preferences.password";
	private static final String SSID = "cooper";

	private EditText mUsername, mPassword;
	private TextView mServerResp, mConnectionInfo, mConnectionInfoTitle;
	private Button mUpdate;

	private NetworkChangeReceiver mReceiver;

	@SuppressWarnings("static-access")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mUsername = (EditText) findViewById(R.id.username);
		mPassword = (EditText) findViewById(R.id.password);
		mConnectionInfo = (TextView) findViewById(R.id.info);
		mServerResp = (TextView) findViewById(R.id.response);
		mConnectionInfoTitle = (TextView) findViewById(R.id.info_title);
		mUpdate = (Button) findViewById(R.id.update);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		mUsername.setText(sharedPrefs.getString(PREF_USERNAME, ""));
		mUsername.setImeActionLabel(getResources().getString(R.string.save), KeyEvent.KEYCODE_ENTER);
		mUsername.setOnKeyListener(new SaveTextListener(this, PREF_USERNAME));

		mPassword.setText(sharedPrefs.getString(PREF_PASSWORD, ""));
		mPassword.setImeActionLabel(getResources().getString(R.string.save), KeyEvent.KEYCODE_ENTER);
		mPassword.setOnKeyListener(new SaveTextListener(this, PREF_PASSWORD));

		mUpdate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				SharedPreferences sharedPrefs = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());

				sharedPrefs.edit().putString(PREF_USERNAME, mUsername.getText().toString()).commit();
				sharedPrefs.edit().putString(PREF_PASSWORD, mPassword.getText().toString()).commit();

                updateInfo();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		mReceiver = new NetworkChangeReceiver();

		// To detect change in network (triggers PokeArubaTask)
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mReceiver, intentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mReceiver);
	}

	private void updateInfo() {
		// WIFI STUFF
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		Log.d("wifiInfo", wifiInfo.toString());
		Log.d("SSID", wifiInfo.getSSID());

		String ssid = wifiInfo.getSSID();
		String supState = wifiInfo.getSupplicantState().toString();

		NetworkInfo.DetailedState detailedSupState = wifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());

		String macAddr = wifiInfo.getMacAddress();

		mConnectionInfoTitle.setVisibility(View.VISIBLE);

		mConnectionInfo.setText(ssid + '\n'
				+ supState + '\n'
				+ detailedSupState.toString() + '\n'
				+ macAddr + '\n'
		);

		// POST auth request to aruba
		new PokeArubaTask().execute();
	}

	/**
	 * Trust any certificate because they have not been updated.
	 */
	private class OpenSSLSocketFactory extends SSLSocketFactory {

		SSLContext sslContext = SSLContext.getInstance("TLS");

		public OpenSSLSocketFactory(KeyStore trustStore) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
			super(trustStore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[]{tm}, null);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}
	}

	private String postAuth() throws IOException {

		KeyStore trustStore = null;
		try {
			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

			trustStore.load(null, null);
		} catch (KeyStoreException e) {
			Log.e(TAG, "Error loading key store.", e);

			return null;
		} catch (CertificateException e) {
			Log.e(TAG, "Error loading key store.", e);

			return null;
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Error loading key store.", e);

			return null;
		}

		SSLSocketFactory sf = null;
		try {
			sf = new OpenSSLSocketFactory(trustStore);
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Error creating socket factory.", e);

			return null;
		} catch (KeyManagementException e) {
			Log.e(TAG, "Error creating socket factory.", e);

			return null;
		} catch (KeyStoreException e) {
			Log.e(TAG, "Error creating socket factory.", e);

			return null;
		} catch (UnrecoverableKeyException e) {
			Log.e(TAG, "Error creating socket factory.", e);

			return null;
		}
		sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
		HttpProtocolParams.setUseExpectContinue(params, true);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schReg.register(new Scheme("https", sf, 443));
		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

		HttpClient httpclient = new DefaultHttpClient(conMgr, params);
		HttpPost httppost = new HttpPost(BASE_URL);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Add auth values
		List<NameValuePair> valPairs = new ArrayList<NameValuePair>(4);
		valPairs.add(new BasicNameValuePair("cmd", "login"));
		valPairs.add(new BasicNameValuePair("user", sharedPrefs.getString(PREF_USERNAME, "")));
		valPairs.add(new BasicNameValuePair("password", sharedPrefs.getString(PREF_PASSWORD, "")));
		valPairs.add(new BasicNameValuePair("url", "reddit.com"));

		httppost.setEntity(new UrlEncodedFormEntity(valPairs));

		// Execute Post Request
		HttpResponse response = httpclient.execute(httppost);

		return response.toString();
	}

	private class SaveTextListener implements View.OnKeyListener {

		private Context mContext;
		private String mPreference;

		private SaveTextListener(Context context, String preference) {
			mContext = context;
			mPreference = preference;
		}

		@Override
		public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
			if (!(view instanceof TextView)) {
				return false;
			}

			TextView textView = (TextView) view;

			// If the event is a key-down event on the "enter" button
			if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
					(keyCode == KeyEvent.KEYCODE_ENTER)) {

				SharedPreferences sharedPrefs = PreferenceManager
						.getDefaultSharedPreferences(mContext);

				sharedPrefs.edit().putString(mPreference, textView.getText().toString()).commit();

				return true;
			}
			return false;
		}
	}

	public class NetworkChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {

				boolean connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);

				Log.w(TAG, "supplicant connection: " + connected);

//                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
//                    //do stuff
//                    new PokeArubaTask().execute();
//                    Toast toast = Toast.makeText(getApplicationContext(),
//                            "Poking Aruba", 1);
//                    toast.show();
//                } else {
//                    // wifi connection was lost
//                }
			} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

				switch (state) {
					case WifiManager.WIFI_STATE_ENABLED:
						Log.w(TAG, "wifi enabled");
						break;
					case WifiManager.WIFI_STATE_ENABLING:
						Log.w(TAG, "wifi enabling");
						break;
				}
			} else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
				SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
				Log.w(TAG, "supplicant: " + state.name());
			} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

				if (info.getType() == ConnectivityManager.TYPE_WIFI) {
					Log.w(TAG, "info: " + info.isConnectedOrConnecting() + " , " + info.isConnected());
					NetworkInfo.DetailedState state = info.getDetailedState();

					switch (state) {
						case BLOCKED:
							Log.w(TAG, "blocked");
							break;
						case CAPTIVE_PORTAL_CHECK:
							Log.w(TAG, "captive portal check");
							break;
						case CONNECTING:
							Log.w(TAG, "connecting");
							break;
						case CONNECTED:
							WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

							Log.w(TAG, "connected " + wifiManager.getConnectionInfo().getSSID());

							if (wifiManager.getConnectionInfo().getSSID().contains(SSID)) {
								updateInfo();
							}

							break;
						case OBTAINING_IPADDR:
							Log.w(TAG, "obtaining ip address");
							break;
					}


				}
			}
		}
	}

	private class PokeArubaTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {

			try {
				return postAuth();
			} catch (ClientProtocolException e) {
				Log.w(TAG, "Already logged in.", e);
				return getResources().getString(R.string.logged_in);
			} catch (IOException e) {
				Log.w(TAG, "Error poking Aruba.", e);
				return e.getMessage();
			}
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			mServerResp.setText(result);
		}
	}
}
