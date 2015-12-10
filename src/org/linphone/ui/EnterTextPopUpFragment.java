package org.linphone.ui;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.linphone.R;

public class EnterTextPopUpFragment extends DialogFragment {
    EditText et_password;
    Button bt_submit;
    EnterTextPopupListener mListener;
    public interface EnterTextPopupListener{
        void onPasswordSubmitted(String input);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.enter_text_popup_fragment_layout, container);
        et_password = (EditText)view.findViewById(R.id.entertext_popup_password);
        bt_submit = (Button)view.findViewById(R.id.entertext_popup_submit);
        bt_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPassword();
                dismiss();
            }
        });
        return view;
    }

    public void attachListener(EnterTextPopupListener listener){
        this.mListener = listener;
    }
    protected void submitPassword(){
        if(mListener != null) {
            mListener.onPasswordSubmitted(et_password.getText().toString());
        }
    }
}
