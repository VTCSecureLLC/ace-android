package org.linphone.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.mediastream.Log;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import util.ContactUtils;

public class ContactSyncAsyncTask extends AsyncTask<Void, Void, Void> implements LinphoneFriendList.LinphoneFriendListListener {


    private static boolean isRunning;
    static boolean isRunning()
    {
        return isRunning;
    }
    private Context mContext;
    private String username;
    private String password;
    private String serverUrl;
    private String realm;

    public ContactSyncAsyncTask(Context context) {
        mContext = context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.username = prefs.getString(context.getString(R.string.card_dav_username), "");
        this.password = prefs.getString(context.getString(R.string.card_dav_password), "");
        this.realm = prefs.getString(context.getString(R.string.preference_settings_sync_realm), "BaikalDAV");
        this.serverUrl = prefs.getString(context.getString(R.string.preference_settings_sync_path), "");


    }


    @Override
    protected Void doInBackground(Void[] params) {

//        if(isRunning)
//        {
//            return null;
//        }
        isRunning = true;

        LinphoneCore lc = LinphoneManager.getLc();;

        String domain = null;
//        try {
//            String dnsLookUp = "_carddav._tcp.mi-st.vatrp.org";
//            Lookup lookup = new Lookup(dnsLookUp, Type.SRV);
//            Record[] records = lookup.run();
//
//            domain = "http://" + ((SRVRecord) records[0]).getTarget().toString().replaceFirst("\\.$", "");
//
//        } catch (TextParseException e) {
//            e.printStackTrace();
//        }



        syncContactsLinphone(lc);
        return null;
    }


    public void syncContactsLinphone(final LinphoneCore lc)
    {

        //http://dav.linphone.org/card.php/addressbooks/vtcsecure/default
       // serverUrl = "http://dav.linphone.org/card.php/addressbooks/vtcsecure/default";

      //  serverUrl = "http://ace-carddav-sabredav.vatrp.org";
        String serverDomain = serverUrl.replace("http://", "").replace("https://", "").split("/")[0]; // We just want the domain name
        //serverUrl = "http://ace-carddav-baikal.vatrp.org/html/card.php/principals/dood";


        LinphoneAuthInfo[] authInfos = lc.getAuthInfosList();

        boolean need_new_account = true;
        if(authInfos!= null) {
            for (int i = 0; i < authInfos.length; i++) {
                if (authInfos[i].getDomain().equals(serverDomain)) {
                    if (!authInfos[i].getUsername().equals(username) || authInfos[i].getPassword() == null || !authInfos[i].getPassword().equals(password)) {
                        lc.removeAuthInfo(authInfos[i]);
                    } else {
                        need_new_account = false;
                    }
                }
            }
        }
        if (need_new_account)
        {
            LinphoneAuthInfo newInfo = LinphoneCoreFactory.instance().createAuthInfo(username, password, "BaikalDAV", serverDomain);
            lc.addAuthInfo(newInfo);
        }




        LinphoneFriendList lfl = null;

        if(lc.getFriendLists() == null || lc.getFriendLists().length == 0)
        {


            LinphoneFriendList[] lists = lc.getFriendLists();
            if(lists != null)
            {
                for (LinphoneFriendList list: lists ) {
                    try {
                        lc.removeFriendList(list);
                    } catch (LinphoneCoreException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                lfl = lc.createLinphoneFriendList();
                lc.addFriendList(lfl);
            } catch (LinphoneCoreException e) {
                e.printStackTrace();
            }


        }
        else
            lfl = lc.getFriendLists()[0];

        try {
            lc.addFriendList(lfl);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }

        lfl.setUri(serverUrl);

        lfl.setListener(this);


        ContactUtils.updateFriendsFromContacts(lfl, mContext.getContentResolver());
//        try {
//            lfl = lc.createLinphoneFriendList();
//        } catch (LinphoneCoreException e) {
//            e.printStackTrace();
//        }
//        LinphoneFriend friend = LinphoneCoreFactory.instance().createLinphoneFriend();
//        try {
//            friend.setAddress(LinphoneCoreFactory.instance().createLinphoneAddress("sip:tt@sip.tt.com"));
//        } catch (LinphoneCoreException e) {
//            e.printStackTrace();
//        }
//        lfl.addFriend(friend);


        if(lfl==null)
        {
            Log.w("Error occurred during sync");
            isRunning = false;
        }
        else if(lfl.getFriendList() == null || lfl.getFriendList().length == 0)
        {
            Log.w("No contacts found");
            isRunning = false;
        }


        isRunning = false;
        lfl.synchronizeFriendsFromServer();
    }


    @Override
    public void onLinphoneFriendCreated(LinphoneFriendList list, LinphoneFriend lf) {
        Log.d("vcard_sync onLinphoneFriendCreated");
        if(lf.getRefKey() != null)
        {
        }
        else
        {
            ContactUtils.addContact(LinphoneActivity.instance(), lf);

        }

    }

    @Override
    public void onLinphoneFriendUpdated(LinphoneFriendList list, LinphoneFriend newFriend, LinphoneFriend oldFriend) {
        Log.d("vcard_sync onLinphoneFriendUpdated");
        ContactUtils.updateContact(LinphoneActivity.instance(), newFriend, oldFriend);
    }

    @Override
    public void onLinphoneFriendDeleted(LinphoneFriendList list, LinphoneFriend lf) {
        Log.d("vcard_sync onLinphoneFriendDeleted");
        if(lf.getRefKey() != null)
            ContactUtils.deleteContact(LinphoneActivity.instance(), lf);
    }

    @Override
    public void onLinphoneFriendSyncStatusChanged(LinphoneFriendList list, LinphoneFriendList.State status, String message) {
        if (status == LinphoneFriendList.State.SyncStarted){


        }
        else if (status == LinphoneFriendList.State.SyncFailure){
            isRunning = false;
        }
        else if (status == LinphoneFriendList.State.SyncSuccessful){
            isRunning = false;
          //  removeAllCotnacts(list);
            //ContactUtils.exportFriendListToContacts(mContext, list);
        }
    }

    void removeAllCotnacts(final LinphoneFriendList ls)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LinphoneFriend[] fs = ls.getFriendList();
                for (LinphoneFriend f:
                     fs) {
                    LinphoneManager.getLc().removeFriend(f);
                }
            }
        }).start();
    }
}
