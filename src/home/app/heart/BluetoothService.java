/**
 * 
 */
package home.app.heart;

import home.app.heart.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author Norbert
 * 
 */
public class BluetoothService extends Service {

	private NotificationManager nm;
	private Timer timer = new Timer();
	private Timer endTimer = new Timer();
	private static boolean isRunning = false;
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track
																// of all
																// current
																// registered
																// clients.
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_ECG_DATA = 4;
	static final int MSG_ECG_DATA_DOUBLE = 5;
	static final int MSG_SERVICE_STATE = 6;
	final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target
																		// we
																		// publish
																		// for
																		// clients
																		// to
																		// send
																		// messages
																		// to
																		// IncomingHandler.

	private BluetoothSocket btSocket;
	private InputStream mmInStream;
	private OutputStream mmOutStream;

	private String devName;
	private CharSequence devAddress;
	private FileOutputStream fileOut;
	private File file;
	private FileWriter fileWriter;
	private CharSequence filename;
	private int seconds, minutes, hours, day;
	private int secondsEnd, minutesEnd, hoursEnd, dayEnd;
	private int readErrors = 0;
	private BluetoothAdapter btAdapter;
	private BluetoothDevice btDevice;
	private boolean btIsConnected;
	private boolean connectedToBluetooth;

	@Override
	public IBinder onBind(Intent arg0) {
		return mMessenger.getBinder();
	}

	class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void sendMessageToUI(int data) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				mClients.get(i).send(
						Message.obtain(null, MSG_ECG_DATA, data, 0));

			} catch (RemoteException e) {
				mClients.remove(i);
			}
		}
	}

	private void sendMessageToUI(double data) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				mClients.get(i).send(
						Message.obtain(null, MSG_ECG_DATA_DOUBLE, data));

			} catch (RemoteException e) {
				mClients.remove(i);
			}
		}
	}

	private void sendMessageToUI(String state) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				mClients.get(i).send(
						Message.obtain(null, MSG_SERVICE_STATE, state));

			} catch (RemoteException e) {
				mClients.remove(i);
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("BluetoothService", "Service Started.");
		showNotification();
	}

	private void showNotification() {
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.service_started);
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.bluetooth,
				text, System.currentTimeMillis());
		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, HeartMonitor.class), 0);
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.service_label),
				text, contentIntent);
		// Send the notification.
		// We use a layout id because it is a unique number. We use it later to
		// cancel.
		nm.notify(R.string.service_started, notification);

	}

	public boolean connectBluetooth(String address) {

		btDevice = btAdapter.getRemoteDevice(address);
		try {
			btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
			btAdapter.cancelDiscovery();
			try {
				btSocket.connect();
				btIsConnected = true;
				// bluetoothReadThread = new BluetoothReadThread(btSocket);
			} catch (IOException e) {
				btIsConnected = false;
			}

		} catch (IOException e) {
			btIsConnected = false;
		}
		return btIsConnected;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		isRunning = true;
		devAddress = intent.getCharSequenceExtra("BLUETOOTHADDRESS");
		filename = intent.getCharSequenceExtra("FILENAME");
		secondsEnd = intent.getIntExtra("SECONDS", seconds);
		minutesEnd = intent.getIntExtra("MINUTES", minutes);
		hoursEnd = intent.getIntExtra("HOURS", hours);
		dayEnd = intent.getIntExtra("DAY", day);

		btAdapter = BluetoothAdapter.getDefaultAdapter();

		connectedToBluetooth = connectBluetooth((String) devAddress);
		if (!connectedToBluetooth) {
			Log.i("BluetoothService", "Cannot connect to bluetooth device.");
		} else {
			Log.i("MyService", "Received start id " + startId + ": " + intent);

			File root = Environment.getExternalStorageDirectory();
			try {
				file = new File(root, filename + ".txt");
			} catch (Exception e2) {
				Log.e("File open", "Couldnt create file");
			}

			try {
				fileWriter = new FileWriter(file, true);
			} catch (IOException e) {
				Log.e("File open", "File write Failed.");
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				Log.e("Wait some",
						"Couldnt wait some time before send start command.");
			}
			try {
				writeBluetoothMessage("S");
			} catch (InterruptedException e) {
				Log.e("Bluetooth", "Couldnt send start command to ecg monitor");
			}

			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try {
				tmpIn = btSocket.getInputStream();
				tmpOut = btSocket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				onTimerTick();
			}
		}, 0, 50);
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				onEndTimerTick();

			}
		}, 0, 50000);
		return START_STICKY;
	}

	public void writeBluetoothMessage(String msg) throws InterruptedException {

		if (btSocket != null) {
			try {

				OutputStreamWriter out = new OutputStreamWriter(
						btSocket.getOutputStream());
				out.write(msg);
				out.flush();

				Thread.sleep(10);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// Error
		}
	}

	public static boolean isRunning() {
		return isRunning;
	}

	private void onEndTimerTick() {
		if (connectedToBluetooth) {
			Calendar c = Calendar.getInstance();
			minutes = c.get(Calendar.MINUTE);
			hours = c.get(Calendar.HOUR_OF_DAY);
			day = c.get(Calendar.DAY_OF_MONTH);

			if (minutes == minutesEnd && hours == hoursEnd && day == dayEnd) {
				this.stopSelf();
				Log.e("Serwice stop", "Time of recorging period end");
				if (timer != null) {
					timer.cancel();
				}
				if (endTimer != null) {
					endTimer.cancel();
				}
				try {
					writeBluetoothMessage("P");
				} catch (InterruptedException e) {
					Log.e("ECG stop", "Falied to stop ECG device.");
				}
			}
		} else {
			sendMessageToUI("ERROR");
			stopSelf();
		}

	}

	private void onTimerTick() {
		if (connectedToBluetooth) {
			byte[] buffer = new byte[1024];
			Integer bytes;

			try {
				bytes = mmInStream.read(buffer);
				String read = new String(buffer, 0, bytes);

				String data[] = read.split("\r\n");
				for (int i = 0; i < data.length; i++) {

					if (data[i].length() >= 5 && data[i].contains(".")) {
						Double a;
						try {
							Log.e("Data arrived", data[i]);
							a = Double.parseDouble(data[i]);
							fileWriter.append(a.toString());
							fileWriter.append('\n');
							// sendMessageToUI(a);
							sendMessageToUI(a);
						} catch (NumberFormatException e) {
							Log.e("Data arrived",
									"Fail to converse string to int");
							sendMessageToUI(0);
						}
					}
				}
			} catch (IOException e) {
				Log.e("Data read", "Fail to read data");
				readErrors++;
				if (readErrors > 100) {
					timer.cancel();
					stopSelf();
				}

			}
		}
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		if (timer != null) {
			timer.cancel();
		}
		if (endTimer != null) {
			endTimer.cancel();
		}
		if (connectedToBluetooth) {
			try {
				writeBluetoothMessage("P");
			} catch (InterruptedException e) {
				Log.e("ECG stop", "Falied to stop ECG device.");
			}
			try {
				fileWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e("File save", "Close file Failed.");
			}
		}
		try {
			btSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nm.cancel(R.string.service_started); // Cancel the persistent
												// notification.
		Log.i("MyService", "Service Stopped.");
		isRunning = false;
	}
}
