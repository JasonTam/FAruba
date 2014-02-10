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

	public static final String BASE_URL = "https://securelogin.arubanetworks.com/cgi-bin/login";
	public static final String PREF_USERNAME = "com.example.faruba.preferences.username";
	public static final String PREF_PASSWORD = "com.example.faruba.preferences.password";
	public static final String SSID = "cooper";

	private EditText mUsername, mPassword;
	private Button mUpdate;

	@SuppressWarnings("static-access")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mUsername = (EditText) findViewById(R.id.username);
		mPassword = (EditText) findViewById(R.id.password);
		mUpdate = (Button) findViewById(R.id.update);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

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

				Intent i = new Intent(getApplicationContext(), FArubaService.class);
				getApplicationContext().startService(i);
			}
		});
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
}
