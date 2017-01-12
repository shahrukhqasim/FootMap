package com.example.footmap;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;


public class MainActivity extends AppCompatActivity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;

    TextView textViewConnectionStatus;

    ImageView imageViewHeatMap;

    public static final String TAG = "MainActivity";

    private void sendData(byte[] data) {
        if(writeCharacteristic ==null)
            return;

        // TODO: Implement this function
        writeCharacteristic.setValue(data);
        mBluetoothLeService.writeCharacteristic(writeCharacteristic);
    }

    private short shortFromBytes(byte firstByte, byte secondByte) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(firstByte);
        bb.put(secondByte);
        return bb.getShort(0);
    }


    private void dataReceived(byte[] data) {
        if(data==null) {
            Log.d(TAG,"Receiving null data");
            return;
        }

        Log.d(TAG,"Data received: "+data.length);

        if(data.length==12) {
            values[0]=shortFromBytes(data[0],data[1]);
            values[1]=shortFromBytes(data[2],data[3]);
            values[2]=shortFromBytes(data[4],data[5]);
            values[3]=shortFromBytes(data[6],data[7]);
            values[4]=shortFromBytes(data[8],data[9]);
            values[5]=shortFromBytes(data[10],data[11]);

            String outString="";
            for(int i=0;i<6;i++) {
                outString+=values[i]+".";
            }
            Log.d(TAG,outString);

            if(myTask!=null) {
                if (myTask.getStatus() == AsyncTask.Status.FINISHED || myTask.getStatus() == AsyncTask.Status.PENDING)
                    myTask.cancel(true);
            }

            myTask=new AsyncTask() {
                Bitmap result;
                @Override
                protected Object doInBackground(Object[] params) {
                    Log.d(TAG,"Generating image task");
                    result=HeatMapGenerator.generateGrid(values,maskingImage);
                    Log.d(TAG,"Generated image task");
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    Log.d(TAG,"On Post Execute setting back" +
                            "");

                    imageViewHeatMap.setImageBitmap(result);
                }
            };

            myTask.execute();

            Log.d(TAG,"Executing new task");


        }
    }


    public void subscribe() {
        if (mGattCharacteristics == null) {
            Log.e(TAG, "Error subscribe null");
            return;
        }

        try {
            // Find out the index opsition
            final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(3).get(1);
            writeCharacteristic = characteristic;
            final BluetoothGattCharacteristic characteristic2 = mGattCharacteristics.get(3).get(0);
            readCharacteristic = characteristic2;
            mBluetoothLeService.setCharacteristicNotification(readCharacteristic, true);

            Log.d(TAG,"Subscribed!");
        }
        catch (Exception e) {
            Log.d(TAG,"Result: "+e.getMessage());
        }
    }

    public void generateHeatMap(View view) {
        byte[]dataSent=new byte[1];
        dataSent[0]='A';
        sendData(dataSent);
    }

    Bitmap maskingImage;

    public static Bitmap scaleBitmap(Bitmap bitmap, int wantedWidth, int wantedHeight) {
        Bitmap output = Bitmap.createBitmap(wantedWidth, wantedHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix m = new Matrix();
        m.setScale((float) wantedWidth / bitmap.getWidth(), (float) wantedHeight / bitmap.getHeight());
        canvas.drawBitmap(bitmap, m, new Paint());

        return output;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BitmapFactory.Options options=new BitmapFactory.Options();
        options.outWidth=371;
        options.outHeight=698;


        maskingImage = BitmapFactory.decodeResource(getResources(), R.raw.foot_image,options);

        maskingImage=scaleBitmap(maskingImage,371,698);


        textViewConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

//        getSupportActionBar().setTitle(mDeviceName);
//        getSupportActionBar()m.setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);



        imageViewHeatMap=(ImageView)findViewById(R.id.imageViewHeatMap);
        //testGenerate();
    }

    void testGenerate() {
        int []z={1023,1023,1023,1023,1023,1023};

        Log.d(TAG,"Generating image");
        Bitmap heatMap=HeatMapGenerator.generateGrid(z,maskingImage);
        imageViewHeatMap.setImageBitmap(heatMap);

        Log.d(TAG,"Generated image");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void handleCharacteristics(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        if (gattServices == null) return;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
            }
            mGattCharacteristics.add(charas);
        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewConnectionStatus.setText(resourceId);
            }
        });
    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: When data is received
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                handleCharacteristics(mBluetoothLeService.getSupportedGattServices());
                // Subscribe to characteristics to receive data
                subscribe();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                dataReceived(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));

            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    int [] values=new int[6];

    AsyncTask myTask;

}
