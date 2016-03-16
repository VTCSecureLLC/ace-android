package io;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import org.linphone.ContactsManager;
import org.linphone.R;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class VCardIO extends Service {

	private boolean isRunning = false;
	static final String DATABASE_NAME = "syncdata.db";
	static final String SYNCDATA_TABLE_NAME = "sync";
	static final String PERSONID = "person";
	static final String SYNCID = "syncid";
	private static final int DATABASE_VERSION = 1;

	private NotificationManager mNM;

	final Object syncMonitor = "SyncMonitor";
	String syncFileName;
	enum Action {
		IDLE, IMPORT, EXPORT, SYNC
	};


	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + SYNCDATA_TABLE_NAME + " ("
					+ PERSONID + " INTEGER PRIMARY KEY,"
					+ SYNCID + " TEXT UNIQUE"
					+");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// No need to do anything --- this is version 1

		}
	}

	private DatabaseHelper mOpenHelper;

	Action mAction;


	public class LocalBinder extends Binder {
		VCardIO getService() {
			return VCardIO.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	@Override
	public void onCreate() {
		mOpenHelper = new DatabaseHelper(getApplicationContext());

		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		mAction = Action.IDLE;

		// Display a notification about us starting.  We put an icon in the status bar.
		showNotification();
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancelAll();
		synchronized (syncMonitor) {
			switch (mAction) {
				case IMPORT:
					// Tell the user we stopped.
					Toast.makeText(this, "VCard import aborted ("+syncFileName +")", Toast.LENGTH_SHORT).show();
					break;
				case EXPORT:
					// Tell the user we stopped.
					Toast.makeText(this, "VCard export aborted ("+syncFileName +")", Toast.LENGTH_SHORT).show();
					break;
				case SYNC:
					Toast.makeText(this, "VCard sync aborted ", Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
			mAction = Action.IDLE;
		}
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		String text = null;
		String detailedText = "";

		synchronized (syncMonitor) {
			switch (mAction) {
				case IMPORT:
					text = (String) getText(R.string.importServiceMsg);
					detailedText = "Importing VCards from " + syncFileName;
					break;
				case EXPORT:
					text = (String) getText(R.string.exportServiceMsg);
					detailedText = "Exporting VCards from " + syncFileName;
					break;
				case SYNC:
					text = (String) getText(R.string.syncServiceMsg);
					detailedText = "Syncing VCards started";
					break;
				default:
					break;
			}
		}

		if (text == null) {
			mNM.cancelAll();
		} else {

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, App.class), 0);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setSmallIcon(R.drawable.status_icon);
			builder.setContentTitle("VCard IO");
			builder.setContentText(detailedText);
			builder.setContentIntent(contentIntent);


			// Set the icon, scrolling text and timestamp
			Notification notification = builder.build();

			// The PendingIntent to launch our activity if the user selects this notification


			// Set the info for the views that show in the notification panel.

			// Send the notification.
			// We use a layout id because it is a unique number.  We use it later to cancel.
			mNM.notify(R.string.app_name, notification);
		}
	}


	public void doImport(final String fileName, final boolean replace, final App app) {
		try {

			File vcfFile = new File(fileName);

			final BufferedReader vcfBuffer = new BufferedReader(new FileReader(fileName));

			final long maxlen = vcfFile.length();

			// Start lengthy operation in a background thread
			new Thread(new Runnable() {
				public void run() {
					long importStatus = 0;

					synchronized (syncMonitor) {
						mAction = Action.IMPORT;
						syncFileName = fileName;
					}

					showNotification();
					SQLiteDatabase db = mOpenHelper.getWritableDatabase();
					SQLiteStatement querySyncId = db.compileStatement("SELECT " + SYNCID + " FROM " + SYNCDATA_TABLE_NAME + " WHERE " + PERSONID + "=?");
					SQLiteStatement queryPersonId = db.compileStatement("SELECT " + PERSONID + " FROM " + SYNCDATA_TABLE_NAME + " WHERE " + SYNCID + "=?");
					SQLiteStatement insertSyncId = db.compileStatement("INSERT INTO  " + SYNCDATA_TABLE_NAME + " (" + PERSONID + "," + SYNCID + ") VALUES (?,?)");
					Contact parseContact = new Contact(querySyncId, queryPersonId, insertSyncId);
					try {
						long ret = 0;
						do  {
							ret = parseContact.parseVCard(vcfBuffer);
							if (ret >= 0) {
								parseContact.addContact(getApplicationContext(), 0, replace);
								importStatus += parseContact.getParseLen();

								// Update the progress bar
								app.updateProgress((int) (100 * importStatus / maxlen));
							}
						} while (ret > 0);

						db.close();
						app.updateProgress(100);
						synchronized (syncMonitor) {
							mAction = Action.IDLE;
							showNotification();
						}
						stopSelf();
					} catch (IOException e) {
					}
				}
			}).start();


		} catch (FileNotFoundException e) {
			app.updateStatus("File not found: " + e.getMessage());
		}
	}

	public void doExport(final String fileName, final App app) {
		try {
			final BufferedWriter vcfBuffer = new BufferedWriter(new FileWriter(fileName));

			final ContentResolver cResolver = getContentResolver();
			final Cursor allContacts = cResolver.query(Contacts.People.CONTENT_URI, null, null, null, null);
			if (allContacts == null || !allContacts.moveToFirst()) {
				app.updateStatus("No contacts found");
				allContacts.close();
				return;
			}


			final long maxlen = allContacts.getCount();

			// Start lengthy operation in a background thread
			new Thread(new Runnable() {
				public void run() {
					long exportStatus = 0;

					synchronized (syncMonitor) {
						mAction = Action.EXPORT;
						syncFileName = fileName;
					}

					showNotification();
					SQLiteDatabase db = mOpenHelper.getWritableDatabase();
					SQLiteStatement querySyncId = db.compileStatement("SELECT " + SYNCID + " FROM " + SYNCDATA_TABLE_NAME + " WHERE " + PERSONID + "=?");
					SQLiteStatement queryPersonId = db.compileStatement("SELECT " + PERSONID + " FROM " + SYNCDATA_TABLE_NAME + " WHERE " + SYNCID + "=?");
					SQLiteStatement insertSyncId = db.compileStatement("INSERT INTO  " + SYNCDATA_TABLE_NAME + " (" + PERSONID + "," + SYNCID + ") VALUES (?,?)");
					Contact parseContact = new Contact(querySyncId, queryPersonId, insertSyncId);
					try {
						boolean hasNext = true;
						do  {
							parseContact.populate(allContacts, cResolver);
							parseContact.writeVCard(vcfBuffer);

							++exportStatus;

							// Update the progress bar
							app.updateProgress((int) (100 * exportStatus / maxlen));

							hasNext = allContacts.moveToNext();
						} while (hasNext);
						vcfBuffer.close();
						db.close();
						app.updateProgress(100);
						synchronized (syncMonitor) {
							mAction = Action.IDLE;
							showNotification();
						}
						stopSelf();
					} catch (IOException e) {
						app.updateStatus("Write error: " + e.getMessage());
					}
				}
			}).start();
		} catch (IOException e) {
			app.updateStatus("Error opening file: " + e.getMessage());
		}
	}

	public void syncContactsLinphone(final LinphoneCore lc, String username, String password, final String serverUrl, final App app)
	{
		if(isRunning)
		{
			return;
		}
		synchronized (syncMonitor) {
			mAction = Action.SYNC;
		}
		isRunning = true;
		String serverDomain = serverUrl.replace("http://", "").replace("https://", "").split("/")[0]; // We just want the domain name

		LinphoneAuthInfo authInfo = LinphoneCoreFactory.instance().createAuthInfo(username, null, password, "4747ce2517a985f2fc20234a38f068b6", "SabreDAV", serverDomain);
		lc.addAuthInfo(authInfo);

		LinphoneFriendList lfl = getLinphoneFriendsFromContacts(lc);
		if(lfl==null)
		{
			app.updateStatus("Error occurred during sync");
			isRunning = false;
			stopSelf();
		}
		else if(lfl.getFriendList() == null || lfl.getFriendList().length == 0)
		{
			app.updateStatus("No contacts found");
			isRunning = false;
			stopSelf();
		}
		lfl.setUri(serverUrl);
		lfl.setListener(new LinphoneFriendList.LinphoneFriendListListener() {
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

				if (status == LinphoneFriendList.State.SyncStarted) {

				} else if (status == LinphoneFriendList.State.SyncFailure) {
					isRunning = false;
					stopSelf();
				} else if (status == LinphoneFriendList.State.SyncSuccessful) {
					isRunning = false;
					synchronized (syncMonitor) {
						mAction = Action.IDLE;
						showNotification();
					}

					stopSelf();
				}

			}
		});
		lfl.synchronizeFriendsFromServer();
		lc.removeAuthInfo(authInfo);
	}


	public void exportContactsLinphone(final LinphoneCore lc, final String fileName, final App app)
	{
		if(isRunning)
		{
			return;
		}
		isRunning = true;

		new Thread(new Runnable() {
			@Override
			public void run() {

				synchronized (syncMonitor) {
					mAction = Action.EXPORT;
					syncFileName = fileName;
				}

				showNotification();

				LinphoneFriendList lfl = getLinphoneFriendsFromContacts(lc);
				if(lfl==null)
				{
					app.updateStatus("Error occurred during export");
					isRunning = false;
					stopSelf();
				}
				else if(lfl.getFriendList() == null || lfl.getFriendList().length == 0)
				{
					app.updateStatus("No contacts found");
					isRunning = false;
					stopSelf();
				}
				app.updateProgress(50);

				lfl.exportFriendsToVCardFile(fileName);
				app.updateProgress(100);
				synchronized (syncMonitor) {
					mAction = Action.IDLE;
					showNotification();
				}
				isRunning = false;
				//stopSelf();

			}
		}).start();


	}

	void importContactsLinphone(final LinphoneCore lc, final String fileName, final App app) {

		if(isRunning)
		{
			return;
		}

		isRunning =true;

		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (syncMonitor) {
					mAction = Action.IMPORT;
					syncFileName = fileName;
				}

				showNotification();



				LinphoneFriendList list = null;
				try {
					list = lc.createLinphoneFriendList();
				} catch (LinphoneCoreException e) {
					isRunning = false;
					app.updateStatus("Error occurred during export");
					e.printStackTrace();
					return;
				}
				list.importFriendsFromVCardFile(fileName);
				exportFriendListToContacts(list);

				synchronized (syncMonitor) {
					mAction = Action.IDLE;
					showNotification();
				}
				isRunning = false;
				app.updateProgress(100);
				//stopSelf();
			}
		}).start();




	}



	private LinphoneFriendList getLinphoneFriendsFromContacts(LinphoneCore lc)
	{

		Pattern SIP_URI_PATTERN = Pattern.compile(
				"^(sip(?:s)?):(?:[^:]*(?::[^@]*)?@)?([^:@]*)(?::([0-9]*))?$", Pattern.CASE_INSENSITIVE);

		ContactsManager m = ContactsManager.getInstance();
		ContentResolver cr = VCardIO.this.getContentResolver();
		Cursor sipCusrsor = Compatibility.getSIPContactsCursor(cr, null);


		ArrayList<org.linphone.Contact> contacts = new ArrayList<org.linphone.Contact>();
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
				lfl.addLocalFriend(lf);


		}
		return lfl;
	}

	private void exportFriendListToContacts(LinphoneFriendList lfl)
	{
		ContentResolver cr = VCardIO.this.getContentResolver();
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
				ContactsManager.getInstance().createNewContact(ops,friend.getName() );
				Compatibility.addSipAddressToContact(VCardIO.this, ops, friend.getAddress().toString());
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
