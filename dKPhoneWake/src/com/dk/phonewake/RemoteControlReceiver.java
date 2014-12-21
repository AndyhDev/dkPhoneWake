package com.dk.phonewake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RemoteControlReceiver extends BroadcastReceiver{
	private static final String TAG = "RemoteControlRecevier";

	public static final String REMOTE_CONTROL_ACTION = "REMOTE_CONTROL_ACTION";
	public static final String REMOTE_CONTROL_DO = "REMOTE_CONTROL_DO";
	
	@Override
	public void onReceive(Context context, Intent i) {
		Log.d(TAG, "onReceive");
		
		Intent intent = new Intent(REMOTE_CONTROL_ACTION);
		intent.putExtra(REMOTE_CONTROL_DO, i.getAction());
		context.sendBroadcast(intent);
	}
}
