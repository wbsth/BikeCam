package com.micung.bikecam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DeviceCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import me.aflak.bluetooth.reader.LineReader;
import me.aflak.ezcam.EZCam;
import me.aflak.ezcam.EZCamCallback;

public class MainActivity extends AppCompatActivity implements EZCamCallback{

    private static final String TAG = "BIKECAM";

    private EZCam cam;
    private SimpleDateFormat dateFormat;

    Bluetooth bluetooth;
    TextView btStatusText;
    Button btStatusButton;
    Button btRefreshButton;

    TextView deviceName;
    TextView deviceStatus;
    View deviceCard;
    View deviceCardTitle;
    Button deviceCardConnectButton;
    Button deviceCardPairButton;

    TextureView textureView;

    BluetoothDevice btdevice;
    ArrayList<BluetoothDevice> deviceList;

    RecyclerView deviceListRecycler;
    deviceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetooth = new Bluetooth(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);
        bluetooth.setDiscoveryCallback(discoveryCallback);
        bluetooth.setDeviceCallback(deviceCallback);

        btStatusText = findViewById(R.id.bt_status_text);
        btStatusButton = findViewById(R.id.bt_switch_button);
        btRefreshButton = findViewById(R.id.bt_refresh_button);

        deviceName = findViewById(R.id.deviceInfoNameText);
        deviceStatus = findViewById(R.id.deviceInfoStatusText);
        deviceCard = findViewById(R.id.deviceInfoCard);
        deviceCardTitle = findViewById(R.id.deviceInfoHeader);
        deviceCardConnectButton = findViewById(R.id.deviceInfoConnectButton);
        deviceCardPairButton = findViewById(R.id.deviceInfoPairButton);

        textureView = findViewById(R.id.textureView);

        btStatusButton.setOnClickListener(btStatusButtonListener);
        btRefreshButton.setOnClickListener(btRefreshButtonListener);
        deviceCardPairButton.setOnClickListener(deviceCardPairButtonListener);
        deviceCardConnectButton.setOnClickListener(deviceCardConnectButtonListener);

        deviceList = new ArrayList<>();

        deviceListRecycler = findViewById(R.id.deviceRecyclerView);
        deviceListRecycler.setLayoutManager(new LinearLayoutManager(this));
        deviceListRecycler.setItemAnimator(new DefaultItemAnimator());

        adapter = new deviceAdapter(deviceList, deviceListRecycler);
        deviceListRecycler.setAdapter(adapter);


        interfaceInit();

        cameraInit();

    }

    @Override
    protected void onStart(){
        super.onStart();
        bluetooth.onStart();
        bluetooth.setReader(DelimiterReader.class);
        interfaceInit();
    }

    private void cameraInit(){
        cam = new EZCam(this);
        cam.setCameraCallback(this);
        String id = cam.getCamerasList().get(CameraCharacteristics.LENS_FACING_BACK);
        cam.selectCamera(id);
        Dexter.withActivity(MainActivity.this).withPermission(Manifest.permission.CAMERA).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                cam.open(CameraDevice.TEMPLATE_PREVIEW, textureView);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                Log.e(TAG, "permission denied");
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();
    }

    @Override
    public void onCameraReady() {
        cam.setCaptureSetting(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
        cam.startPreview();
    }

    @Override
    public void onPicture(Image image) {
        //cam.stopPreview();
        try {
            dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
            String filename = "image_"+dateFormat.format(new Date())+".jpg";
            Log.d("TESTY", String.valueOf(getExternalFilesDir(null)));
            File file = new File(getExternalFilesDir(null), filename);

            EZCam.saveImage(image, file);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onCameraDisconnected() {
        Log.e(TAG, "Camera disconnected");
    }

    @Override
    public void onError(String message) {
        Log.e(TAG, message);
    }

    @Override
    protected void onDestroy() {
        cam.close();
        super.onDestroy();
    }

    private void interfaceInit(){
        if(bluetooth.isEnabled()){
            btStatusText.setText(R.string.bt_status_on);
            btStatusText.setTextColor(Color.GREEN);
            btRefreshButton.setEnabled(true);
            btStatusButton.setText(R.string.bt_switch_off_button);
        }
        else {
            btStatusText.setText(R.string.bt_status_off);
            btStatusText.setTextColor(Color.RED);
            btRefreshButton.setEnabled(false);
            btStatusButton.setText(R.string.bt_switch_on_button);
        }
        btStatusButton.setEnabled(true);
    }

    private void fillDeviceInfo(){
        deviceCard.setVisibility(View.VISIBLE);
        deviceCardTitle.setVisibility(View.VISIBLE);

        deviceName.setText(btdevice.getName());
        Toast.makeText(getApplicationContext(), "New device was choosen", Toast.LENGTH_SHORT).show();

        refreshDeviceInfo();
    }

    private void refreshDeviceInfo(){
        int bondState = btdevice.getBondState();
        boolean isConnected = bluetooth.isConnected();
        String deviceStatusText = "";
        if(isConnected)
            bluetooth.disconnect();

        switch (bondState){
            case 10:
                deviceStatusText = "Not paired";
                deviceCardPairButton.setText(R.string.deviceCardPairButtonTextPair);
                deviceCardPairButton.setEnabled(true);
                deviceCardConnectButton.setText(R.string.deviceCardConnectButtonConnect);
                deviceCardConnectButton.setEnabled(false);
                break;
            case 11:
                deviceStatusText = "Pairing in progress...";
                deviceCardPairButton.setText(R.string.deviceCardPairButtonTextUnpair);
                deviceCardPairButton.setEnabled(true);
                deviceCardConnectButton.setText(R.string.deviceCardConnectButtonConnect);
                deviceCardConnectButton.setEnabled(true);
                break;
            case 12:
                deviceStatusText = "Paired";
                deviceCardPairButton.setText(R.string.deviceCardPairButtonTextUnpair);
                deviceCardPairButton.setEnabled(true);
                deviceCardConnectButton.setText(R.string.deviceCardConnectButtonConnect);
                deviceCardConnectButton.setEnabled(true);
                break;
        }
        deviceStatus.setText(deviceStatusText);
        Log.d("TESTY", deviceStatusText);
    }

    private void clearDeviceInfo(){
        deviceCard.setVisibility(View.INVISIBLE);
        deviceCardTitle.setVisibility(View.INVISIBLE);
        btdevice = null;
    }

    public View.OnClickListener deviceCardPairButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int state = btdevice.getBondState();
            if(state == 10){
                // device is not paired
                bluetooth.pair(btdevice);
            }
            else{
                // device is paired
                bluetooth.unpair(btdevice);
            }
        }
    };

    public View.OnClickListener deviceCardConnectButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(bluetooth.isConnected()){
                bluetooth.disconnect();
            }
            else{
                bluetooth.connectToDevice(btdevice);
            }
        }
    };



    public View.OnClickListener btStatusButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(bluetooth.isEnabled())
                bluetooth.disable();
            else
                bluetooth.enable();
        }
    };

    public View.OnClickListener btRefreshButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "startScanning pressed");
            try{
                bluetooth.startScanning();
                deviceList.clear();
                clearDeviceInfo();
                adapter.notifyDataSetChanged();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    public class deviceAdapter extends RecyclerView.Adapter<deviceAdapter.ViewHolder>{

        private List<BluetoothDevice> btDevices;
        private RecyclerView btRecyclerView;

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView nameTextView;
            private TextView addressTextView;
            private Button chooseButton;
            private BluetoothDevice device;

            public ViewHolder(View view) {
                super(view);
                nameTextView = view.findViewById(R.id.bt_device_name);
                addressTextView = view.findViewById(R.id.bt_device_address);
                chooseButton = view.findViewById(R.id.bt_device_choose_button);
                chooseButton.setOnClickListener((v -> {
                    btdevice = device;
                    fillDeviceInfo();
                    Log.d("TESTY", device.getName());
                }));
            }
        }

        public deviceAdapter(List<BluetoothDevice> btDev, RecyclerView tempRecyclerView) {
            btDevices = btDev;
            btRecyclerView = tempRecyclerView;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // inflate custom lay
            View deviceView = inflater.inflate(R.layout.device_list, parent, false);

            return new ViewHolder(deviceView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDevice btDevice = btDevices.get(position);

            TextView nameTextView = holder.nameTextView;
            TextView addressTextView = holder.addressTextView;
            holder.device = btDevice;

            if(btDevice.getName()!=null)
                nameTextView.setText(btDevice.getName());
            else
                nameTextView.setText("unknown");
            addressTextView.setText(btDevice.getAddress());
        }

        @Override
        public int getItemCount() {
            return btDevices.size();
        }
    }

    private BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override public void onBluetoothTurningOn() {
            btStatusText.setText(R.string.bt_status_turning_on);
            btStatusButton.setEnabled(false);
        }
        @Override public void onBluetoothTurningOff() {
            btStatusText.setText(R.string.bt_status_turning_off);
            btStatusButton.setEnabled(false);
        }
        @Override public void onBluetoothOff() {
            Log.i(TAG, "BT off");
            deviceList.clear();
            adapter.notifyDataSetChanged();
            interfaceInit();
        }

        @Override
        public void onBluetoothOn() {
            Log.i(TAG, "BT on");
            interfaceInit();
        }

        @Override
        public void onUserDeniedActivation() {
            // handle activation denial...
        }
    };

    private DiscoveryCallback discoveryCallback = new DiscoveryCallback() {
        @Override
        public void onDiscoveryStarted() {
            Log.i(TAG, "Discovery started");
            btRefreshButton.setText(R.string.bt_refresh_in_progress);
            btRefreshButton.setEnabled(false);
        }

        @Override
        public void onDiscoveryFinished() {
            Log.i(TAG, "Discovery finished");
            Toast.makeText(getApplicationContext(), "Device scan finished", Toast.LENGTH_SHORT).show();
            btRefreshButton.setEnabled(true);
            btRefreshButton.setText(R.string.bt_refresh_button);
        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {
            deviceList.add(device);
            adapter.notifyItemInserted(deviceList.size()-1);
            Log.i(TAG, "Device found");
            if(device.getName() != null)
                Log.i(TAG, device.getName());
        }

        @Override
        public void onDevicePaired(BluetoothDevice device) {
            refreshDeviceInfo();
            Log.i(TAG, "device paired");
        }

        @Override
        public void onDeviceUnpaired(BluetoothDevice device) {
            refreshDeviceInfo();
            Log.i(TAG, "device unpaired");
        }

        @Override
        public void onError(int errorCode) {
            Log.e(TAG, String.format("%d", errorCode));
        }
    };

    private DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onDeviceConnected(BluetoothDevice device) {
            Log.d("TESTY", "DEVICE CONNECTED");
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Device connected", Toast.LENGTH_SHORT).show();
                }
            });
            deviceCardConnectButton.setText(R.string.deviceCardConnectButtonTextDisconnect);
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {
            Log.d("TESTY", "DEVICE DISCONNECTED");
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Device disconnected", Toast.LENGTH_SHORT).show();
                }
            });
            deviceCardConnectButton.setText(R.string.deviceCardConnectButtonConnect);
        }

        @Override
        public void onMessage(byte[] message) {
            cam.takePicture();
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Photo taken!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onError(int errorCode) {
            Log.d("TESTY", String.valueOf(errorCode));
        }

        @Override
        public void onConnectError(BluetoothDevice device, String message) {

        }
    };

}