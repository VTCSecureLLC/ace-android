/*
NumpadView.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

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
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.linphone.LinphoneActivity;
import org.linphone.R;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Guillaume Beraudo
 *
 */
public class Numpad extends LinearLayout implements AddressAware {

	private boolean mPlayDtmf;
	public void setPlayDtmf(boolean sendDtmf) {
		this.mPlayDtmf = sendDtmf;
	}





	public Numpad(Context context, boolean playDtmf) {
		super(context);
		mPlayDtmf = playDtmf;
		View view=LayoutInflater.from(context).inflate(R.layout.numpad, this);
		setLongClickable(true);
		onFinishInflate();
		setNumpadColors((ViewGroup) view);
	}

	public Numpad(Context context, AttributeSet attrs) {
		super(context, attrs);
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
			String color_theme = prefs.getString(getContext().getResources().getString(R.string.pref_theme_app_color_key), "default");



			if(color_theme.equals("Red")) {

				view.findViewById(R.id.Digit1).setBackgroundResource(R.drawable.numpad_one_red);
				view.findViewById(R.id.Digit2).setBackgroundResource(R.drawable.numpad_two_red);
				view.findViewById(R.id.Digit3).setBackgroundResource(R.drawable.numpad_three_red);
				view.findViewById(R.id.Digit4).setBackgroundResource(R.drawable.numpad_four_red);
				view.findViewById(R.id.Digit5).setBackgroundResource(R.drawable.numpad_five_red);
				view.findViewById(R.id.Digit6).setBackgroundResource(R.drawable.numpad_six_red);
				view.findViewById(R.id.Digit7).setBackgroundResource(R.drawable.numpad_seven_red);
				view.findViewById(R.id.Digit8).setBackgroundResource(R.drawable.numpad_eight_red);
				view.findViewById(R.id.Digit9).setBackgroundResource(R.drawable.numpad_nine_red);
				view.findViewById(R.id.DigitStar).setBackgroundResource(R.drawable.numpad_star_red);
				view.findViewById(R.id.Digit00).setBackgroundResource(R.drawable.numpad_zero_red);
				view.findViewById(R.id.DigitHash).setBackgroundResource(R.drawable.numpad_sharp_red);

			}else if(color_theme.equals("Yellow")) {
				view.findViewById(R.id.Digit1).setBackgroundResource(R.drawable.numpad_one_yellow);
				view.findViewById(R.id.Digit2).setBackgroundResource(R.drawable.numpad_two_yellow);
				view.findViewById(R.id.Digit3).setBackgroundResource(R.drawable.numpad_three_yellow);
				view.findViewById(R.id.Digit4).setBackgroundResource(R.drawable.numpad_four_yellow);
				view.findViewById(R.id.Digit5).setBackgroundResource(R.drawable.numpad_five_yellow);
				view.findViewById(R.id.Digit6).setBackgroundResource(R.drawable.numpad_six_yellow);
				view.findViewById(R.id.Digit7).setBackgroundResource(R.drawable.numpad_seven_yellow);
				view.findViewById(R.id.Digit8).setBackgroundResource(R.drawable.numpad_eight_yellow);
				view.findViewById(R.id.Digit9).setBackgroundResource(R.drawable.numpad_nine_yellow);
				view.findViewById(R.id.DigitStar).setBackgroundResource(R.drawable.numpad_star_yellow);
				view.findViewById(R.id.Digit00).setBackgroundResource(R.drawable.numpad_zero_yellow);
				view.findViewById(R.id.DigitHash).setBackgroundResource(R.drawable.numpad_sharp_yellow);
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
	@Override
	protected final void onFinishInflate() {
		for (Digit v : retrieveChildren(this, Digit.class)) {
			v.setPlayDtmf(mPlayDtmf);
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
