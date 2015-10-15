package com.vtcsecure.tutorials;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.widget.TextView;
import android.widget.Toast;

import com.vtcsecure.R;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.Transports;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

public class TutorialRttReceiver extends Activity {
	private TutorialRttManager manager;
	private LinphoneCoreListenerBase listener;
	private Handler handler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rtt_receiver_tuto);
		
		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
		
		manager = new TutorialRttManager(this);
		listener = new LinphoneCoreListenerBase() {
			@Override
			public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, final LinphoneChatMessage message) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(TutorialRttReceiver.this, message.getText(), Toast.LENGTH_SHORT).show();
					}
				});
				lc.terminateAllCalls();
			}
			
			@Override
			public void isComposingReceived(LinphoneCore core, LinphoneChatRoom room) {
				LinphoneCall call = room.getCall();
				if (call != null && call.getCurrentParamsCopy().realTimeTextEnabled()) {
					final char c = (char) room.getChar();
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(TutorialRttReceiver.this, String.valueOf(c), Toast.LENGTH_SHORT).show();
						}
					});
				}
			}
			
			@Override
			public void callState(LinphoneCore lc, LinphoneCall call, State state, String message) {
				if (state == LinphoneCall.State.IncomingReceived) {
					try {
						manager.core.acceptCall(call);
					} catch (LinphoneCoreException e) {
						Log.e(e);
					}
				}
			}
		};
		
		Transports ports = manager.core.getSignalingTransportPorts();
		((TextView)findViewById(R.id.localip)).setText("Local IP is " + ip + ", udp port is " + ports.udp);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		manager.core.addListener(listener);
	}
	
	@Override
	protected void onPause() {
		manager.core.removeListener(listener);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		manager.core.terminateAllCalls();
		manager.destroy();
		super.onDestroy();
	}
}
