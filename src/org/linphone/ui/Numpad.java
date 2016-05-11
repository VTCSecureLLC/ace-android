/*
NumpadView.java
Developed pursuant to contract FCC15C0008 as open source software under GNU General Public License version 2.

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
package org.linphone.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.linphone.LinphoneActivity;
import org.linphone.R;
import org.linphone.custom.HapticFeedback;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Guillaume Beraudo
 *
 */
public class Numpad extends LinearLayout implements AddressAware {

	private boolean mPlayDtmf;
	private Context mContext;

	public void setPlayDtmf(boolean sendDtmf) {
		this.mPlayDtmf = sendDtmf;
	}



	HapticFeedback mHaptic = new HapticFeedback();


	public Numpad(Context context, boolean playDtmf) {
		super(context);
		mContext = context;
		mPlayDtmf = playDtmf;
		View view=LayoutInflater.from(context).inflate(R.layout.numpad, this);
		setLongClickable(true);
		onFinishInflate();
		setNumpadColors((ViewGroup) view);

	}

	public void setDTMFSoundEnabled(boolean enabled)
	{
		mHaptic.setDTMFSoundEnabled(enabled);
	}

	public void setHapticEnabled(boolean enabled)
	{
		mHaptic.setHapticEnabled(enabled);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mHaptic.init(mContext, true);
		mHaptic.checkSystemSetting();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mHaptic.deInit();
	}

	public void recheckSystemSettings()
	{
		mHaptic.checkSystemSetting();
	}

	public Numpad(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Numpad);
		mPlayDtmf = 1 == a.getInt(R.styleable.Numpad_play_dtmf, 1);

		a.recycle();
		View view=LayoutInflater.from(context).inflate(R.layout.numpad, this);
		setLongClickable(true);
		setNumpadColors((ViewGroup) view);
	}

	public void setNumpadColors(ViewGroup view){

		try {
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.instance());
			String color_theme = prefs.getString(getContext().getResources().getString(R.string.pref_theme_app_color_key), "Tech");

			if(color_theme.equals("Tech")) {
				techify(view,R.id.Digit1,"1\n   ");
				techify(view,R.id.Digit2,"2\nABC");
				techify(view, R.id.Digit3, "3\nDEF");
				techify(view, R.id.Digit4, "4\nGHI");
				techify(view, R.id.Digit5, "5\nJKL");
				techify(view,R.id.Digit6,"6\nMNO");
				techify(view,R.id.Digit7,"7\nPQRS");
				techify(view,R.id.Digit8,"8\nTUV");
				techify(view, R.id.Digit9, "9\nWXYZ");
				techify(view, R.id.DigitStar, "*\n   ");
				techify(view, R.id.Digit00, "0\n + ");
				techify(view, R.id.DigitHash, "#\n   ");

			}else{


				view.findViewById(R.id.Digit1).setBackgroundResource(R.drawable.numpad_one);
				view.findViewById(R.id.Digit2).setBackgroundResource(R.drawable.numpad_two);
				view.findViewById(R.id.Digit3).setBackgroundResource(R.drawable.numpad_three);
				view.findViewById(R.id.Digit4).setBackgroundResource(R.drawable.numpad_four);
				view.findViewById(R.id.Digit5).setBackgroundResource(R.drawable.numpad_five);
				view.findViewById(R.id.Digit6).setBackgroundResource(R.drawable.numpad_six);
				view.findViewById(R.id.Digit7).setBackgroundResource(R.drawable.numpad_seven);
				view.findViewById(R.id.Digit8).setBackgroundResource(R.drawable.numpad_eight);
				view.findViewById(R.id.Digit9).setBackgroundResource(R.drawable.numpad_nine);
				view.findViewById(R.id.DigitStar).setBackgroundResource(R.drawable.numpad_star);
				view.findViewById(R.id.Digit00).setBackgroundResource(R.drawable.numpad_zero);
				view.findViewById(R.id.DigitHash).setBackgroundResource(R.drawable.numpad_sharp);

			}

		}catch(Throwable e){

		}

	}
	public void techify(View view, int id, String s){
		view.findViewById(id).setBackgroundResource(R.drawable.numpad_generic_tech);
//		view.findViewById(id).getBackground().setAlpha(127);
		((Button)view.findViewById(id)).setTextSize(44);

		SpannableString ss;
		ss=  new SpannableString(s);
		ss.setSpan(new RelativeSizeSpan(.4f), 2, ss.length(), 0); // set size
		((Button)view.findViewById(id)).setTextColor(Color.WHITE);
		((Button)view.findViewById(id)).setText(ss);
		((Button)view.findViewById(id)).setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, getResources().getDisplayMetrics()), .8f);
		((Button)view.findViewById(id)).setGravity(Gravity.CENTER);

	}
	@Override
	protected final void onFinishInflate() {
		for (Digit v : retrieveChildren(this, Digit.class)) {
			v.setPlayDtmf(mPlayDtmf);
			v.setFeedbackHandler(mHaptic);
		}
		super.onFinishInflate();
	}
	public void setAddressWidget(AddressText address) {
		for (AddressAware v : retrieveChildren(this, AddressAware.class)) {
			v.setAddressWidget(address);
		}
	}


	private final <T> Collection<T> retrieveChildren(ViewGroup viewGroup, Class<T> clazz) {
		final Collection<T> views = new ArrayList<T>();

		for (int i = 0; i < viewGroup.getChildCount(); i++) {
			View v = viewGroup.getChildAt(i);
			if (v instanceof ViewGroup) {
				views.addAll(retrieveChildren((ViewGroup) v, clazz));
			} else {
				if (clazz.isInstance(v))
					views.add(clazz.cast(v));
			}
		}

		return views;
	}

}
