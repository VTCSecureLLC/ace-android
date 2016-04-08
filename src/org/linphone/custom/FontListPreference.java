package org.linphone.custom;

/**
 * Created by accontech-samson on 4/4/16.
 */
import java.util.ArrayList;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.app.Dialog;
import android.app.AlertDialog.Builder;

import org.linphone.R;

public class FontListPreference extends ListPreference
{
    CustomListPreferenceAdapter customListPreferenceAdapter = null;
    Context mContext;
    private LayoutInflater mInflater;
    CharSequence[] entries;
    CharSequence[] entryValues;
    ArrayList<RadioButton> rButtonList;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    private int mClickedDialogEntryIndex;


    public FontListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        rButtonList = new ArrayList<RadioButton>();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = prefs.edit();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder)
    {
        entries = getEntries();
        entryValues = getEntryValues();

        if (entries == null || entryValues == null || entries.length != entryValues.length )
        {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        customListPreferenceAdapter = new CustomListPreferenceAdapter(mContext);
        mClickedDialogEntryIndex = findIndexOfValue(getValue());
        builder.setAdapter(customListPreferenceAdapter, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                mClickedDialogEntryIndex = which;
                FontListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(null, null);
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mClickedDialogEntryIndex >= 0 && entryValues != null) {
            String value = entryValues[mClickedDialogEntryIndex].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
        }
    }

    private class CustomListPreferenceAdapter extends BaseAdapter
    {
        public CustomListPreferenceAdapter(Context context)
        {

        }

        public int getCount()
        {
            return entries.length;
        }

        public Object getItem(int position)
        {
            return position;
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent)
        {
            View row = convertView;

            if(row == null)
            {

                row = mInflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false);

            }
            CheckedTextView tv = (CheckedTextView) row;
            try {
                String family = entries[position].toString();
                Typeface tf;
                if(family.equals("Default"))
                {
                    tf = Typeface.DEFAULT;
                }else{
                    tf = Typeface.create(family, Typeface.NORMAL);
                }
                tv.setTypeface(tf);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            tv.setText(entries[position]);
            if(position == mClickedDialogEntryIndex)
                tv.setChecked(true);
            else
                tv.setChecked(false);
            return row;
        }


    }
}