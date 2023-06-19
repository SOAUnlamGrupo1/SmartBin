package com.example.smartbin_2020_2;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private ImageView ivLogo, ivConexion;
    private Button btnConectar, buttonRealizarMantenimiento;
    private String estado = "desconectado";
    private BluetoothAdapter mBluetoothAdapter;
    private ProgressDialog mProgressDlg;
    public static final int MULTIPLE_PERMISSIONS = 10; // code you want.
    private int conectionAttempts = 3;
    public static Handler bluetoothIN;

    public static ArduinoStatus arduinoStatus = ArduinoStatus.Desconnected; // 0 no, 1 si, 2 estableciendo conexion


    String[] permissions= new String[]{
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        ivLogo = findViewById(R.id.ivLogo);
        ivConexion = findViewById(R.id.ivConexion);
        btnConectar = (Button) findViewById(R.id.btnConectar);
        buttonRealizarMantenimiento = (Button) findViewById(R.id.button);


        checkPermissions();

        // Accedemos al servicio de sensores
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        bluetoothIN = new HandlerMessage(this);

        btnConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getArduinoStatus() == ArduinoStatus.Desconnected) {
                    showDialogConnect();
                }
                else {
                    desconnect();
                }
            }
        });

        buttonRealizarMantenimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    callNextActivity();

            }
        });

    }

    private void checkPermissions(){
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
            );
        } else if (permission2 != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    1
            );
        }
    }

    public void setArduinoStatus(ArduinoStatus arduinoStatus) {
        if(arduinoStatus == ArduinoStatus.Desconnected) {
            runOnUiThread(() -> btnConectar.setText("Conectarse a SmartBin"));
        }
        if (arduinoStatus == ArduinoStatus.Connected) {
            runOnUiThread(() -> btnConectar.setText("Desconectarse de SmartBin"));
        }
        MainActivity.arduinoStatus = arduinoStatus;
    }

    public ArduinoStatus getArduinoStatus() {
        return arduinoStatus;
    }

    private void desconnect() {
        BTHandler.getInstance().sendDesconnect();
    }

    public void showDialogConnect() {


        //primero verifico si el bluetooh esta habilitado, si no lo estoy pide que lo habilites
        if (!BTHandler.getInstance().isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);

        } else
        {
            tryConnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //metodo que cuando recibe que me conecte al bluetooh, trata de conectarse al arduino
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            tryConnect();
        }
    }

    public void tryConnect() {
        setArduinoStatus(ArduinoStatus.AttemptingToConnect);
        btnConectar.setEnabled(false);
        Toast.makeText(this, "Intentando establecer conexión...", Toast.LENGTH_SHORT).show();

        Thread thread = new Thread() {
            @Override
            public void run() {
                //llama al metodo conectar de la clase bluetooh, si se conecta setea el flag de modo de trabajo arduino
                if (BTHandler.getInstance().connect()) {
                    conectionAttempts = 3;
                    setArduinoStatus(ArduinoStatus.Connected);
                    runOnUiThread(new Runnable(){
                        public void run() {
                            btnConectar.setEnabled(true);
                        }
                    });
                } else {
                    conectionAttempts--;
                    setArduinoStatus(ArduinoStatus.Desconnected);
                    runOnUiThread(new Runnable(){
                        public void run() {
                            btnConectar.setEnabled(true);
                            if(conectionAttempts == 0) {
                                showToast("Problemas de conexión con SmartBin. Se cerrará la aplicación...", Toast.LENGTH_LONG);
                                new Handler().postDelayed(new Runnable(){
                                    @Override
                                    public void run(){
                                        finish();
                                    };
                                }, 3000);
                            } else {
                                showToast("Conexión con arduino fallida", Toast.LENGTH_SHORT);
                            }
                        }
                    });
                }
            }
        };
        thread.start();
    }

    public void showToast(String message, int length) {
        Toast.makeText(this, message, length).show();
    }
    /*

    //Metodo que chequea si estan habilitados los permisos
    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        //Se chequea si la version de Android es menor a la 6
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }


        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted.
                    enableComponent(); // Now you call here what ever you want :)
                } else {
                    String perStr = "";
                    for (String per : permissions) {
                        perStr += "\n" + per;
                    }
                    // permissions list of don't granted permission
                    Toast.makeText(this, "ATENCION: La aplicacion no funcionara " +
                            "correctamente debido a la falta de Permisos", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
*/


/*
    public void presionarConectarSmartBin(View v){

        if (estado.equals("desconectado")) {
            ivConexion.setImageResource(R.drawable.connected_icon);
            btnConectar.setText("DESCONECTARSE DE SMARTNBIN");
            Toast.makeText(this, "Conectado", Toast.LENGTH_SHORT).show();
            estado = "conectado";
        }
        else {
            ivConexion.setImageResource(R.drawable.disconnected_icon);
            btnConectar.setText("CONECTARSE A SMARTNBIN");
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show();
            estado = "desconectado";
        }


    }*/

    // Metodo para iniciar el acceso a los sensores
    protected void Ini_Sensores()
    {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),   SensorManager.SENSOR_DELAY_NORMAL);
    }

    // Metodo para parar la escucha de los sensores
    private void Parar_Sensores()
    {

        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    // Metodo que escucha el cambio de sensibilidad de los sensores
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    // Metodo que escucha el cambio de los sensores
    @Override
    public void onSensorChanged(SensorEvent event) {
        String txt = "";

        // Cada sensor puede lanzar un thread que pase por aqui
        // Para asegurarnos ante los accesos simult�neos sincronizamos esto

        synchronized (this) {
            Log.d("sensor", event.sensor.getName());

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if ((event.values[0] > 15) || (event.values[1] > 15) || (event.values[2] > 15)) {
                        Toast.makeText(this, "Vibracion Detectada", Toast.LENGTH_SHORT).show();
                    }
                    break;


            }
        }
    }

    @Override
    protected void onStop()
    {

        Parar_Sensores();

        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        Parar_Sensores();
       // unregisterReceiver(mReceiver);
        super.onDestroy();
        if (getArduinoStatus() == ArduinoStatus.Connected) {
            try {
                BTHandler.getInstance().desconnect();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void onPause()
    {
        Parar_Sensores();
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
        super.onPause();
    }

    @Override
    protected void onRestart()
    {
        Ini_Sensores();

        super.onRestart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Ini_Sensores();
    }

    public void callNextActivity() {

                Intent intent = new Intent(MainActivity.this,
                        com.example.smartbin_2020_2.Mantenimiento.class);
                startActivity(intent);
                finish();

    }

}