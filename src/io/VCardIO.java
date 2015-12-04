package io;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Contacts;
import android.widget.Toast;

import org.linphone.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VCardIO extends Service {
	static final String DATABASE_NAME = "syncdata.db";
	static final String SYNCDATA_TABLE_NAME = "sync";
	static final String PERSONID = "person";
	static final String SYNCID = "syncid";
    private static final int DATABASE_VERSION = 1;
	
    private NotificationManager mNM;

    final Object syncMonitor = "SyncMonitor";
    String syncFileName;
    enum Action {
    	IDLE, IMPORT, EXPORT
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
            default:
            	break;
        	}
		}
        
        if (text == null) {
        	mNM.cancelAll();
        } else {
        	// Set the icon, scrolling text and timestamp
        	Notification notification = new Notification(R.drawable.status_icon, text,
        			System.currentTimeMillis());

        	// The PendingIntent to launch our activity if the user selects this notification
        	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        			new Intent(this, App.class), 0);

        	// Set the info for the views that show in the notification panel.
        	notification.setLatestEventInfo(this, "VCard IO", detailedText, contentIntent);

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
}
