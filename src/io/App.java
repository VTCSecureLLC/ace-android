package io;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.LinphoneActivity;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.mediastream.Log;

public class App extends Activity {
    /** Called when the activity is first created. */
	boolean isActive; 
	
	Handler mHandler = new Handler();
	VCardIO mBoundService = null;
	
	int mLastProgress;
	TextView mStatusText = null;
	
	CheckBox mReplaceOnImport = null;

	@Override
	protected void onPause() {
		isActive = false; 

		super.onPause();
	}


	@Override
	protected void onResume() {
		super.onResume();
		
		isActive = true;
		updateProgress(mLastProgress);
	}	
		
	protected void updateProgress(final int progress) {
		// Update the progress bar
		mHandler.post(new Runnable() {
			public void run() {
				if (isActive) {
					setProgress(progress * 100);
					if (progress == 100)
						mStatusText.setText("Done");
				} else {
					mLastProgress = progress;
				}
			}
		});
	}

	void updateStatus(final String status) {
		// Update the progress bar
		mHandler.post(new Runnable() {
			public void run() {
				if (isActive) {
					mStatusText.setText(status);
				}
			}
		});
	}

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request the progress bar to be shown in the title
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setProgress(10000); // Turn it off for now
        
        setContentView(R.layout.vcardio_main);

        Button importButton = (Button) findViewById(R.id.ImportButton);
        Button exportButton = (Button) findViewById(R.id.ExportButton);

    	mStatusText = ((TextView) findViewById(R.id.StatusText));
    	mReplaceOnImport = ((CheckBox) findViewById(R.id.ReplaceOnImport));
    	
    	final Intent app = new Intent(App.this, VCardIO.class);
        OnClickListener listenImport = new OnClickListener() {
    		public void onClick(View v) { 
    	        // Make sure the service is started.  It will continue running
    	        // until someone calls stopService().  The Intent we use to find
    	        // the service explicitly specifies our service component, because
    	        // we want it running in our own process and don't want other
    	        // applications to replace it.
    			
    			if (mBoundService != null) {
    				String fileName = ((EditText) findViewById(R.id.ImportFile)).getText().toString();
    			    // Update the progress bar
    				setProgress(0);
    	            mStatusText.setText("Importing Contacts...");
    	            
    	            // Start the import
					mBoundService.importContactsLinphone(LinphoneManager.getLc(), fileName, App.this);
    	            //mBoundService.doImport(fileName, mReplaceOnImport.isChecked(), App.this);
    			}
    		}
    	};
    	
        OnClickListener listenExport = new OnClickListener() {
    		public void onClick(View v) { 
    	        // Make sure the service is started.  It will continue running
    	        // until someone calls stopService().  The Intent we use to find
    	        // the service explicitly specifies our service component, because
    	        // we want it running in our own process and don't want other
    	        // applications to replace it.
    			Log.d("ListenExport started");
    			if (mBoundService != null) {
    				String fileName = ((EditText) findViewById(R.id.ExportFile)).getText().toString();
    			    // Update the progress bar
    				setProgress(0);
    	            mStatusText.setText("Exporting Contacts...");
    	            
    	            // Start the import
					mBoundService.exportContactsLinphone(LinphoneManager.getLc(), fileName, App.this);
    	            //mBoundService.doExport(fileName, App.this);
    			}
    		}
    	};
    	
    	// Start the service using startService so it won't be stopped when activity is in background.
    	startService(app);
        bindService(app, mConnection, Context.BIND_AUTO_CREATE);
        importButton.setOnClickListener(listenImport);
        exportButton.setOnClickListener(listenExport);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	mBoundService = ((VCardIO.LocalBinder)service).getService();

        	// Tell the user about this for our demo.
            Toast.makeText(App.this, "Connected to VCard IO Service", Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            Toast.makeText(App.this, "Disconnected from VCard IO!", Toast.LENGTH_SHORT).show();
        }
    };
    

}