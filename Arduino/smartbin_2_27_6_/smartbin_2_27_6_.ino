#include <Servo.h>
#include "rgb_lcd.h"
#include <Wire.h>
#include <SoftwareSerial.h>   // Incluimos la librería  SoftwareSerial  
SoftwareSerial BT(10,11);    // Definimos los pines RX y TX del Arduino conectados al Bluetooth

rgb_lcd lcd;

const int colorR = 255;
const int colorG = 100;
const int colorB = 255;

//----------------------------------------------
//  constantes
#define UMBRAL_CM_A_TAPA_LLENO   10
#define UMBRAL_DIFERENCIA_TIMEOUT   50
#define MAX_CANT_SENSORES   1
#define MAX_CANT_SENSORESH  1
#define SENSOR_PROXIMIDAD_TAPA 10
#define VELOCIDAD_SONIDO_CM_X_SEG 34000.0
#define PRESENCIA_DETECTADA 1
#define PRESENCIA_NO_DETECTADA 0
#define SENSOR_HUMEDAD 0
#define UMBRAL_HUMEDAD 100
// ------------------------------------------------

// ------------------------------------------------
// Pines actuadores (P = PWM | D = Digital)
#define PIN_D_ACTUADOR_SERVO 3
// ------------------------------------------------

// ------------------------------------------------
// Pines sensores (A = analógico | D = Digital)
#define PIN_D_SENSOR_ULTRASONIDO_1_TRIGGER 6
#define PIN_D_SENSOR_ULTRASONIDO_1_ECHO 5
#define PIN_D_SENSOR_INFRARROJO_1 7
#define PIN_A_SENSOR_HUMEDAD A3
// ------------------------------------------------

// ------------------------------------------------
// Posiciones Servo
#define POS_INICIAL 0
#define POS_ABIERTO 85
#define POS_CERRADO -85
// ------------------------------------------------

//----------------------------------------------
// Estado del embeded ...
enum estado_e
{
    ESTADO_EMBEDDED_INIT,
    ESTADO_EMBEDDED_CERRADO,
  ESTADO_EMBEDDED_ABIERTO,
  ESTADO_EMBEDDED_BLOQUEADO,
  ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO
};
//----------------------------------------------

// ------------------------------------------------
// Estados del sensor distancia
enum estado_sensores
{
    ESTADO_SENSOR_OFF,
    ESTADO_SENSOR_ON,
};
//----------------------------------------------


// Tipos de eventos disponibles .
enum evento_e
{
    TIPO_EVENTO_CONTINUE,
    TIPO_EVENTO_TIMEOUT,
    TIPO_EVENTO_TAPA_ABIERTA,
    TIPO_EVENTO_TAPA_CERRADA,
  TIPO_EVENTO_CAPACIDAD_TACHO_LLENO,
  TIPO_EVENTO_ABRIR_MANTENIMIENTO,
  TIPO_EVENTO_CERRAR_MANTENIMIENTO,
    TIPO_EVENTO_HAY_AGUA
  
};
//----------------------------------------------

//----------------------------------------------
struct stSensor
{
  int  trigPin;
  int  echoPin;
  int  estado;
  long distancia;
};
//----------------------------------------------

struct stSensorH
{
  int signal;
  int estado;
  int humedad;
};

//----------------------------------------------
struct stInfrarrojo
{
  int valor;
  int estado;
  int state;
  int valor_antiguo;
};
//----------------------------------------------

//----------------------------------------------
struct stEvento
{
  evento_e tipo;
  int param1;
  int param2;
};
//----------------------------------------------


//----------------------------------------------
// Variables globales y objetos
bool timeout;
long lct;
int humedad = 0;
int lectura = 0;
Servo servo; 
stSensor sensores[MAX_CANT_SENSORES];
stSensorH sensoresH[MAX_CANT_SENSORESH];
estado_e estado;
stEvento evento;
int estadoBluetooth;
stInfrarrojo sensorInfrarrojo;
//LiquidCrystal_I2C lcd(0x20,16,2);
//----------------------------------------------

char leerBluetooth()
{
  if(BT.available()) 
  {
    char comando = BT.read();
    return comando;
  }
  return ' ';


}


long leer_sensor_distancia( int trigPin, int echoPin )
{
  long tiempo_pulso;
  
  pinMode( trigPin, OUTPUT );
  digitalWrite( trigPin, LOW);
  delayMicroseconds(2);
  
  digitalWrite( trigPin, HIGH );
  delayMicroseconds(5);
  digitalWrite( trigPin, LOW );

  pinMode( echoPin, INPUT );
  tiempo_pulso = pulseIn( echoPin, HIGH );

  // Convierto la medicion en centimetros.
  return tiempo_pulso * 0.000001 * VELOCIDAD_SONIDO_CM_X_SEG / 2.0;
}

long leer_sensor_humedad(int humedadPin)
{
  lectura = analogRead(humedadPin);
  humedad = map(lectura, 0, 880, 0, 100);
  return humedad;
}

int leer_valor_boton(int pinSensorInfrarrojo)
{
  return digitalRead(pinSensorInfrarrojo);
}

void mover_servo(int pos)
{
    servo.write(pos);
}

void do_init()
{
  Serial.begin(9600);
    BT.begin(9600);       // Inicializamos el puerto serie BT (Para Modo AT 2)
  //lcd.init();
  //lcd.backlight();
  lcd.begin(16, 2);
  lcd.setRGB(colorR, colorG, colorB);
  
  sensores[SENSOR_PROXIMIDAD_TAPA].trigPin = PIN_D_SENSOR_ULTRASONIDO_1_TRIGGER;
  sensores[SENSOR_PROXIMIDAD_TAPA].echoPin = PIN_D_SENSOR_ULTRASONIDO_1_ECHO;
  sensores[SENSOR_PROXIMIDAD_TAPA].estado = ESTADO_SENSOR_OFF;
  
  sensoresH[SENSOR_HUMEDAD].humedad = humedad;
  sensoresH[SENSOR_HUMEDAD].signal = PIN_A_SENSOR_HUMEDAD;
  sensoresH[SENSOR_HUMEDAD].estado = ESTADO_SENSOR_OFF;
  
  estadoBluetooth = ESTADO_SENSOR_OFF;
  
  servo.attach(PIN_D_ACTUADOR_SERVO); //pin donde conecto el Servo
  servo.write(POS_INICIAL);
  
  sensorInfrarrojo.valor = 0;
  sensorInfrarrojo.state= PRESENCIA_NO_DETECTADA;
  sensorInfrarrojo.valor_antiguo = 0;
  sensorInfrarrojo.estado = ESTADO_SENSOR_OFF;
  
  pinMode(PIN_D_SENSOR_INFRARROJO_1,INPUT);
  
   // Inicializo el evento inicial
  estado = ESTADO_EMBEDDED_INIT;
  timeout = false;
  lct     = millis();
}

void display()
{
  lcd.clear();
  switch(evento.tipo)
  {
    case(TIPO_EVENTO_CONTINUE):
    {
        lcd.setCursor(0, 0);
        lcd.print("AGUA:N ABIERTO:N");
      lcd.setCursor(0, 1);
      lcd.print("MANT:N LLENO:N");
    }
    break;

    case(TIPO_EVENTO_TAPA_ABIERTA):
    {
        lcd.setCursor(0, 0);
        lcd.print("AGUA:N ABIERTO:S");
      lcd.setCursor(0, 1);
      lcd.print("MANT:N LLENO:N");
    }
    break;

    case(TIPO_EVENTO_TAPA_CERRADA):
    {
        lcd.setCursor(0, 0);
            lcd.print("AGUA:N ABIERTO:N");
      lcd.setCursor(0, 1);
            lcd.print("MANT:N LLENO:N");
    }
    break;

    case(TIPO_EVENTO_CAPACIDAD_TACHO_LLENO):
    {
        lcd.setCursor(0, 0);
            lcd.print("AGUA:N ABIERTO:N");
      lcd.setCursor(0, 1);
            lcd.print("MANT:S LLENO:S");
    }
    break;

    case(TIPO_EVENTO_HAY_AGUA):
    {
        lcd.setCursor(0, 0);
            lcd.print("AGUA:S ABIERTO:N");
      lcd.setCursor(0, 1);
            lcd.print("MANT:S LLENO:N");
    }
    break;

    case(TIPO_EVENTO_ABRIR_MANTENIMIENTO):
    {
        lcd.setCursor(0, 0);
        lcd.print("AGUA:N ABIERTO:S");
      lcd.setCursor(0, 1);
      lcd.print("MANT:S LLENO:N");
    }
    break;

    case(TIPO_EVENTO_CERRAR_MANTENIMIENTO):
    {
        lcd.setCursor(0, 0);
        lcd.print("AGUA:N ABIERTO:N");
      lcd.setCursor(0, 1);
      lcd.print("MANT:N LLENO:N");
    }
    break;
  }


}

bool verificarEstadoBluetooth()
{
  char comando = leerBluetooth();

if (sensorInfrarrojo.estado == ESTADO_SENSOR_OFF && (sensores[SENSOR_PROXIMIDAD_TAPA].estado == ESTADO_SENSOR_ON || sensoresH[SENSOR_HUMEDAD].estado == ESTADO_SENSOR_ON))
{
  if (comando == 'a')
  {
    evento.tipo=TIPO_EVENTO_ABRIR_MANTENIMIENTO;
    estadoBluetooth=ESTADO_SENSOR_ON;
    return true;
  }
  else if (comando == 'b')
  {
      evento.tipo=TIPO_EVENTO_CERRAR_MANTENIMIENTO;
      estadoBluetooth=ESTADO_SENSOR_OFF;
      return true;    
  }
}

  return false; 
}

bool verificarEstadoSensorHumedad()
{
  sensoresH[SENSOR_HUMEDAD].humedad = leer_sensor_humedad( sensoresH[SENSOR_HUMEDAD].signal);
  int humedad = sensoresH[SENSOR_HUMEDAD].humedad;  
   
   if (estadoBluetooth==ESTADO_SENSOR_OFF){
    if(sensoresH[SENSOR_HUMEDAD].humedad <= UMBRAL_HUMEDAD
      && sensorInfrarrojo.estado == ESTADO_SENSOR_OFF

      ) //detecta humedad solo con la tapa cerrada
    {
      evento.tipo  = TIPO_EVENTO_HAY_AGUA;
      evento.param1 = SENSOR_HUMEDAD;
            evento.param2 = humedad;
      sensoresH[SENSOR_HUMEDAD].estado = ESTADO_SENSOR_ON;
      estadoBluetooth=ESTADO_SENSOR_ON;
      
      return true;
    }
    else
      sensoresH[SENSOR_HUMEDAD].estado = ESTADO_SENSOR_OFF;
   }
   
  return false;
}


bool verificarEstadoSensorUltrasonidoTapa()
{
  sensores[SENSOR_PROXIMIDAD_TAPA].distancia = leer_sensor_distancia( sensores[SENSOR_PROXIMIDAD_TAPA].trigPin, sensores[SENSOR_PROXIMIDAD_TAPA].echoPin);
  
  int cm = sensores[SENSOR_PROXIMIDAD_TAPA].distancia;
    
if (estadoBluetooth==ESTADO_SENSOR_OFF){
    if( (sensores[SENSOR_PROXIMIDAD_TAPA].distancia <= UMBRAL_CM_A_TAPA_LLENO) 
          && (sensores[SENSOR_PROXIMIDAD_TAPA].distancia >= 0)
    && sensorInfrarrojo.estado == ESTADO_SENSOR_OFF

    ) //tapa cerrada
    {
      evento.tipo  = TIPO_EVENTO_CAPACIDAD_TACHO_LLENO;
      evento.param1 = SENSOR_PROXIMIDAD_TAPA;
            evento.param2 = cm;
      
      sensores[SENSOR_PROXIMIDAD_TAPA].estado = ESTADO_SENSOR_ON;
      estadoBluetooth=ESTADO_SENSOR_ON;
      return true;
    }
      else 
    {
      sensores[SENSOR_PROXIMIDAD_TAPA].estado = ESTADO_SENSOR_OFF;
      evento.tipo  = TIPO_EVENTO_CONTINUE;
      return false;
    }
}
  
  
  return false;
}


bool verificarEstadoInfrarrojoTacho()
{
  sensorInfrarrojo.valor = leer_valor_boton(PIN_D_SENSOR_INFRARROJO_1);
    
  if ( sensorInfrarrojo.estado == ESTADO_SENSOR_OFF ) //tapa cerrada previamente
  {
    if(  sensorInfrarrojo.valor == HIGH && sensorInfrarrojo.valor_antiguo == LOW && sensorInfrarrojo.state == PRESENCIA_NO_DETECTADA 
    &&  sensores[SENSOR_PROXIMIDAD_TAPA].estado == ESTADO_SENSOR_OFF && sensoresH[SENSOR_HUMEDAD].estado == ESTADO_SENSOR_OFF && estadoBluetooth==ESTADO_SENSOR_OFF) // no hay capacidad llena  y no hay humedad
    {
      evento.tipo  = TIPO_EVENTO_TAPA_ABIERTA;
      
      sensorInfrarrojo.estado = ESTADO_SENSOR_ON;
      sensorInfrarrojo.state = PRESENCIA_DETECTADA-sensorInfrarrojo.state;
      sensorInfrarrojo.valor_antiguo = sensorInfrarrojo.valor;
      return true;
    }
    else
      sensorInfrarrojo.valor_antiguo = sensorInfrarrojo.valor;
  } 
    else if( sensorInfrarrojo.estado==ESTADO_SENSOR_ON) //tapa abierta previamente
  {
    if( sensorInfrarrojo.valor == HIGH && sensorInfrarrojo.valor_antiguo == LOW & sensorInfrarrojo.state == PRESENCIA_DETECTADA )
    {
      evento.tipo  = TIPO_EVENTO_TAPA_CERRADA;
      
      sensorInfrarrojo.estado = ESTADO_SENSOR_OFF;
      sensorInfrarrojo.state = PRESENCIA_DETECTADA-sensorInfrarrojo.state;
      sensorInfrarrojo.valor_antiguo = sensorInfrarrojo.valor;
      return true;
    }
    else
      sensorInfrarrojo.valor_antiguo = sensorInfrarrojo.valor;
    
  }
  
  
  return false;

}


void genera_evento( )
{
  long ct = millis();
  int  diferencia = (ct - lct);
  timeout = (diferencia > UMBRAL_DIFERENCIA_TIMEOUT)? (true):(false);

  if( timeout )
  {
    // Doy acuse de la recepcion del timeout
    timeout = false;
    lct   = ct;
    
    if ( 
       (verificarEstadoInfrarrojoTacho() == true) || (verificarEstadoSensorUltrasonidoTapa() == true) || (verificarEstadoSensorHumedad() == true) || (verificarEstadoBluetooth() == true))     
    {
      return;
    }
  }
  
  // Genero evento dummy ....
  evento.tipo = TIPO_EVENTO_CONTINUE;
}


void maquina_estados( )
{
  genera_evento();
  
  switch( estado )
  {
    case ESTADO_EMBEDDED_INIT:
  {
    switch(evento.tipo)
    {
      case TIPO_EVENTO_CONTINUE:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_INIT...");
        Serial.println("Evento TIPO_EVENTO_CONTINUE...");
        Serial.println("-----------------------------------------------------");

        display();
                
        estado = ESTADO_EMBEDDED_CERRADO;
      }
      break;
      
      default:
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_INIT...");
        Serial.println("Evento NO RECONOCIDO...");
        Serial.println(evento.tipo);
        Serial.println("-----------------------------------------------------");
      break;
    }
  }
  break;
  
    case ESTADO_EMBEDDED_CERRADO: 
  {
    switch(evento.tipo)
    {
      case TIPO_EVENTO_TAPA_ABIERTA:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_CERRADO...");
        Serial.println("Evento TIPO_EVENTO_TAPA_ABIERTA...");
        Serial.println("-----------------------------------------------------");
                
        display();
        
        mover_servo(POS_ABIERTO);
        
        estado = ESTADO_EMBEDDED_ABIERTO;
      }
      break;
    
      case TIPO_EVENTO_TAPA_CERRADA:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_CERRADO...");
        Serial.println("Evento TIPO_EVENTO_TAPA_CERRADA...");
        Serial.println("-----------------------------------------------------");

        display();

        estado = ESTADO_EMBEDDED_CERRADO;
      }
      break;
      
      
      case TIPO_EVENTO_CAPACIDAD_TACHO_LLENO:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_CERRADO...");
        Serial.println("Evento TIPO_EVENTO_CAPACIDAD_TACHO_LLENO...");
        Serial.println("-----------------------------------------------------");
        Serial.println("Ingrese 'a' para abrir el tacho y hacer el mantenimiento:");

        display();

        char msj[5];
        char settings_data[100];

        //strcpy(settings_data,"valor sensor ultrasonido (tapa): ");
        //strcat(settings_data, itoa(sensores[SENSOR_PROXIMIDAD_TAPA].distancia,msj,10));
        //strcat(settings_data, "\n");
        
        strcpy(settings_data,"INICIAR_MANTENIMIENTO\r");
        BT.write(settings_data);
        //BT.write(itoa(sensores[SENSOR_PROXIMIDAD_TAPA].distancia,msj,10));

        estado = ESTADO_EMBEDDED_BLOQUEADO;
      }
      break;
          
            case TIPO_EVENTO_HAY_AGUA:
            {
              Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_CERRADO...");
        Serial.println("Evento TIPO_EVENTO_HAY_AGUA...");
        Serial.println("-----------------------------------------------------");

        display();

        Serial.println("Ingrese 'a' para abrir el tacho y hacer el mantenimiento:");
        estado = ESTADO_EMBEDDED_BLOQUEADO;
      }
      break;
          
      
      case TIPO_EVENTO_CONTINUE:
      {
        estado = ESTADO_EMBEDDED_CERRADO;
      }
      break;
      
      case TIPO_EVENTO_TIMEOUT:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_CERRADO...");
        Serial.println("Evento TIPO_EVENTO_TIMEOUT...");
        Serial.println("-----------------------------------------------------");
        
        estado = ESTADO_EMBEDDED_CERRADO;
      }
      break;
      
      default:
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_CERRADO...");
        Serial.println("Evento NO RECONOCIDO...");
        Serial.println(evento.tipo);
        Serial.println("-----------------------------------------------------");
      break;
    }
  } 
  break;
  
   case ESTADO_EMBEDDED_ABIERTO: 
  {
    switch(evento.tipo)
    {
      case TIPO_EVENTO_TAPA_CERRADA:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_ABIERTO...");
        Serial.println("Evento TIPO_EVENTO_TAPA_CERRADA...");
        Serial.println("-----------------------------------------------------");

        display();
        
        mover_servo(POS_CERRADO);
        
        estado = ESTADO_EMBEDDED_CERRADO;
      }
      break;
      
      case TIPO_EVENTO_CONTINUE:
      {       
        estado = ESTADO_EMBEDDED_ABIERTO;
      }
      break;
      
      case TIPO_EVENTO_TIMEOUT:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_ABIERTO...");
        Serial.println("Evento TIPO_EVENTO_TIMEOUT...");
        Serial.println("-----------------------------------------------------");
        
        estado = ESTADO_EMBEDDED_ABIERTO;
      }
      break;
      
      default:
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_ABIERTO...");
        Serial.println("Evento NO RECONOCIDO...");
        Serial.println(evento.tipo);
        Serial.println("-----------------------------------------------------");
      break;
    }
  }
  
  break; 
  
   case ESTADO_EMBEDDED_BLOQUEADO: 
  {
    switch(evento.tipo)
    {
      case TIPO_EVENTO_CONTINUE:
      {       
        estado = ESTADO_EMBEDDED_BLOQUEADO;
      }
      break;
      
      case TIPO_EVENTO_ABRIR_MANTENIMIENTO:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO...");
        Serial.println("Evento TIPO_EVENTO_ABRIR_MANTENIMIENTO...");
        Serial.println("-----------------------------------------------------");

        display();

        Serial.println("Ingrese 'b' para cerrar el tacho y finalizar el mantenimiento:");

        char msj[5];
        char settings_data[100];

        //strcpy(settings_data,"valor sensor ultrasonido (tapa): ");
        //strcat(settings_data, itoa(sensores[SENSOR_PROXIMIDAD_TAPA].distancia,msj,10));
        //strcat(settings_data, "\n");
        
        strcpy(settings_data,"FINALIZAR_MANTENIMIENTO\r");
        BT.write(settings_data);

        mover_servo(POS_ABIERTO);
        estado = ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO;
      }
      break;
      
      case TIPO_EVENTO_TIMEOUT:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_BLOQUEADO...");
        Serial.println("Evento TIPO_EVENTO_TIMEOUT...");
        Serial.println("-----------------------------------------------------");
        
        estado = ESTADO_EMBEDDED_BLOQUEADO;
      }
      break;
      
      default:
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_BLOQUEADO...");
        Serial.println("Evento NO RECONOCIDO...");
        Serial.println(evento.tipo);
        Serial.println("-----------------------------------------------------");
      break;
    }
  }
  
  break; 
  
  
  case ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO: 
  {
    switch(evento.tipo)
    {
      
      case TIPO_EVENTO_CONTINUE:
      {       
        estado = ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO;
      }
      break;      
      
      case TIPO_EVENTO_CERRAR_MANTENIMIENTO:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO...");
        Serial.println("Evento TIPO_EVENTO_CERRAR_MANTENIMIENTO...");
        Serial.println("-----------------------------------------------------");

        display();
        
                mover_servo(POS_CERRADO);
        estado = ESTADO_EMBEDDED_CERRADO;
      }
      break;
      
      case TIPO_EVENTO_TIMEOUT:
      {
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO...");
        Serial.println("Evento TIPO_EVENTO_TIMEOUT...");
        Serial.println("-----------------------------------------------------");
        
        estado = ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO;
      }
      break;
      
      default:
        Serial.println("-----------------------------------------------------");
        Serial.println("Estado ESTADO_EMBEDDED_ABIERTO_MANTENIMIENTO...");
        Serial.println("Evento NO RECONOCIDO...");
        Serial.println(evento.tipo);
        Serial.println("-----------------------------------------------------");
      break;
    }
  }
  
  break; 
  
  }
  
  // Consumo el evento...
  evento.tipo  = TIPO_EVENTO_CONTINUE;
}

void setup()
{
  do_init();
}

void loop()
{       
  maquina_estados();
}
