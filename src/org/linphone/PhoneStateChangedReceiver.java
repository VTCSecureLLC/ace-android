/*
PhoneStateReceiver.java
Copyright (C) 2011  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.linphone;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * Pause current SIP calls when GSM phone rings or is active.
 * 
 * @author Guillaume Beraudo
 *
 */
public class PhoneStateChangedReceiver extends BroadcastReceiver {


	static LinphoneCall currentCall;
	@Override
	public void onReceive(Context context, Intent intent) {
		if(!LinphoneManager.isInstanciated())
			return;
		LinphoneCore lc = LinphoneManager.getLc();
		int callCount = lc.getCallsNb();


		final String extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

		if (/*TelephonyManager.EXTRA_STATE_RINGING.equals(extraState) ||*/ TelephonyManager.EXTRA_STATE_OFFHOOK.equals(extraState)) {
			LinphoneManager.setGsmIdle(false);
			if (!LinphoneManager.isInstanciated()) {
				Log.i("GSM call state changed but manager not instantiated");
				return;
			}
			if(callCount>1 && currentCall == null)
			{
				currentCall = lc.getCurrentCall();
			}

			lc.pauseAllCalls();
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(extraState)) {
        	LinphoneManager.setGsmIdle(true);
			if (callCount == 1)
				lc.resumeCall(lc.getCalls()[0]);
			else if(callCount> 0)
			{
				if(callExist(lc.getCalls()))
				{
					lc.resumeCall(currentCall);
				}
				else if(lc.getCurrentCall() == null)
					lc.resumeCall(lc.getCalls()[0]);
			}
			currentCall = null;

        }

	}

	boolean callExist(LinphoneCall[] calls)
	{
		boolean res = false;
		if(currentCall == null)
			return false;
		for (LinphoneCall call :
				calls) {
			if(call.equals(currentCall))
				res = true;
		}
		return res;
	}

}
