package home.app.heart;

import home.app.heart.R;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class HeartMonitor extends Activity {
	
	private static final int REQUEST_ENABLE_BT = 1;
	private String btAddress = "00:12:6F:09:98:1C";//"00:21:4F:B3:65:8A";
	private Messenger mService = null;
	private boolean mIsBound;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private TextView textStatus, textIntValue, textStrValue;
    private GraphView graphView;
    private AlertDialog alertDialog;
    private boolean closeFlag = false;
    private ProgressDialog progressDialog;
    private final String devName = "FizzyFlyOne";
    private boolean ecgVisible = false;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d("Bluetoorh", device.getName());
                Log.d("Bluetoorh", device.getAddress());
                
                if(device.getAddress().equals(btAddress)){
                	progressDialog.dismiss();
                	ecgVisible = true;
                	btAdapter.cancelDiscovery();
                }

            } 
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {                
                progressDialog.dismiss();
                if(!ecgVisible)
                {
                	alertDialog.setMessage("ECG device is not visible!");
                	alertDialog.show();
                }
                btAdapter.cancelDiscovery();
            }
        }
    };
    
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MSG_ECG_DATA:
            	graphView.addPointToGraph(msg.arg1);
            	graphView.invalidate();
                break;
            case BluetoothService.MSG_ECG_DATA_DOUBLE:
            	graphView.addPointToGraph((Double) msg.obj);
            	graphView.invalidate();
            	break;
            case BluetoothService.MSG_SERVICE_STATE:
                stopService(serviceIntent);
            	Toast.makeText(getApplicationContext(), "Cannot connect to ECG device!" + '\n' + "Application will be closed, check bluetooth connection.", Toast.LENGTH_LONG);
            	finish();
            	break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            Log.i("Service", "Connected");
            try {
                Message msg = Message.obtain(null, BluetoothService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Log.i("Service", "Disconnected");
        }
    };
	private BluetoothAdapter btAdapter;
	private ArrayAdapter<String> btDevArrayAdapter;
	private Intent serviceIntent;
    
    private boolean CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (BluetoothService.isRunning()) {
            doBindService();
            return true;
        }
        return false;
    }
    
    void doBindService() {
        bindService(new Intent(this, BluetoothService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.i("Bind service", "Binding");
    }
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, BluetoothService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.i("Bind service", "Unbinding");

        }
    }
    
    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
//            if (mService != null) {
//                try {
//                    Message msg = Message.obtain(null, BluetoothService.MSG_, intvaluetosend, 0);
//                    msg.replyTo = mMessenger;
//                    mService.send(msg);
//                } catch (RemoteException e) {
//                }
//            }
        }
    }
    
    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        TableRow startRow = (TableRow)findViewById(R.id.start_row);
        TableRow settingsRow = (TableRow)findViewById(R.id.settings_row);
        TableRow helpRow = (TableRow)findViewById(R.id.help_row);
        TableRow exitRow = (TableRow)findViewById(R.id.exit_row);
//        startRow.setBackgroundColor(Color.argb(150, 0, 0, 0));
//        settingsRow.setBackgroundColor(Color.argb(150, 0, 0, 0));
//        helpRow.setBackgroundColor(Color.argb(150, 0, 0, 0));
//        exitRow.setBackgroundColor(Color.argb(150, 0, 0, 0));
        startRow.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				setContentView(R.layout.start);
				final EditText nameText = (EditText)findViewById(R.id.editText1);
				final TimePicker timePicker = (TimePicker)findViewById(R.id.timePicker1);
				final DatePicker datePicker = (DatePicker)findViewById(R.id.datePicker1);
				Button startButton = (Button)findViewById(R.id.buttonStart);
				
				startButton.setOnClickListener(new OnClickListener(){				
					@Override
					public void onClick(View v) {					
						if(ecgVisible){
							Calendar c = Calendar.getInstance(); 
							int seconds = c.get(Calendar.SECOND);
							int minutes = c.get(Calendar.MINUTE);
							int hours = c.get(Calendar.HOUR);
							int day = c.get(Calendar.DAY_OF_MONTH);
							
							String name = nameText.getText().toString();
							
							String filename = name + '_' + day + '_' + hours + '_' + minutes + '_' + seconds;
							
							startMyService(filename,0,timePicker.getCurrentMinute(),timePicker.getCurrentHour(),datePicker.getDayOfMonth(),datePicker.getMonth());
							doBindService();
							setContentView(graphView);						
						}
						else{
							alertDialog.setMessage("ECG device is not visible!" + '\n' + "Cannot start application");
							alertDialog.show();
						}
						
					}});				
			 	   btDevArrayAdapter.clear();
			 	   btAdapter.startDiscovery();
			 	   progressDialog = ProgressDialog.show(v.getContext(), "Please wait....", "Looking for ECG device");			 	   
			}
        });
        
        settingsRow.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
			    Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
			    startActivity(intent);
			}
        });
        
        helpRow.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
			    Intent intent = new Intent(getApplicationContext(), HelpActivity.class);
			    startActivity(intent);
			}
        });
        
        exitRow.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(CheckIfServiceIsRunning())
				{
					Toast.makeText(getApplicationContext(), "Registration of ECG signal is running in background.",Toast.LENGTH_SHORT ).show();
				}
				//doUnbindService();
				stopService(new Intent(HeartMonitor.this, BluetoothService.class));
				finish();				
			}
        });
		
        alertDialog = new AlertDialog.Builder(HeartMonitor.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(getString(R.string.btnotaval));
        
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
       
             try {
				closeFlag = true;
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
       
          } });
        
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		btDevArrayAdapter = new ArrayAdapter<String>(getBaseContext(), 0);
	    // Register the BroadcastReceiver
	    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	    registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
	    
	    IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	    registerReceiver(mReceiver, filter2); // Don't forget to unregister during onDestroy

    	if (btAdapter == null) {
    		alertDialog.show();
        } else {
        	if (!btAdapter.isEnabled()) {
        		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);  
        		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        	}   
        }
           
        graphView = new GraphView(this, "ECG Graph", 800);
              
    }
    
    private void startMyService(String filename, int seconds, int minutes, int hours, int day, int month){
    
		serviceIntent = new Intent(HeartMonitor.this, BluetoothService.class);
		serviceIntent.putExtra("BLUETOOTHADDRESS", btAddress);
		serviceIntent.putExtra("FILENAME", filename);
		serviceIntent.putExtra("SECONDS", seconds);
		serviceIntent.putExtra("MINUTES", minutes);
		serviceIntent.putExtra("HOURS", hours);
		serviceIntent.putExtra("DAY", day);
		serviceIntent.putExtra("MONTH", month);
		startService(serviceIntent);
    }
    
    protected void onStart(){
    	super.onStart();     	
    }
    protected void onRestart(){
    	super.onRestart();
        if(CheckIfServiceIsRunning()){
        	// TODO Switch view to graph representation.
        	setContentView(graphView);
        }
    }

    protected void onResume(){
    	super.onResume();
        if(CheckIfServiceIsRunning()){
        	// TODO Switch view to graph representation.
        	setContentView(graphView);
        }
    }

    protected void onPause(){
    	super.onPause();
    }

    protected void onStop(){
    	super.onStop();
    }

    protected void onDestroy(){
    	super.onDestroy();
    	unregisterReceiver(mReceiver);
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e("EKGActivity", "Failed to unbind from the service", t);
        }
    }

}
