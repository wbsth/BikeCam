package com.micung.bikecam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DeviceCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BIKECAM";

    Bluetooth bluetooth;
    TextView btStatusText;
    Button btStatusButton;
    Button btRefreshButton;

    TextView deviceName;
    TextView deviceStatus;
    View deviceCard;
    View deviceCardTitle;
    Button deviceCardConnectButton;
    Button deviceCardDetectionButton;
    Button deviceCardPairButton;

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
        deviceCardDetectionButton = findViewById(R.id.deviceInfoDetectionButton);
        deviceCardPairButton = findViewById(R.id.deviceInfoPairButton);

        btStatusButton.setOnClickListener(btStatusButtonListener);
        btRefreshButton.setOnClickListener(btRefreshButtonListener);
        deviceCardPairButton.setOnClickListener(deviceCardPairButtonListener);

        deviceList = new ArrayList<BluetoothDevice>();

        deviceListRecycler = findViewById(R.id.deviceRecyclerView);
        deviceListRecycler.setLayoutManager(new LinearLayoutManager(this));
        deviceListRecycler.setItemAnimator(new DefaultItemAnimator());

        adapter = new deviceAdapter(deviceList, deviceListRecycler);
        deviceListRecycler.setAdapter(adapter);

        interfaceInit();

    }

    @Override
    protected void onStart(){
        super.onStart();
        bluetooth.onStart();
        interfaceInit();
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
        deviceCardConnectButton.setText("Connect");
        Toast.makeText(getApplicationContext(), "New device was choosen", Toast.LENGTH_SHORT).show();

        refreshDeviceInfo();
    }

    private void refreshDeviceInfo(){
        int bondState = btdevice.getBondState();
        String deviceStatusText = "";

        Log.d(TAG, String.valueOf(bondState));

        switch (bondState){
            case 10:
                deviceStatusText = "Not paired";
                deviceCardPairButton.setText("Pair");
                deviceCardPairButton.setEnabled(true);
                deviceCardConnectButton.setText("Connect");
                deviceCardConnectButton.setEnabled(false);
                break;
            case 11:
                deviceStatusText = "Paired";
                deviceCardPairButton.setText("Unpair");
                deviceCardPairButton.setEnabled(true);
                deviceCardConnectButton.setText("Connect");
                deviceCardConnectButton.setEnabled(true);
            case 12:
                deviceStatusText = "Connected";
                deviceCardPairButton.setText("Unpair");
                deviceCardPairButton.setEnabled(true);
                deviceCardConnectButton.setText("Disconnect");
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
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, String message) {

        }

        @Override
        public void onMessage(byte[] message) {

        }

        @Override
        public void onError(int errorCode) {

        }

        @Override
        public void onConnectError(BluetoothDevice device, String message) {

        }
    };

}