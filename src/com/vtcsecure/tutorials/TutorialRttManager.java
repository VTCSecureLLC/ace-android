package com.vtcsecure.tutorials;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.LogCollectionUploadState;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCore.RemoteProvisioningState;
import org.linphone.core.LinphoneCore.Transports;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;
import org.linphone.mediastream.Log;

import android.content.Context;

public class TutorialRttManager implements LinphoneCoreListener {
	public LinphoneCore core;
	private Context context;
	private Timer timer;
	
	public TutorialRttManager(Context c) {
		context = c;
		LinphoneCoreFactory.instance().setDebugMode(true, "[RTT]");
		
		try {
			core = LinphoneCoreFactory.instance().createLinphoneCore(this, null, null, null, context);
			
			Transports transports = core.getSignalingTransportPorts();
			transports.udp = 55111;
			transports.tcp = 0;
			transports.tls = 0;
			core.setSignalingTransportPorts(transports);
			
			startIterate();
	        core.setNetworkReachable(true); // Let's assume it's true
		} catch (LinphoneCoreException e) {
			Log.e(e);
		}
	}
	
	public void destroy() {
		try {
			timer.cancel();
			core.destroy();
		}
		catch (RuntimeException e) {
		}
		finally {
			core = null;
		}
	}
	
	public void startCall(String to) {
		try {
			LinphoneCallParams params = core.createDefaultCallParameters();
			params.enableRealTimeText(true);
			LinphoneAddress destination = LinphoneCoreFactory.instance().createLinphoneAddress(to);
			core.inviteAddressWithParams(destination, params);
		} catch (LinphoneCoreException e) {
			Log.e(e);
		}
	}
	
	private void startIterate() {
		TimerTask lTask = new TimerTask() {
			@Override
			public void run() {
				core.iterate();
			}
		};
		
		/*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
		timer = new Timer("RTT tutorial scheduler");
		timer.schedule(lTask, 0, 20); 
	}
	
	@Override
	public void globalState(LinphoneCore lc, GlobalState state, String message) {
	}

	@Override
	public void callState(LinphoneCore lc, LinphoneCall call, State cstate, String message) {
	}

	@Override
	public void callStatsUpdated(LinphoneCore lc, LinphoneCall call,
			LinphoneCallStats stats) {
		
	}

	@Override
	public void callEncryptionChanged(LinphoneCore lc, LinphoneCall call,
			boolean encrypted, String authenticationToken) {
		
	}

	@Override
	public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg,
			RegistrationState cstate, String smessage) {
	}

	@Override
	public void newSubscriptionRequest(LinphoneCore lc, LinphoneFriend lf,
			String url) {
		
	}

	@Override
	public void notifyPresenceReceived(LinphoneCore lc, LinphoneFriend lf) {
		
	}

	@Override
	public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr, LinphoneChatMessage message) {
	}

	@Override
	public void isComposingReceived(LinphoneCore lc, LinphoneChatRoom cr) {
	}

	@Override
	public void dtmfReceived(LinphoneCore lc, LinphoneCall call, int dtmf) {
		
	}

	@Override
	public void ecCalibrationStatus(LinphoneCore lc, EcCalibratorStatus status,
			int delay_ms, Object data) {
		
	}

	@Override
	public void notifyReceived(LinphoneCore lc, LinphoneCall call,
			LinphoneAddress from, byte[] event) {
		
	}

	@Override
	public void transferState(LinphoneCore lc, LinphoneCall call,
			State new_call_state) {
		
	}

	@Override
	public void infoReceived(LinphoneCore lc, LinphoneCall call,
			LinphoneInfoMessage info) {
		
	}

	@Override
	public void subscriptionStateChanged(LinphoneCore lc, LinphoneEvent ev,
			SubscriptionState state) {
		
	}

	@Override
	public void notifyReceived(LinphoneCore lc, LinphoneEvent ev,
			String eventName, LinphoneContent content) {
	}

	@Override
	public void publishStateChanged(LinphoneCore lc, LinphoneEvent ev,
			PublishState state) {
		
	}

	@Override
	public void configuringStatus(LinphoneCore lc,
			RemoteProvisioningState state, String message) {
	}

	@Override
	public void show(LinphoneCore lc) {
		
	}

	@Override
	public void displayStatus(LinphoneCore lc, String message) {
		
	}

	@Override
	public void displayMessage(LinphoneCore lc, String message) {
		
	}

	@Override
	public void displayWarning(LinphoneCore lc, String message) {
		
	}

	@Override
	public void authInfoRequested(LinphoneCore lc, String realm,
			String username, String Domain) {
		
	}

	@Override
	public void fileTransferProgressIndication(LinphoneCore lc,
			LinphoneChatMessage message, LinphoneContent content, int progress) {
		
	}

	@Override
	public void fileTransferRecv(LinphoneCore lc, LinphoneChatMessage message,
			LinphoneContent content, byte[] buffer, int size) {
		
	}

	@Override
	public int fileTransferSend(LinphoneCore lc, LinphoneChatMessage message,
			LinphoneContent content, ByteBuffer buffer, int size) {
		return 0;
	}

	@Override
	public void uploadProgressIndication(LinphoneCore lc, int offset, int total) {
		
	}

	@Override
	public void uploadStateChanged(LinphoneCore lc,
			LogCollectionUploadState state, String info) {
		
	}

}
