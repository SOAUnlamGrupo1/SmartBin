package net.londatiga.android.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/*********************************************************************************************************
 * Activity que muestra realiza la comunicacion con Arduino
 **********************************************************************************************************/

//******************************************** Hilo principal del Activity**************************************
public class activity_comunicacion extends Activity implements SensorEventListener
{
    private SensorManager mSensorManager;
    Boolean fondo_original=true;
    Button btnApagar;
    Button btnEncender;
    TextView txtValorLleno, txtValorTapa, txtValorLiquido, txtValorMantenimiento,
            txtValorHumedad, txtValorCapacidad, txtPotenciometro, txtEstadoGeneral;
     ImageView ivStatus;

    Handler bluetoothIn;
    final int handlerState = 0; //used to identify handler message

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service  - Funciona en la mayoria de los dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address del Hc05
    private static String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comunicacion);
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        // Accedemos al servicio de sensores
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Se definen los componentes del layout
        btnApagar=(Button)findViewById(R.id.btnApagar);
        btnEncender=(Button)findViewById(R.id.btnEncender);
        txtValorTapa=(TextView)findViewById(R.id.txtValorTapa);
        txtValorLiquido=(TextView)findViewById(R.id.txtValorLiquido);
        txtValorLleno=(TextView)findViewById(R.id.txtValorLleno);
        txtValorMantenimiento=(TextView)findViewById(R.id.txtValorMantenimiento);
        txtValorHumedad=(TextView)findViewById(R.id.txtValorHumedad);
        txtValorCapacidad=(TextView)findViewById(R.id.txtValorCapacidad);
        ivStatus = findViewById(R.id.ivStatus);
        txtEstadoGeneral=(TextView)findViewById(R.id.txtEstadoGeneral);

        btnEncender.setEnabled(false);
        btnApagar.setEnabled(false);

        //obtengo el adaptador del bluethoot
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //defino el Handler de comunicacion entre el hilo Principal  el secundario.
        //El hilo secundario va a mostrar informacion al layout atraves utilizando indeirectamente a este handler
        bluetoothIn = Handler_Msg_Hilo_Principal();

        //defino los handlers para los botones Apagar y encender
        btnEncender.setOnClickListener(btnEncenderListener);
        btnApagar.setOnClickListener(btnApagarListener);

    }

    @Override
    //Cada vez que se detecta el evento OnResume se establece la comunicacion con el HC05, creando un
    //socketBluethoot
    public void onResume() {
        super.onResume();
        Ini_Sensores();

        //Obtengo el parametro, aplicando un Bundle, que me indica la Mac Adress del HC05
        Intent intent=getIntent();
        Bundle extras=intent.getExtras();

        address= extras.getString("Direccion_Bluethoot");

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        //se realiza la conexion del Bluethoot crea y se conectandose a atraves de un socket
        try
        {
            btSocket = createBluetoothSocket(device);
        }
        catch (IOException e)
        {
            showToast( "La creacción del Socket fallo");
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        }
        catch (IOException e)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e2)
            {
                //insert code to deal with this
            }
        }

        //Una establecida la conexion con el Hc05 se crea el hilo secundario, el cual va a recibir
        // los datos de Arduino atraves del bluethoot
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called

        mConnectedThread.write("x\r");
    }


    @Override
    //Cuando se ejecuta el evento onPause se cierra el socket Bluethoot, para no recibiendo datos
    public void onPause()
    {
        Parar_Sensores();
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    @Override
    protected void onRestart()
    {
        Ini_Sensores();

        super.onRestart();
    }



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

    //Metodo que crea el socket bluethoot
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    //Handler que sirve que permite mostrar datos en el Layout al hilo secundario
    private Handler Handler_Msg_Hilo_Principal ()
    {
        return new Handler() {
            public void handleMessage(android.os.Message msg)
            {
                //si se recibio un msj del hilo secundario
                if (msg.what == handlerState)
                {
                    //voy concatenando el msj
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\r"); //ASI PROBAR CON PUTTY
                    //int endOfLineIndex = recDataString.indexOf("\r\n"); // ASI USAR CON ARDUINO POSTA

                    //cuando recibo toda una linea la muestro en el layout
                    if (endOfLineIndex > 0)
                    {

                        if (recDataString.toString().contains("ABIERTO|")) {
                            String dataInPrint = recDataString.substring(recDataString.indexOf("|")+1, recDataString.indexOf("\r")); //si o NO
                            txtValorTapa.setText(dataInPrint);

                            recDataString.delete(0, recDataString.length());
                        }
                        else if (recDataString.toString().contains("LIQUIDO|")) {
                            String dataInPrint = recDataString.substring(recDataString.indexOf("|")+1, recDataString.indexOf("\r")); //si o NO
                            txtValorLiquido.setText(dataInPrint);

                             recDataString.delete(0, recDataString.length());
                        }
                        else if (recDataString.toString().contains("LLENO|")) {
                            String dataInPrint = recDataString.substring(recDataString.indexOf("|")+1, recDataString.indexOf("\r")); //si o NO
                            txtValorLleno.setText(dataInPrint);

                            recDataString.delete(0, recDataString.length());
                        }
                        else if (recDataString.toString().contains("MANTENIMIENTO|")) {
                            String dataInPrint = recDataString.substring(recDataString.indexOf("|")+1, recDataString.indexOf("\r")); //si o NO
                            txtValorMantenimiento.setText(dataInPrint);

                            recDataString.delete(0, recDataString.length());
                        }
                        else if (recDataString.toString().contains("HUMEDAD|")) {
                            String dataInPrint = recDataString.substring(recDataString.indexOf("|")+1, recDataString.indexOf("\r")); //si o NO
                            txtValorHumedad.setText("HUMEDAD "+dataInPrint+" %");

                            recDataString.delete(0, recDataString.length());
                        }
                        else if (recDataString.toString().contains("CAPACIDAD|")) {
                            String dataInPrint = recDataString.substring(recDataString.indexOf("|")+1, recDataString.indexOf("\r")); //si o NO
                            txtValorCapacidad.setText("CAPACIDAD AL "+dataInPrint+" %");

                             recDataString.delete(0, recDataString.length());
                        }
                        else if (recDataString.toString().contains("INICIAR_MANTENIMIENTO")) { //habilita boton iniciar mantenimiento
                            btnEncender.setEnabled(true);
                            ivStatus.setImageResource(R.drawable.error);
                            txtEstadoGeneral.setText("ESTADO: EN MANTENIMIENTO");
                             recDataString.delete(0, recDataString.length());
                        }
                        else if (recDataString.toString().contains("FINALIZAR_MANTENIMIENTO")) { //habilita boton finalizar mantenimiento
                            //btnEncender.setEnabled(false);
                            btnApagar.setEnabled(true);

                            recDataString.delete(0, recDataString.length());
                        }

                        else {
                            //String dataInPrint = recDataString.substring(0, endOfLineIndex);
                           // txtPotenciometro.setText(dataInPrint);

                            recDataString.delete(0, recDataString.length());
                        }
                    }
                }
            }
        };

    }

    //Listener del boton encender que envia  msj para enceder Led a Arduino atraves del Bluethoot
    private View.OnClickListener btnEncenderListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mConnectedThread.write("a");    // Send "1" via Bluetooth
            showToast("Se abre la tapa");
            btnEncender.setEnabled(false);
            //btnApagar.setEnabled(true);
        }
    };


    //Listener del boton encender que envia  msj para Apagar Led a Arduino atraves del Bluethoot
    private View.OnClickListener btnApagarListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mConnectedThread.write("b");    // Send "0" via Bluetooth
            showToast("Se cierra la tapa");
            btnApagar.setEnabled(false);
            ivStatus.setImageResource(R.drawable.checked);
            txtEstadoGeneral.setText("ESTADO: CORRECTO");
        }
    };


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    // Metodo que escucha el cambio de los sensores


    @Override
    protected void onStop()
    {

        Parar_Sensores();

        super.onStop();
    }
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
                        Toast.makeText(this, "Se ha cambiado el fondo de pantalla", Toast.LENGTH_SHORT).show();
                        if (fondo_original == true) {
                            getWindow().getDecorView().setBackgroundColor(Color.LTGRAY);
                            fondo_original = false;
                        }
                        else
                        {
                            getWindow().getDecorView().setBackgroundColor(Color.WHITE);
                            fondo_original = true;
                        }
                    }
                    break;


            }
        }
    }

    @Override
    protected void onDestroy()
    {
        Parar_Sensores();
        // unregisterReceiver(mReceiver);
        super.onDestroy();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //******************************************** Hilo secundario del Activity**************************************
    //*************************************** recibe los datos enviados por el HC05**********************************

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC05
            while (true)
            {
                try
                {
                    //se leen los datos del Bluethoot
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                     //se muestran en el layout de la activity, utilizando el handler del hilo
                    // principal antes mencionado
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }


        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                showToast("La conexion fallo");
                finish();

            }
        }
    }

}
