package com.vtcsecure.tutorials;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.vtcsecure.R;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

public class TutorialRttSender extends Activity implements OnClickListener {
	private TutorialRttManager manager;
	private LinphoneCoreListenerBase listener;
	private LinphoneChatMessage message;
	private Handler handler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rtt_sender_tuto);
		
		manager = new TutorialRttManager(this);
		listener = new LinphoneCoreListenerBase() {
			@Override
			public void callState(LinphoneCore lc, LinphoneCall call, State state, String msg) {
				if (state == LinphoneCall.State.StreamsRunning) {
					LinphoneChatRoom room = call.getChatRoom();
					message = room.createLinphoneChatMessage("");
					for (final char c : "Hello World !".toCharArray()) {
						try {
							message.putChar(c);
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(TutorialRttSender.this, String.valueOf(c), Toast.LENGTH_SHORT).show();
								}
							});
						} catch (LinphoneCoreException e) {
							Log.e(e);
						}
					}
					room.sendChatMessage(message);
				}
			}
		};
		message = null;
		
		findViewById(R.id.call).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.call) {
			String dest = ((EditText)findViewById(R.id.dest)).getText().toString();
			manager.startCall(dest);
		}
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
