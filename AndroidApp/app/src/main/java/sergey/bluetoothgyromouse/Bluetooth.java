package sergey.bluetoothgyromouse;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by sergey on 20.11.16.
 */

public class Bluetooth{
    private static final String TAG = "bluetooth1";

    private static final UUID MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private static final UUID BASE_UUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");
    private static final String MACaddress = "18:67:B0:69:D2:AF";

    private BluetoothAdapter adapter= null;
    private BluetoothSocket socket = null;
    private BluetoothDevice device = null;
    private OutputStream stream = null;

    Activity activity = null;

    public Bluetooth(Activity activity){
        adapter = BluetoothAdapter.getDefaultAdapter();
        this.activity = activity;
    }

    public void enable(){
        if (adapter == null){
            printMessage("Device doesn't support bluetooth");
        } else {
            if (!adapter.isEnabled()){
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBT, 0);
            } else {
                printMessage("Already on");
            }
        }
    }

    public void disable(){
        if (adapter.isEnabled()){
            adapter.disable();
            printMessage("Turn off");
        } else {
            printMessage("Already off");
        }
    }

    public void visible(){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        activity.startActivityForResult(getVisible, 0);
        printMessage("Visible off");
    }

    public void connect(){
        device = adapter.getRemoteDevice(MACaddress);

        try{
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e ) {
            Log.e(TAG, String.valueOf(e));
        }

        adapter.cancelDiscovery();

        try {
            socket.connect();
        } catch (IOException e) {
            Log.e(TAG, String.valueOf(e));
            try {
                socket.close();
            } catch (IOException e2) {
                Log.e(TAG, String.valueOf(e2));
            }
        }

        try {
            stream = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, String.valueOf(e));
        }
        printMessage("Successfull connect");
    }

    public void disconnect(){
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, String.valueOf(e));
        }
        printMessage("Disconnect");
    }

    public void discover(){

    }

    public boolean checkState(){
        if (adapter == null)
            return false;
        else
            return adapter.isEnabled();
    }

    public boolean checkConnect(){
        if (socket == null)
            return false;
        else
            return socket.isConnected();
    }

    public String getDeviceData(){
        return device.getName() + "(" + device.getAddress() + ")";
    }

    public void sendCommand(int[] coordinates, int command){
        String msg = Integer.toString(command) + " " +
                Integer.toString(coordinates[0]) + " " +
                Integer.toString(coordinates[1]) + " " +
                Integer.toString(coordinates[2]) + "\n";
        byte[] buffer = msg.getBytes();

        try{
            if (stream != null){
                stream.write(buffer);
            }

        } catch (IOException e) {
            Log.e(TAG, String.valueOf(e));
        }
    }

    private void printMessage(String message){
        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
