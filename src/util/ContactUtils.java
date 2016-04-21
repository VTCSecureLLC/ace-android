package util;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;

import org.linphone.Contact;
import org.linphone.ContactsManager;
import org.linphone.LinphoneManager;
import org.linphone.LinphoneUtils;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.mediastream.*;
import org.linphone.mediastream.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by tarevik on 3/21/16.
 */
public class ContactUtils {

    private  static Pattern SIP_URI_PATTERN = Pattern.compile(
            "^(sip(?:s)?):(?:[^:]*(?::[^@]*)?@)?([^:@]*)(?::([0-9]*))?$", Pattern.CASE_INSENSITIVE);
    public static LinphoneFriendList getLinphoneFriendsFromContacts(Context context, LinphoneCore lc)
    {



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

        // remove not existing contacts for linphonefriend list



        for (org.linphone.Contact contact : contacts)
        {
            LinphoneFriend lf = LinphoneCoreFactory.instance().createLinphoneFriend();

            lf.setName(contact.getName());
            lf.setRefKey(contact.getID());
            boolean hasSip = false;
            for(String number : contact.getNumbersOrAddresses())
            {
                if(SIP_URI_PATTERN.matcher(number).matches()) {
                    if(hasSip == true) {
                        try {
                            lf.setAddress(LinphoneCoreFactory.instance().createLinphoneAddress(number));
                            hasSip = true;

                        } catch (LinphoneCoreException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        lf.addPhoneNumber(number);
                }
                else
                {
                    lf.addPhoneNumber(number);
                }
            }

            if(hasSip) {
                Log.d("vcard_sync addint friend export: " + lf.getName());
                lfl.addFriend(lf);
            }


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


    public static void updateFriendsFromContacts(LinphoneFriendList list, ContentResolver cr)
    {
        deleteNotExistingContacts(list, cr);

        Cursor sipCusrsor = Compatibility.getSIPContactsCursor(cr, null);
        ArrayList<Contact> contacts = new ArrayList<org.linphone.Contact>();
        while (sipCusrsor.moveToNext())
        {
            boolean isNewFriend = false;
            String id = sipCusrsor.getString(sipCusrsor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
            String name = sipCusrsor.getString(sipCusrsor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            org.linphone.Contact contact = new org.linphone.Contact(id, name);
            contact.refresh(cr);
            LinphoneFriend friend = getFriendById(contact.getID(), list);

            if(friend == null)
            {
                friend = LinphoneCoreFactory.instance().createLinphoneFriend();
                friend.setRefKey(contact.getID());
                friend.enableSubscribes(false);
                Log.d("vcard_sync creating friend export: " + name);
                isNewFriend = true;

            }

            if(isNewFriend)
            {
                friend.setName(contact.getName());
                boolean addressIsSet = false;
                for (String contactNumber : contact.getNumbersOrAddresses())
                {
                    if(SIP_URI_PATTERN.matcher(contactNumber).matches() ) {
                        try {
                            LinphoneAddress address = LinphoneCoreFactory.instance().createLinphoneAddress(contactNumber);
                            if(!addressIsSet) {
                                friend.setAddress(address);
                                addressIsSet = true;
                            }
                            else
                                friend.addAddress(address);
                        } catch (LinphoneCoreException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        friend.addPhoneNumber(contactNumber);
                }
            }
            else// we need to update if the contact is changed
            {
                    friend.edit();
                    friend.setName(contact.getName());


                    String[] numbers = friend.getPhoneNumbers();
                    LinphoneAddress[] addresses = friend.getAddresses();


                    for (String number : numbers) {
                        friend.removePhoneNumber(number);
                    }
                    for (LinphoneAddress address: addresses) {
                        friend.removeAddress(address);
                    }

                boolean addressIsSet = false;
                for (String contactNumber : contact.getNumbersOrAddresses())
                {
                    if(SIP_URI_PATTERN.matcher(contactNumber).matches() ) {
                        try {
                            LinphoneAddress address = LinphoneCoreFactory.instance().createLinphoneAddress(contactNumber);
                            if(!addressIsSet)
                            {
                                friend.setAddress(address);
                                addressIsSet = true;
                            }
                            else
                                friend.addAddress(address);
                        } catch (LinphoneCoreException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        friend.addPhoneNumber(contactNumber);
                }

                friend.done();
            }



            if(isNewFriend) {
                Log.d("vcard_sync adding friend to list for export : " + name);
                list.addFriend(friend);
            }


        }
        sipCusrsor.close();

    }

    private static void deleteNotExistingContacts(LinphoneFriendList list, ContentResolver cr)
    {
        LinphoneFriend[] friends = list.getFriendList();
        ArrayList<LinphoneFriend> friends_to_delete = new ArrayList<LinphoneFriend>();
        if(friends!=null) {
            for (LinphoneFriend friend : friends) {
                Contact contact = null;
                if(friend.getRefKey() != null) {
                    contact = ContactsManager.getInstance().getContact(friend.getRefKey(), cr);
                    if(contact == null)// check for raw contact id
                    {
                        String contactId = null;
                        Cursor c = cr.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{ContactsContract.RawContacts.CONTACT_ID}, ContactsContract.RawContacts._ID +"=?", new String[]{friend.getRefKey()+""}, null );
                        if(c.moveToNext())
                            contactId = c.getString(0);
                        c.close();
                        if(contactId != null)
                            contact = ContactsManager.getInstance().getContact(contactId, cr);
                    }
                }

                if(contact == null)
                    friends_to_delete.add(friend);
                else if(!contact.getID().equals(friend.getRefKey()))
                {
                    friend.setRefKey(contact.getID());
                }
            }

            for (LinphoneFriend delete_friend : friends_to_delete)
            {
                Log.d("vcard_sync deleting contact export: " + delete_friend.getName());
                LinphoneManager.getLc().removeFriend(delete_friend);
            }
        }
    }
    private static LinphoneFriend getFriendById(String id, LinphoneFriendList list)
    {
        LinphoneFriend res = null;
        LinphoneFriend[] friends = list.getFriendList();
        if(friends!=null)
        {
            for (LinphoneFriend friend : friends) {
                if (friend.getRefKey().equals(id)) {
                    res = friend;
                    break;
                }
            }
        }
        return res;
    }




    public static void addContact(Context context, LinphoneFriend friend)
    {

        Log.d("vcard_sync adding contact to contact list" + friend.getName());

        String name = friend.getName();
        ArrayList<String> phoneNumbers = new ArrayList<String>();
        ArrayList<String> sipNumbers = new ArrayList<String>();

        LinphoneAddress[] addresses = friend.getAddresses();
        String mainAddress = friend.getAddress().asStringUriOnly();

        if (mainAddress.startsWith("sip:")) {
            mainAddress = mainAddress.substring(4);
        }
      //  sipNumbers.add(mainAddress);
        for (LinphoneAddress address : addresses) {
            String sipaddress = address.asStringUriOnly();
            if (sipaddress.startsWith("sip:")) {
                sipaddress = sipaddress.substring(4);
            }
            sipNumbers.add(sipaddress);

        }



        String[] friendNumbers = friend.getPhoneNumbers();
        if(friendNumbers != null) {
            for (String friendNumber : friendNumbers) {
                if (LinphoneUtils.isSipAddress(friendNumber)) {

                    if (friendNumber.startsWith("sip:")) {
                        friendNumber = friendNumber.substring(4);
                    }//vcard_sync
                    sipNumbers.add(friendNumber);

                } else {
                    phoneNumbers.add(friendNumber);
                }
            }
        }



        //list.toArray(new String[0]);
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Compatibility.createContact(name, phoneNumbers.toArray(new String[0]), sipNumbers.toArray(new String[0]), ops, context);

        try {
            ContentProviderResult[] results =context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            int contactId = Integer.parseInt(results[0].uri.getLastPathSegment());

            friend.setRefKey(String.valueOf(contactId));



        } catch (RemoteException e) {
            e.printStackTrace();
            org.linphone.mediastream.Log.e("vcard_sync " + "updating failed");
        } catch (OperationApplicationException e) {
            org.linphone.mediastream.Log.e("vcard_sync " + "updating failed");
            e.printStackTrace();
        }

    }

    public static void updateContact(Context context, LinphoneFriend oldFriend, LinphoneFriend newFriend)
    {
        String rfKey = oldFriend.getRefKey();
        int contactID = Integer.parseInt(rfKey);
        int rawContactid = Integer.parseInt(ContactsManager.getInstance().findRawContactID(context.getContentResolver(), String.valueOf(contactID)));

        String name = newFriend.getName();
        ArrayList<String> phoneNumbers = new ArrayList<String>();
        ArrayList<String> sipNumbers = new ArrayList<String>();

        LinphoneAddress[] addresses = newFriend.getAddresses();
        for (LinphoneAddress address : addresses) {
            String sipaddress = address.asStringUriOnly();
            if (sipaddress.startsWith("sip:")) {
                sipaddress = sipaddress.substring(4);
            }
            sipNumbers.add(sipaddress);

        }


        String[] friendNumbers = newFriend.getPhoneNumbers();
        if(friendNumbers != null) {
            for (String friendNumber : friendNumbers) {
                if (LinphoneUtils.isSipAddress(friendNumber)) {

                    if (friendNumber.startsWith("sip:")) {
                        friendNumber = friendNumber.substring(4);
                    }
                    sipNumbers.add(friendNumber);

                } else {
                    phoneNumbers.add(friendNumber);
                }
            }
        }



        //list.toArray(new String[0]);
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Compatibility.updateExistingContact(contactID, rawContactid, name, phoneNumbers.toArray(new String[0]), sipNumbers.toArray(new String[0])
                , ops , context);

        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
            org.linphone.mediastream.Log.e("vcard_sync " + "updating failed");
        } catch (OperationApplicationException e) {
            org.linphone.mediastream.Log.e("vcard_sync " + "updating failed");
            e.printStackTrace();
        }
    }

    public static void deleteContact(Context context, LinphoneFriend friend)
    {
        Log.d("vcard_sync remvoing contact from phone" + friend.getName());
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Compatibility.removeContact(friend.getRefKey(), ops);

        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }



}

