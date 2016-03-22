package util;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;

import org.linphone.Contact;
import org.linphone.ContactsManager;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by tarevik on 3/21/16.
 */
public class ContactUtils {

    public static LinphoneFriendList getLinphoneFriendsFromContacts(Context context, LinphoneCore lc)
    {

        Pattern SIP_URI_PATTERN = Pattern.compile(
                "^(sip(?:s)?):(?:[^:]*(?::[^@]*)?@)?([^:@]*)(?::([0-9]*))?$", Pattern.CASE_INSENSITIVE);

        ContactsManager m = ContactsManager.getInstance();
        ContentResolver cr = context.getContentResolver();
        Cursor sipCusrsor = Compatibility.getSIPContactsCursor(cr, null);


        ArrayList<Contact> contacts = new ArrayList<org.linphone.Contact>();
        while (sipCusrsor.moveToNext())
        {
            String id = sipCusrsor.getString(sipCusrsor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
            String name = sipCusrsor.getString(sipCusrsor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            org.linphone.Contact contact = new org.linphone.Contact(id, name);
            contact.refresh(cr);
            contacts.add(contact);
        }
        sipCusrsor.close();


        LinphoneFriendList lfl = null;

        try {
            lfl = lc.createLinphoneFriendList();
            //lc.addFriendList(lfl);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
            //app.updateStatus("Error occurred during export");
            //isRunning = false;
            //stopSelf();
            return null;
        }

        if(sipCusrsor.getCount()==0)
        {

            return lfl;
        }
        for (org.linphone.Contact contact : contacts)
        {
            LinphoneFriend lf = LinphoneCoreFactory.instance().createLinphoneFriend();
            lf.setName(contact.getName());
            lf.setRefKey(contact.getID());
            boolean hasSip = false;
            for(String number : contact.getNumbersOrAddresses())
            {
                if(SIP_URI_PATTERN.matcher(number).matches()) {
                    try {
                        lf.setAddress(LinphoneCoreFactory.instance().createLinphoneAddress(number));
                        hasSip = true;
                        break;
                    } catch (LinphoneCoreException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(hasSip)
                lfl.addFriend(lf);


        }
        return lfl;
    }

    public static void exportFriendListToContacts(Context context, LinphoneFriendList lfl)
    {
        ContentResolver cr = context.getContentResolver();
        ArrayList<ContentProviderOperation> ops;
        for (LinphoneFriend friend: lfl.getFriendList()
                ) {
            ops = new ArrayList<ContentProviderOperation>();

            String [] projection = new String[]  {ContactsContract.Data.CONTACT_ID};
            String selection = new StringBuilder()
                    .append(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS)
                    .append(" = ?").toString();

            org.linphone.Contact contact_to_udate = ContactsManager.getInstance().findContactWithAddress(cr, friend.getAddress());


            if(contact_to_udate!=null)
            {
                ContactsManager.getInstance().updateExistingContact(ops, contact_to_udate.getID(), friend.getName());

            } else
            {
                //insert
                String uri = friend.getAddress().asStringUriOnly();
                if(uri.startsWith("sip:"))
                    uri = uri.substring(4);
                ContactsManager.getInstance().createNewContact(ops,friend.getName() );
                Compatibility.addSipAddressToContact(context, ops, uri);
            }
            if(ops.size()>0)
                try {
                    cr.applyBatch(ContactsContract.AUTHORITY, ops);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }

            //lc.removeFriendList(list);

        }
    }
}
