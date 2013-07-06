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
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;
import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;
import com.neurosky.thinkgear.TGRawMulti;

public class MainActivity extends Activity {
	public static final String intent = "io.kate.coatrack.update";
	public static final String LOG_TAG = MainActivity.class.getName();
	public static final int num_effects_in_ring = 8;
	Button buttonEruption;
	Button buttonEruption2;
	Button buttonPinwheel;
	Button buttonRandom;

	String ssid = "CoatRack2";
	String wirelessPassword = "dootdoot";

	public String ipString;
	public int port = 2000;

	static final int hack = 48;

	int timeout = 100; // flame effects turn off after 100 milliseconds
	int refreshTimeout = 50;

	WifiManager wifiManager;

	CoatRackView ringView;

	SharedPreferences prefs;

	PendingIntent pintent;
	
	TGDevice tgDevice;
	BluetoothAdapter btAdapter;
	
	private GraphView graphView;
	private GraphViewSeries attentionSeries;
	private GraphViewSeries meditationSeries;
	
	private double attentionSeriesLastX = 0d;
	private double meditationSeriesLastX = 0d;
	
	private TextView signal;
	private TextView attention;
	private TextView meditation;
	
	private int fireOnMeditationOf = -1;
	private int fireOnAttentionOf = -1;
	
	private boolean meditationShouldFire = false;
	private boolean attentionShouldFire = false;
	
	private long step;

	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ringView == null || ringView.getEmitters() == null)
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
			if (buttonEruption2.isPressed() || buttonEruption.isPressed() || meditationShouldFire || attentionShouldFire) {
				triggerEffects(new int[] { 0, 1, 2, 3, 4, 5, 6, 7 });
			}
			
			if (buttonRandom.isPressed()) {
				doRandom();
			}
			
			if (buttonPinwheel.isPressed()) {
				doPinwheel();
			}
			
			step++;
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

		buttonEruption = (Button) findViewById(R.id.button_eruption);
		buttonEruption2 = (Button) findViewById(R.id.button_eruption2);
		buttonPinwheel = (Button) findViewById(R.id.button_pinwheel);
		buttonRandom = (Button) findViewById(R.id.button_random);
		
		graphView = new LineGraphView(this, "");
		graphView.setViewPort(0, 30);
		graphView.setScrollable(true);
		graphView.setManualYAxisBounds(100, 0);
		
		attentionSeries = new GraphViewSeries("attention", new GraphViewSeriesStyle(getResources().getColor(R.color.attention), 3), new GraphViewData[] {new GraphViewData(0, 50)});
		meditationSeries = new GraphViewSeries("meditation", new GraphViewSeriesStyle(getResources().getColor(R.color.meditation), 3), new GraphViewData[] {new GraphViewData(0, 50)});
		
		graphView.addSeries(attentionSeries);
		graphView.addSeries(meditationSeries);

		LinearLayout layout = (LinearLayout) findViewById(R.id.viewer1);
		layout.addView(graphView);
		
		signal = (TextView) findViewById(R.id.signal);
		attention = (TextView) findViewById(R.id.attention);
		meditation = (TextView) findViewById(R.id.meditation);
		
	  final ActionBar bar = getActionBar();
	  bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	  bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
	
	  bar.addTab(bar.newTab()
	          .setText(R.string.register_player)
	          .setTabListener(new TabListener(arenaDisplayFragment, "Register Player")));
	
	  if (savedInstanceState != null) {
	      bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
	  }
	}

	@Override
	public void onResume() {
		super.onResume();

		startTimeout();
		
		registerReceiver(receiver, new IntentFilter(intent));
		wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		createWifiAccessPoint();
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter != null) {
			tgDevice = new TGDevice(btAdapter, handler);
			tgDevice.connect(false);
		}
		
		if (prefs.getBoolean(CoatRackApplication.PREF_ATTENTION_ENABLED, false))
		fireOnAttentionOf = prefs.getInt(CoatRackApplication.PREF_ATTENTION_CUTOFF, -1);
		if (prefs.getBoolean(CoatRackApplication.PREF_MEDITATION_ENABLED, false))
		fireOnMeditationOf = prefs.getInt(CoatRackApplication.PREF_MEDITATION_CUTOFF, -1);
		
		step = 0;
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
		wifiManager.setWifiEnabled(false);
		
		if (tgDevice != null) {
			tgDevice.close();
		}
		
		AlarmManager manager = (AlarmManager) (this
				.getSystemService(Context.ALARM_SERVICE));
		manager.cancel(pintent);
	}

	public void startTimeout() {
		pintent = PendingIntent.getBroadcast(this, 0, new Intent(intent), 0);
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
	
	// This is dumb and probably means I can't java
	private void triggerEffects(Integer[] ids) {
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
		DialogFragment newFragment;
		switch (item.getItemId()) {
		case R.id.connect:
			newFragment = new ServerDialogFragment();
			newFragment.show(getFragmentManager(), "dialog");
			return true;
		case R.id.eeg_settings:
			newFragment = new EEGActivationFragment();
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
				// netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
				netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
				netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				netConfig.preSharedKey = wirelessPassword;
				try {
					boolean apstatus = (Boolean) method.invoke(wifiManager, netConfig,
							true);
					Log.e(LOG_TAG,
							String.format("Creating a Wi-Fi Network %s", netConfig.SSID));
					for (Method isWifiApEnabledmethod : wmMethods) {
						if (isWifiApEnabledmethod.getName().equals("isWifiApEnabled")) {
							while (!(Boolean) isWifiApEnabledmethod.invoke(wifiManager)) {
							}
							;
							for (Method method1 : wmMethods) {
								if (method1.getName().equals("getWifiApState")) {
									int apstate;
									apstate = (Integer) method1.invoke(wifiManager);
									// netConfig=(WifiConfiguration)method1.invoke(wifi);
									Log.e(LOG_TAG, String.format("SSID: %s, Password: %s",
											netConfig.SSID, netConfig.preSharedKey));
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
			Log.e(
					LOG_TAG,
					"Your phone's API does not contain setWifiApEnabled method to configure an access point");
		}
	}
	
	private void doRandom() {
		if (step % 2 == 0) return;
		
		List<Integer> effectsToFire = new ArrayList<Integer>(8);
		for (int i = 0; i < num_effects_in_ring; i++) {
			if (Math.random() > .5) {
				effectsToFire.add(i);
			}
		}
		
		Integer[] fireEffects = new Integer[0];
		fireEffects = effectsToFire.toArray(fireEffects);
		triggerEffects(fireEffects);
	}
	
	private void doPinwheel() {
		int slowDown = 5;
		int fireEffect = (int) ((step / slowDown) % num_effects_in_ring);
		triggerEffect(fireEffect);
	}

	private final Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			//Log.e(LOG_TAG, "msg: " + msg);
			switch (msg.what) {
			case TGDevice.MSG_STATE_CHANGE:
				switch (msg.arg1) {
				case TGDevice.STATE_IDLE:
					break;
				case TGDevice.STATE_CONNECTING:
					Log.i(LOG_TAG, "Connecting to bluetooth device");
					break;
				case TGDevice.STATE_CONNECTED:
					tgDevice.start();
					Log.i(LOG_TAG, "Connected to bluetooth device");
					break;
				case TGDevice.STATE_DISCONNECTED:
					break;
				case TGDevice.STATE_NOT_FOUND:
				case TGDevice.STATE_NOT_PAIRED:
				default:
					break;
				}
				break;
			case TGDevice.MSG_RAW_DATA:
				break;
			case TGDevice.MSG_RAW_MULTI:
				TGRawMulti multiRaw = (TGRawMulti)msg.obj;
				Log.e(LOG_TAG, "raw multi: " + multiRaw.ch1);
				break;
			case TGDevice.MSG_POOR_SIGNAL:
				Integer signalValue = (200 - msg.arg1) / 2;
				signal.setText(signalValue.toString());
				break;
			case TGDevice.MSG_ATTENTION:
				Integer attentionValue = msg.arg1;
				attentionSeries.appendData(new GraphViewData(attentionSeriesLastX, attentionValue), true);
				attentionSeriesLastX += 1d;
				attention.setText(attentionValue.toString());
				attentionShouldFire = attentionValue > fireOnAttentionOf;
				break;
			case TGDevice.MSG_MEDITATION:
				Integer meditationValue = msg.arg1;
				meditationSeries.appendData(new GraphViewData(meditationSeriesLastX, meditationValue), true);
				meditationSeriesLastX += 1d;
				meditation.setText(meditationValue.toString());
				meditationShouldFire = meditationValue > fireOnMeditationOf;
				break;
			case TGDevice.MSG_HEART_RATE:
				break;
			case TGDevice.MSG_BLINK:
				// TODO show blink icon
				break;
			case TGDevice.MSG_EEG_POWER:
				TGEegPower ep = (TGEegPower)msg.obj;
				Log.e(LOG_TAG, "Delta: " + ep.delta);
			default:
				break;
			}
		}
	};
}
