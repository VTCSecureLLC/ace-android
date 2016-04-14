package org.linphone.sync;

import android.content.Context;
import android.os.AsyncTask;

import org.linphone.LinphoneManager;
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

    public ContactSyncAsyncTask(Context context, String username, String password) {
        mContext = context;
        this.username = username;
        this.password = password;
        this.username = "dood";
    }


    @Override
    protected Void doInBackground(Void[] params) {

        if(isRunning)
        {
            return null;
        }
        isRunning = true;

        LinphoneCore lc = LinphoneManager.getLc();;

        String domain = null;
        try {
            String dnsLookUp = "_carddav._tcp.mi-st.vatrp.org";
            Lookup lookup = new Lookup(dnsLookUp, Type.SRV);
            Record[] records = lookup.run();

            domain = "http://" + ((SRVRecord) records[0]).getTarget().toString().replaceFirst("\\.$", "");

        } catch (TextParseException e) {
            e.printStackTrace();
        }



        syncContactsLinphone(lc, domain);
        return null;
    }


    public void syncContactsLinphone(final LinphoneCore lc, String serverUrl)
    {

        String serverDomain = serverUrl.replace("http://", "").replace("https://", "").split("/")[0]; // We just want the domain name
        //serverUrl = "http://ace-carddav-baikal.vatrp.org/html/card.php/principals/dood";
        serverUrl = "http://ace-carddav-baikal.vatrp.org/html/card.php/principals/dood";
        LinphoneAuthInfo[] authInfos = lc.getAuthInfosList();

        boolean need_new_account = true;
        for (int i = 0; i < authInfos.length; i++) {
            if (authInfos[i].getDomain().equals(serverDomain)){
                if (!authInfos[i].getUsername().equals(username) || !authInfos[i].getPassword().equals(password)) {
                    lc.removeAuthInfo(authInfos[i]);
                }
                else
                {
                    need_new_account = false;
                }
            }
        }
        if (need_new_account)
        {
            LinphoneAuthInfo newInfo = LinphoneCoreFactory.instance().createAuthInfo(username, password, null, serverDomain);
            lc.addAuthInfo(newInfo);
        }

        for (LinphoneFriendList list: lc.getFriendLists()
                ) {
            try {
                lc.removeFriendList(list);
            } catch (LinphoneCoreException e) {
                e.printStackTrace();
            }
        }

        LinphoneFriendList lfl = ContactUtils.getLinphoneFriendsFromContacts(mContext, lc);
//        try {
//            lfl = lc.createLinphoneFriendList();
//        } catch (LinphoneCoreException e) {
//            e.printStackTrace();
//        }
        LinphoneFriend friend = LinphoneCoreFactory.instance().createLinphoneFriend();
        try {
            friend.setAddress(LinphoneCoreFactory.instance().createLinphoneAddress("sip:tt@sip.tt.com"));
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
        lfl.addFriend(friend);

        try {
            lc.addFriendList(lfl);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
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
        lfl.setUri(serverUrl);
        lfl.setListener(this);
        lfl.synchronizeFriendsFromServer();
    }


    @Override
    public void onLinphoneFriendCreated(LinphoneFriendList list, LinphoneFriend lf) {

    }

    @Override
    public void onLinphoneFriendUpdated(LinphoneFriendList list, LinphoneFriend newFriend, LinphoneFriend oldFriend) {

    }

    @Override
    public void onLinphoneFriendDeleted(LinphoneFriendList list, LinphoneFriend lf) {

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
            ContactUtils.exportFriendListToContacts(mContext, list);
        }
    }
}
