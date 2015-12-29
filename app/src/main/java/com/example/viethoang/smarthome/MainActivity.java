package com.example.viethoang.smarthome;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "bluetooth2";

    public static String textArduino;

    Button[] btnNumber = new Button[10];
    int[] buttonID = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9};
    TextView txtArduino;
    Button btnOpen, btnClose, btnChange;
    private int mode = 2;
    private int lightMode = 0;
    private int[] btnNumberStatus = new int[10];

    Handler h;

    final int RECIEVE_MESSAGE = 1;        // Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Define handle buttons
        btnOpen = (Button) findViewById(R.id.btn_open);
        btnClose = (Button) findViewById(R.id.btn_close);
        btnChange = (Button) findViewById(R.id.btn_change);

        for (int i = 0; i < 10; i++) {
            btnNumber[i] = (Button) findViewById(buttonID[i]);
        }

        txtArduino = (TextView) findViewById(R.id.text_view_display);      // for display the received data from the Arduino

        h = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:                                                   // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                        sb.append(strIncom);                                                // append string
                        int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                        if (endOfLineIndex > 0) {                                            // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                            sb.delete(0, sb.length());                                      // and clear
                            txtArduino.setText(sbprint);
                            /*if (sbprint.equals(R.string.right_pass))
                                txtArduino.setText(R.string.right_text);            // update TextView
                            else if (sbprint.equals(R.string.wrong_pass))
                                txtArduino.setText(R.string.wrong_text);            // update TextView
                            else if (sbprint.equals(R.string.change_pass))
                                txtArduino.setText(R.string.change_text);            // update TextView*/
                            textArduino = sbprint;
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            }

            ;
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        for (int i = 0; i < 10; i++){
            btnNumberStatus[i] = 0;
        }
        // Handle buttons
        for (int i = 0; i < 10; i++) {
            final int index = i;
            final String[] send = {"q", "w", "e", "r", "t", "y", "u", "i"};
            final String strIndex = String.valueOf(i);
            btnNumber[i].setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    txtArduino.setText("");
                    if (lightMode == 0)
                        mConnectedThread.write(strIndex);
                    else if (lightMode == 1) {
                        if (index < 9) {
                            mConnectedThread.write(send[index - 1]);
                            if (btnNumberStatus[index] == 0) {
                                btnNumberStatus[index] = 1;
                                btnNumber[index].setBackgroundColor(getResources().getColor(R.color.pad_numeric_background_color));
                            } else if (btnNumberStatus[index] == 1) {
                                btnNumberStatus[index] = 0;
                                btnNumber[index].setBackgroundColor(getResources().getColor(R.color.switch_color_2));
                            }
                        }
                    }
                }
            });
        }

        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtArduino.setText("");
                clickOpen();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtArduino.setText("");
                clickClose();
            }
        });

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtArduino.setText("");
                clickChange();
            }
        });
    }

    private void clickOpen() {
        mode = 1;
        setBackground(mode);
        // Send data
        mConnectedThread.write("a");
    }

    private void clickClose() {
        mode = 2;
        setBackground(mode);
        // Send data
        mConnectedThread.write("s");
    }

    private void clickChange() {
        mode = 3;
        setBackground(mode);
        // Send data
        mConnectedThread.write("d");
    }

    public void clickLight() {
        if(lightMode == 0) {
            lightMode = 1;
            // Change UI
            btnOpen.setBackgroundColor(getResources().getColor(R.color.ripple_color));
            btnClose.setBackgroundColor(getResources().getColor(R.color.ripple_color));
            btnChange.setBackgroundColor(getResources().getColor(R.color.ripple_color));
            btnNumber[0].setBackgroundColor(getResources().getColor(R.color.ripple_color));
            btnNumber[9].setBackgroundColor(getResources().getColor(R.color.ripple_color));
            btnOpen.setText("");
            btnClose.setText("");
            btnChange.setText("");
            btnNumber[0].setText("");
            btnNumber[9].setText("");
            btnOpen.setClickable(false);
            btnClose.setClickable(false);
            btnChange.setClickable(false);
            btnNumber[0].setClickable(false);
            btnNumber[9].setClickable(false);
            for (int i = 1; i < 9; i++)
            {
                if (btnNumberStatus[i] == 0)
                    btnNumber[i].setBackgroundColor(getResources().getColor(R.color.switch_color_2));
                else if (btnNumberStatus[i] == 1)
                    btnNumber[i].setBackgroundColor(getResources().getColor(R.color.pad_numeric_background_color));
            }
        } else if (lightMode == 1){
            lightMode = 0;
            setBackground(mode);
            btnNumber[0].setBackgroundColor(getResources().getColor(R.color.pad_numeric_background_color));
            btnNumber[9].setBackgroundColor(getResources().getColor(R.color.pad_numeric_background_color));
            btnOpen.setText(getResources().getString(R.string.open));
            btnClose.setText(getResources().getString(R.string.close));
            btnChange.setText(getResources().getString(R.string.change));
            btnNumber[0].setText(getResources().getString(R.string.number0));
            btnNumber[9].setText(getResources().getString(R.string.number9));
            btnOpen.setClickable(true);
            btnClose.setClickable(true);
            btnChange.setClickable(true);
            btnNumber[0].setClickable(true);
            btnNumber[9].setClickable(true);
            for (int i = 1; i < 9; i++)
            {
                btnNumber[i].setBackgroundColor(getResources().getColor(R.color.pad_numeric_background_color));
            }
        }
    }

    private void setBackground(int index) {
        btnOpen.setBackgroundColor(getResources().getColor(R.color.open_color));
        btnClose.setBackgroundColor(getResources().getColor(R.color.close_color));
        btnChange.setBackgroundColor(getResources().getColor(R.color.change_color));
        switch (index) {
            case 1:
                btnOpen.setBackgroundColor(getResources().getColor(R.color.ripple_color));
                break;
            case 2:
                btnClose.setBackgroundColor(getResources().getColor(R.color.ripple_color));
                break;
            case 3:
                btnChange.setBackgroundColor(getResources().getColor(R.color.ripple_color));
                break;
            default:
                break;
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean value = false;
        switch (item.getItemId()) {
            case R.id.action_light:
                clickLight();
                value = true;
        }
        return value;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        String address = "20:15:07:27:36:12";
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
            Toast.makeText(getBaseContext(), device.getName() + " connected", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            try {
                btSocket.close();
                Log.d(TAG, "...Socket closed...");
                Toast.makeText(getBaseContext(), "Socket" + device.getName() + " closed", Toast.LENGTH_LONG).show();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
            textArduino = "";
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

}
