package home.app.heart;

import home.app.heart.R;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothActivity extends Activity {
	
	private static final int  REQUEST_ENABLE_BT = 0x1;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private BluetoothAdapter btAdapter;
	private BluetoothDevice btDevice;
	private BluetoothSocket btSocket;
	private ArrayAdapter<String> bluetoothDevicesArrayAdapter;
	private boolean success;
	
	//private BluetoothReadThread bluetoothReadThread;
	
	ListView bluetoothDevicesListView;
	Button buttonFindNewDevices;
	Button buttonSendMessage;
	Button buttonRead;
	EditText message;
	TextView incommingMessage;
	TextView incommingfloat;
	TextView btState;
	
	ProgressDialog progressDialog;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);
               
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        
        buttonFindNewDevices = (Button) findViewById(R.id.buttonFinfDevices);
        btState = (TextView) findViewById(R.id.textBluetoothState);
        btState.setText(getString(R.string.none));
	    bluetoothDevicesListView = (ListView) findViewById(R.id.listBluetoothDevices);
	    bluetoothDevicesArrayAdapter = new ArrayAdapter<String>(BluetoothActivity.this, android.R.layout.simple_list_item_1);
	    bluetoothDevicesListView.setAdapter(bluetoothDevicesArrayAdapter);
	    
	    bluetoothDevicesListView.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {	        	
//	        	//runDialog(10,"Connecting");
//	        	btState.setText(getString(R.string.connecting));
//	        	String deviceAddress[] = ((TextView) view).getText().toString().split("\n");
//	        	if(!connectTo(deviceAddress[1])){
//	        		btState.setText(getString(R.string.couldntconnect) + deviceAddress[0] + deviceAddress[1]);
//	        		Toast.makeText(getApplicationContext(), getString(R.string.couldntconnect) + deviceAddress[0] + deviceAddress[1],Toast.LENGTH_LONG).show();
//	        	}
//	        	else{
//	        		btState.setText(getString(R.string.connectedto) + deviceAddress[0] + deviceAddress[1]);
//	        		Toast.makeText(getApplicationContext(), getString(R.string.connectedto) + deviceAddress[0] + deviceAddress[1],Toast.LENGTH_SHORT).show();
//	        		//bluetoothReadThread.start();	
//	                Intent myIntent = new Intent(view.getContext(), Connected.class);
//	                startActivity(myIntent);
//	                //startActivityForResult(myIntent, 0);
//	        	}      
	        }
	      });
      
	    checkBluetooth();
	    
	    buttonFindNewDevices.setOnClickListener(buttonFindNewDevicesOnClickListener);
   
	    // Register the BroadcastReceiver
	    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	    registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy    
	   	        
    }
    
    public void onDestroy(){
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
            	Toast.makeText(getApplicationContext(), getString(R.string.clouldntclose),Toast.LENGTH_SHORT).show();
            }
        }
    	unregisterReceiver(mReceiver);
    	
//    	if(bluetoothReadThread != null)
//    	{
//    		bluetoothReadThread.stop();
//    	}
    	super.onDestroy();
    }
    protected void onResume() {
    	  super.onResume();
    }
    
    private void checkBluetooth(){
        
    	if (btAdapter == null) {
        	Toast.makeText(getBaseContext(), R.string.btnotaval , Toast.LENGTH_LONG).show();
        	buttonFindNewDevices.setEnabled(false);
        	btState.setText(getString(R.string.btnotaval));
        	finish();
            return;
        } else {
        	if (!btAdapter.isEnabled()) {
        	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        	}
        	buttonFindNewDevices.setEnabled(true);
        	btState.setText(getString(R.string.btenabled));
    	    showBondedDevices();	    
        }
    }

    private Button.OnClickListener buttonFindNewDevicesOnClickListener = new Button.OnClickListener(){
    	
	  @Override
	  public void onClick(View arg0) {
	   bluetoothDevicesArrayAdapter.clear();
	   btAdapter.startDiscovery();
	   //setProgressBarIndeterminateVisibility(true);
	   btState.setText(getString(R.string.btsearch));
	}};
	  
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_ENABLE_BT){
			checkBluetooth();
		}
	}
	
    private void showBondedDevices()
    {
    	Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    	// If there are paired devices
    	if (pairedDevices.size() > 0) {
    	    // Loop through paired devices
    	    for (BluetoothDevice device : pairedDevices) {
    	        // Add the name and address to an array adapter to show in a ListView
    	    	bluetoothDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
    	    	bluetoothDevicesArrayAdapter.notifyDataSetChanged();
    	    }
    	}
    }
    
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                bluetoothDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                bluetoothDevicesArrayAdapter.notifyDataSetChanged();
            } 
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                if (bluetoothDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.nonefound).toString();
                    bluetoothDevicesArrayAdapter.add(noDevices);
                    bluetoothDevicesArrayAdapter.notifyDataSetChanged();
                }
                btState.setText(getString(R.string.btsearchend));
            }
        }
    };
    
    public boolean connectTo(String address){
  	
    	btDevice = btAdapter.getRemoteDevice(address);	
    	try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
            btAdapter.cancelDiscovery();
            try {
            	btSocket.connect();
                success = true;
               //bluetoothReadThread = new BluetoothReadThread(btSocket);
            } catch (IOException e){
            	success=false;
            }
            
        } catch (IOException e) {
        	success=false;
        }      
        return success;   
    }
    
    
    public void writeMessage(String msg) throws InterruptedException{

        if(btSocket!=null){
            try {

                OutputStreamWriter out=new OutputStreamWriter(btSocket.getOutputStream());
                out.write(msg);
                out.flush();

                Thread.sleep(100);


            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else{
            //Error
        }
    }

    public int readMessage(){
        int n;
        if(btSocket!=null){
            try {

                InputStreamReader in=new InputStreamReader(btSocket.getInputStream());
                n=in.read();

                return n;


            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return -1;
            }
        }else{
            //Error
            return -1;
        }

    }

    public String readData() {
        String rxString = null;
        try {
            char[] buffer = null;
            // throw an exception if length is not greater then 0
            InputStreamReader inputStream = new InputStreamReader(btSocket.getInputStream());
            int length = inputStream.read();
            if (length==-1) {
                 rxString="Lost connection";
                 return rxString;
  
            } else {
                //if data arrived
                buffer = new char[length];
                length = 0;
                // Assemble data
                while (length != buffer.length) {
                    //read into buffer until the end
                    int ch = inputStream.read(buffer, length, buffer.length - length);
                    if (ch == -1) {
                        rxString = "Lost connection";
                        return rxString;
                    }
                    length += ch;
                }
                //after finishing reading the buffer
               // if (buffer != null) {
                    //make string from buffer
                    rxString = new String(buffer);
                    //System.err.println("Read Data :" + rxString);
               // }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return rxString;
    }
    
//	private void runDialog(final int seconds, String msg)
//	{
//		progressDialog = ProgressDialog.show(this, "Please wait....", msg);
//
//	    	new Thread(new Runnable(){
//	    		public void run(){
//	    			try {
//				                Thread.sleep(seconds * 1000);						
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//	    			progressDialog.dismiss();
//	    		}	    		
//	    	}).start();
//	}
}
