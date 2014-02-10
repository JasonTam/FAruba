package com.faruba;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
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

/**
 * Created by Eric on 2/9/14.
 */
public class FArubaService extends IntentService {

	private static final String TAG = FArubaService.class.getSimpleName();

	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public FArubaService() {
		super("FArubaService");
	}

	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns, IntentService
	 * stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			postAuth(getApplicationContext());

			Toast.makeText(getApplicationContext(), "Logging in.", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			Log.e(TAG, "Error handling", e);
		}
	}

	private String postAuth(Context context) throws IOException {

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
		HttpPost httppost = new HttpPost(FAruba.BASE_URL);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		// Add auth values
		List<NameValuePair> valPairs = new ArrayList<NameValuePair>(4);
		valPairs.add(new BasicNameValuePair("cmd", "login"));
		valPairs.add(new BasicNameValuePair("user", sharedPrefs.getString(FAruba.PREF_USERNAME, "")));
		valPairs.add(new BasicNameValuePair("password", sharedPrefs.getString(FAruba.PREF_PASSWORD, "")));
		valPairs.add(new BasicNameValuePair("url", "reddit.com"));

		httppost.setEntity(new UrlEncodedFormEntity(valPairs));

		// Execute Post Request
		HttpResponse response = httpclient.execute(httppost);

		return response.toString();
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
}
