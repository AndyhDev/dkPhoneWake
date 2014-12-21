package com.dk.phonewake;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Service extends android.app.Service{
	private final IBinder binder = new MyBinder();
	private static final String LIST_KEY = "list_key";
	private boolean active = false;
	private int prevRingMode = -1;
	private int prevRingVolume = -1;
	
	private int prevState = -1;
	
	private final int STATE_RING = 1;
	private final int STATE_OFFHOOK = 2;
	private final int STATE_IDLE = 3;
	
	private int notifyId = 2;

	private RemoteControl remoteControlReceiver;
	private Receiver receiver;
	private ArrayList<String> numbers;
	private AudioManager aManager;
	
	private static final String ACTION_STOP = "stop";
	
	@Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
		aManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		
		startNotify();
		
		if(remoteControlReceiver == null){
			remoteControlReceiver = new RemoteControl();

			IntentFilter intentFilter = new IntentFilter(RemoteControlReceiver.REMOTE_CONTROL_ACTION);
			registerReceiver(remoteControlReceiver, intentFilter);
		}
		if(receiver == null){
			receiver = new Receiver();

			IntentFilter intentFilter = new IntentFilter("android.intent.action.PHONE_STATE");
			registerReceiver(receiver, intentFilter);
		}
	    return Service.START_NOT_STICKY;
	  }
	
	private void startNotify(){
		Log.d("1PHONE", "quiet()1");
		
		active = true;
		/*int curApi = android.os.Build.VERSION.SDK_INT;
		if(curApi >= 21){
			startNotifyApi21();
		}else if(curApi >= android.os.Build.VERSION_CODES.JELLY_BEAN){
			startNotifyApi16();
		}else{
			startNotifyApi14();
		}*/
		startNotifyApi14();
	}
	
	private void stopNotify(){
		active = false;
		stopForeground(true);
	}
	private void startNotifyApi14(){

		Intent contentIntent = new Intent(this, MainActivity.class);
		PendingIntent pContentIntent = PendingIntent.getActivity(this, 0, contentIntent, 0);

		Intent stopIntent = new Intent(this, RemoteControlReceiver.class);
		stopIntent.setAction(ACTION_STOP);
		PendingIntent pStopIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);


		android.support.v4.app.NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_stat_phonewake_notify);
		builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		builder.setContentText(getString(R.string.notify_content));
		builder.setContentTitle(getString(R.string.app_name));
		builder.setContentIntent(pContentIntent);
		builder.addAction(R.drawable.ic_action_cancel, "", pStopIntent);
		
		Notification n = builder.build();


		startForeground(notifyId , n);
	}
	
	public class RemoteControl extends BroadcastReceiver{
		private static final String TAG = "RemoteControl";

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getStringExtra(RemoteControlReceiver.REMOTE_CONTROL_DO);
			Log.d(TAG, "action=" + action);
			if(action.equals(ACTION_STOP)){
				stopNotify();
			}
		}
	}
	
	public void newState(int state, String number){
		if(!active){
			return;
		}
		if(state == STATE_RING){
			if(number != null){
				loadList();
				if(isNumberIn(number)){
					loud();
				}
			}
		}
		Log.d("1PHONE", "prev:" + prevState);
		if(state == STATE_IDLE){
			if(prevState == STATE_OFFHOOK || prevState == STATE_RING){
				quiet();
			}
		}
		prevState = state;
	}
	
	private void loud() {
		Log.d("1PHONE", "load()");
		prevRingMode = aManager.getRingerMode();
		prevRingVolume = aManager.getStreamVolume(AudioManager.STREAM_RING);
		
		int streamMaxVolume = aManager.getStreamMaxVolume(AudioManager.STREAM_RING);
		aManager.setStreamVolume(AudioManager.STREAM_RING, streamMaxVolume, AudioManager.FLAG_ALLOW_RINGER_MODES|AudioManager.FLAG_PLAY_SOUND);
		aManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
	}
	
	private void quiet(){
		Log.d("1PHONE", "quiet()");
		Log.d("1PHONE", "prevRingMode:" + prevRingMode);
		Log.d("1PHONE", "prevRingVolume:" + prevRingVolume);
		if(prevRingMode != -1){
			int curApi = android.os.Build.VERSION.SDK_INT;
			if(curApi >= 21){
				prevRingMode = AudioManager.RINGER_MODE_SILENT;
				prevRingVolume = 0;
				
				aManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				aManager.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_ALLOW_RINGER_MODES|AudioManager.FLAG_PLAY_SOUND);
				return;
			}
			
			Log.d("1PHONE", "quiet()1");
			aManager.setStreamVolume(AudioManager.STREAM_RING, prevRingVolume, AudioManager.FLAG_ALLOW_RINGER_MODES|AudioManager.FLAG_PLAY_SOUND);
			aManager.setRingerMode(prevRingMode);
			
		}
		
	}
	private boolean isNumberIn(String number){
		for(int i = 0; i < numbers.size(); i++){
			if(PhoneNumberUtils.compare(number, numbers.get(i))){
				return true;
			}
		}
		return false;
	}
	private void loadList(){
		SharedPreferences prefs = getSharedPreferences("list", MODE_PRIVATE);
		String raw = prefs.getString(LIST_KEY, "[]");
		numbers = new ArrayList<String>();
		try {
			JSONArray array = new JSONArray(raw);
			for(int i = 0; i < array.length(); i++){
				JSONArray c = array.optJSONArray(i);
				String number = c.optString(1);
				if(number != null){
					numbers.add(number);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	public class Receiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
			if(state != null){
				if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
					if(prevState  != STATE_RING){
						String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
						newState(STATE_RING, incomingNumber);
						Log.d("1PHONE", "RING " + incomingNumber);
					}

				}else if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
					if(prevState != STATE_OFFHOOK){
						newState(STATE_OFFHOOK, null);
						Log.d("1PHONE", "OFFHOOK");
					}

				}else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
					if(prevState != STATE_IDLE){
						newState(STATE_IDLE, null);
						Log.d("1PHONE", "IDLE");
					}

				}
			}
		}
	}
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public class MyBinder extends Binder {
	    Service getService() {
	      return Service.this;
	    }
	} 
}
