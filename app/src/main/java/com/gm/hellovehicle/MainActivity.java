package com.gm.hellovehicle;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gm.android.vehicle.signals.Permissions;
import com.gm.android.vehicle.signals.config.Config;
import com.gm.android.vehicle.signals.motion.Motion;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends Activity implements MainActivityPresenter.View,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private TextView mVin;
    private TextView mSpeed;
    private ProgressBar mRotaryProgress;

    private MainActivityPresenter presenter;

    private final String DEVICE_ADDRESS="98:D3:34:91:0F:3E";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    TextView textView;

    boolean deviceConnected=false;
    boolean found = false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.alc_stat);

        mVin = (TextView) findViewById(R.id.vin);
        mSpeed = (TextView) findViewById(R.id.speed);
        mRotaryProgress = (ProgressBar) findViewById(R.id.rotaryProgress);

        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Device supports bluetooth", Toast.LENGTH_SHORT).show();

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableAdapter, 0);
            }
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices.isEmpty()) {

                Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();

            } else {
                for (BluetoothDevice iterator : bondedDevices) {

                    if (iterator.getAddress().equals(DEVICE_ADDRESS)) //Replace with iterator.getName() if comparing Device names.

                    {

                        device = iterator; //device is an object of type BluetoothDevice

                        found = true;

                        break;

                    }
                }
                if (found) {
                    if (BTconnect()) {
                        textView.append("connected");
                        beginListenForData();
                    }
                }


            }
        }

        presenter = new MainActivityPresenter(this, getApplicationContext());
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    textView.append(string);
                                }
                            });
                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart();
    }

    @Override
    protected void onStop() {
        presenter.onStop();
        super.onStop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return presenter.didHandleEvent(event) || super.dispatchKeyEvent(event);
    }

    @Override
    public void requestPermissions(int requestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{Permissions.getPermission(Config.VIN),
                        Permissions.getPermission(Motion.SPEED)}, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        presenter.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Callbacks from MainActivityPresenter
    @Override
    public void updateDriveMode(String state) {
        Toast.makeText(this, String.format("Current driving mode is: %s", state), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateVin(String vin) {
        mVin.setText(vin);
    }

    @Override
    public void updateSpeed(final String speed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSpeed.setText(speed);
            }
        });
    }

    @Override
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void rotaryClockwise() {
        mRotaryProgress.incrementProgressBy(5);
    }

    @Override
    public void rotaryCounterClockwise() {
        mRotaryProgress.incrementProgressBy(-5);
    }
}