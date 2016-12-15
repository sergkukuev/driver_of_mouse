package sergey.bluetoothgyromouse;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    //Команды мыши
    private static final int MOUSE_LEFT_BUTTON_PRESS = 1;
    private static final int MOUSE_RIGHT_BUTTON_PRESS = 2;
    private static final int MOUSE_MOVE = 3;
    private static final int MOUSE_DOUBLE_CLICK = 4;
    private static final int DOUBLE_CLICK_TIME_DELTA = 300;

    private static final int CORRECT_X = 100;
    private static final int CORRECT_Y = 200;
    private static final int CORRECT_Z = 10;
    private static final int CORRECT_CMD = 30;

    //Параметры гироскопа
    private SensorManager sensorManager;
    private Sensor sensorGyro;
    private float[] valuesGyro = new float[3];

    private Timer timer;
    private int rotation;
    private static long lastClickTime;

    private Bluetooth bluetooth = null;

    //Параметры интерфейса
    private TextView tvGyroData;
    private TextView tvState;
    private Button btnCLeft, btnCRight;
    private Button btnConnectBT;
    private Button btnDisconnectBT;
    private Button btnBTStat;

    SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            for (int i=0; i < 3; i++){
                valuesGyro[i] = event.values[i];
            }
        }
    };

    View.OnClickListener connectBTClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!bluetooth.checkState()){
                Toast.makeText(getApplicationContext(), "Bluetooth disable", Toast.LENGTH_SHORT).show();
            } else {
                if (!bluetooth.checkConnect()) {
                    bluetooth.connect();
                } else {
                    bluetooth.disconnect();
                    bluetooth.connect();
                }
            }
        }
    };

    View.OnClickListener BtClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!bluetooth.checkState()){
                bluetooth.enable();
            } else {
                bluetooth.disable();
            }
        }
    };

    View.OnClickListener disconnectBTClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bluetooth.checkConnect()){
                bluetooth.disconnect();
                Toast.makeText(getApplicationContext(), "Disconnect", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "No connection", Toast.LENGTH_SHORT).show();

        }
    };

    View.OnClickListener rightMouseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendCommand(MOUSE_RIGHT_BUTTON_PRESS);
        }
    };

    View.OnClickListener leftMouseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendCommand(MOUSE_LEFT_BUTTON_PRESS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        tvGyroData = (TextView) findViewById(R.id.tvGyroData);
        tvState = (TextView) findViewById(R.id.tvState);
        btnConnectBT = (Button) findViewById(R.id.btnConnect);
        btnDisconnectBT = (Button) findViewById(R.id.btnDisconnect);
        btnCLeft = (Button) findViewById(R.id.btnCLeft);
        btnCRight = (Button) findViewById(R.id.btnCRight);
        btnBTStat = (Button) findViewById(R.id.btnBT);

        btnConnectBT.setOnClickListener(connectBTClickListener);
        btnDisconnectBT.setOnClickListener(disconnectBTClickListener);
        btnCLeft.setOnClickListener(leftMouseClickListener);
        btnCRight.setOnClickListener(rightMouseClickListener);
        btnBTStat.setOnClickListener(BtClickListener);

        bluetooth = new Bluetooth(this);
    }

 /*   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }*/

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        long clickTime = System.currentTimeMillis();
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                sendCommand(MOUSE_DOUBLE_CLICK);
            }
        lastClickTime = clickTime;
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(listener, sensorGyro, SensorManager.SENSOR_DELAY_NORMAL);

        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showCoordinates();
                        showBTStatus();
                        if (bluetooth.checkConnect()){
                            if (valuesGyro[1] * CORRECT_Z > CORRECT_CMD){
                                sendCommand(MOUSE_RIGHT_BUTTON_PRESS);
                            } else if (valuesGyro[1] * CORRECT_Z < -CORRECT_CMD){
                                sendCommand(MOUSE_LEFT_BUTTON_PRESS);
                            } else {
                                sendCommand(MOUSE_MOVE);
                            }
                        }
                    }
                });
            }
        };
        timer.schedule(task, 0, 400);

        WindowManager windowManager = ((WindowManager) getSystemService(Context.WINDOW_SERVICE));
        Display display = windowManager.getDefaultDisplay();
        rotation = display.getRotation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(listener);
        timer.cancel();
    }

    private void sendCommand(int cmd){
        if (bluetooth.checkConnect()){
            bluetooth.sendCommand(correctData(valuesGyro), cmd);
        } else {
            Toast.makeText(getApplicationContext(), "No connection", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCoordinates(){
        String info = String.format("%1$.1f\t\t%2$.1f\t\t%3$.1f",
                valuesGyro[0], valuesGyro[1], valuesGyro[2]);
        tvGyroData.setText("Gyroscope data: " + info);
    }

    private void showBTStatus(){
        String status;
        if (bluetooth.checkState()){
            if (bluetooth.checkConnect()){
                status = bluetooth.getDeviceData();
            } else {
                status = "Bluetooth enable";
            }
        } else {
            status = "Bluetooth disable";
        }
        tvState.setText("BT status: " + status);
    }

    private int[] correctData(float[] data)
    {
        int[] result = new int[data.length];
        float[] tmp = data;

        tmp[0] *= CORRECT_X;
        tmp[2] *= CORRECT_Y;
        tmp[1] *= CORRECT_Z;

        for (int i = 0; i < data.length; i++) {
            result[i] = (int) tmp[i];
        }

        return result;
    }
}
