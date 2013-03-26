package io.kate.coatrack;

import io.kate.coatrack.CoatRackView.EmitterView;
import io.kate.coatrackcontrol.R;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	public static final String intent = "io.kate.coatrack.update";
	public static final String LOG_TAG = MainActivity.class.getName();
	public static final int num_effects_in_ring = 8;
	Button button;

	String ssid = "CoatRack";
	String wirelessPassword = "dootdoot";

	public String ipString;
	public int port = 2000;

	static final int hack = 48;

	int timeout = 100; // flame effects turn off after 100 milliseconds
	int refreshTimeout = 50;
	
	WifiManager wifiManager;

	CoatRackView ringView;

	SharedPreferences prefs;

	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(intent))
				return;
			for (EmitterView emitter : ringView.getEmitters()) {
				if (System.currentTimeMillis() - emitter.lastActivated > timeout) {
					if (emitter.touching) {
						triggerEffect(emitter.id);
					} else {
						emitter.intensity = 0;
					}
					ringView.postInvalidate();
				}
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.e(LOG_TAG, "started");

		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		ipString = prefs.getString(CoatRackApplication.PREF_SERVER_ADDRESS,
				CoatRackApplication.DEFAULT_IP);
		port = prefs.getInt(CoatRackApplication.PREF_SERVER_PORT,
				CoatRackApplication.DEFAULT_PORT);

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		ringView = (CoatRackView) findViewById(R.id.ring_view);
		ringView.postInvalidate();

		ringView.onEmitterTouch = ringView.new OnEmitterTouch() {
			@Override
			public void onEmitterTouch(EmitterView[] emitters, int id) {
				triggerEffect(id);
			}
		};

		button = (Button) findViewById(R.id.eruption);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Fire all the flame effects
				triggerEffects(new int[] { 0, 1, 2, 3, 4, 5, 6, 7 });
			}
		});

		startTimeout();
	}

	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(receiver, new IntentFilter(intent));
		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		createWifiAccessPoint();
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
		wifiManager.setWifiEnabled(false);
	}

	public void startTimeout() {
		PendingIntent pintent = PendingIntent.getBroadcast(this, 0, new Intent(
				intent), 0);
		AlarmManager manager = (AlarmManager) (this
				.getSystemService(Context.ALARM_SERVICE));

		manager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + refreshTimeout, refreshTimeout,
				pintent);
	}

	private class ConnectTask extends AsyncTask<byte[], Void, Void> {
		protected Void doInBackground(byte[]... arg0) {
			send(arg0[0]);
			return null;
		}
	}

	private void triggerEffects(int[] ids) {
		EmitterView[] emitters = ringView.getEmitters();
		byte[] message = new byte[ids.length];
		for (int i = 0; i < ids.length; i++) {
			emitters[ids[i]].lastActivated = System.currentTimeMillis();
			emitters[ids[i]].intensity = 1;
			message[i] = (byte) (ids[i] + hack);
		}
		(new ConnectTask()).execute(message);
		ringView.postInvalidate();
	}

	private void triggerEffect(int id) {
		EmitterView[] emitters = ringView.getEmitters();
		emitters[id].lastActivated = System.currentTimeMillis();
		emitters[id].intensity = 1;
		(new ConnectTask()).execute(new byte[] { (byte) (id + hack) });
		ringView.postInvalidate();
	}

	public void send(byte[] msg) {
		try {
			InetAddress ip = InetAddress.getByName(ipString);
			DatagramSocket s = new DatagramSocket();
			DatagramPacket p = new DatagramPacket(msg, msg.length, ip, port);
			s.send(p);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.connect:
			DialogFragment newFragment = new ServerDialogFragment();
			newFragment.show(getFragmentManager(), "dialog");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void createWifiAccessPoint() {
		
		if (wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(false);
		}
		
		// Get all declared methods in WifiManager class
		Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
		
		boolean methodFound = false;
		for (Method method : wmMethods) {
			if (method.getName().equals("setWifiApEnabled")) {
				methodFound = true;
				WifiConfiguration netConfig = new WifiConfiguration();
				netConfig.SSID = ssid;
//				netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
				netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
				netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				netConfig.preSharedKey= wirelessPassword;
				try {
					boolean apstatus = (Boolean) method.invoke(wifiManager, netConfig, true);
					Log.e(LOG_TAG, String.format("Creating a Wi-Fi Network %s", netConfig.SSID));
					for (Method isWifiApEnabledmethod : wmMethods) {
						if (isWifiApEnabledmethod.getName().equals("isWifiApEnabled")) {
							while (!(Boolean) isWifiApEnabledmethod.invoke(wifiManager)) {
							};
							for (Method method1 : wmMethods) {
								if (method1.getName().equals("getWifiApState")) {
									int apstate;
									apstate = (Integer) method1.invoke(wifiManager);
									// netConfig=(WifiConfiguration)method1.invoke(wifi);
									Log.e(LOG_TAG, String.format("SSID: %s, Password: %s", netConfig.SSID, netConfig.preSharedKey));
								}
							}
						}
					}
					if (apstatus) {
						Log.e(LOG_TAG, "Access Point Created!");
					} else {
						Log.e(LOG_TAG, "Access Point Creation failed!");
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		if (!methodFound) {
			Log.e(LOG_TAG, "Your phone's API does not contain setWifiApEnabled method to configure an access point");
		}
	}
}
