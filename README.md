# SmartBin
Tacho de Basura inteligente desarrollado en Arduino + app Android Studio

## Integrantes
* García Pomez Santiago, 38.286.149
* Iglesias Agustín, 41.894.233,
* Messina Gonzalo, 38.130.447
* Morales Maximiliano, 38.176.604
* Salas Sergio, 32.090.753

Martes Noche – Grupo M1

## Resumen 
El Sistema Smart Bin es el proyecto elegido por nuestro grupo para la materia Sistemas Operativos Avanzados. El sistema tiene la capacidad de detectar el nivel de llenado de los residuos con el fin de agilizar el retiro de estos por parte del área de limpieza o individuo, como así también, controlar su mantenimiento desde el celular. 

## Introduccion
Smart Bin es un cesto de basura inteligente que se desarrolla con la necesidad de mantener la higiene en los lugares donde sea instalado. Ya que permite depositar residuos sin tener contacto con el cesto, al mismo tiempo que uno puede visualizar si el mismo tiene espacio para seguir arrojando residuos, sin tener que abrir el mismo. De acuerdo al llenado el mismo, el Cesto puede bloquearse, no permitiendo que se ingrese residuos. Para llevar adelante estas capacidades, se hace uso de sensores  y actuadores, que con una logica aplicada sobre la placa Arduino, logra el objetivo del proyecto. La higiene como objetivo del cesto, tambien tiene la capacidad de detectar perdidas de liquidos que pueden emitir olores no deseados en el ambiente, por consiguiente en este escenario Smart Bin bloquea la tapa para que sea solo retirado manualmente por el area de Limpieza a traves de al aplicación mobile de Android. 

## Desarrollo
Para el funcionamiento del cesto, se empleó un sistema embebido Arduino. Cuando se detecta la presencia de una persona, la tapa se levantará automáticamente para permitir el ingreso de la basura que se arrojará. La tapa permanece en este estado hasta que se vuelva a detectar presencia delante del cesto. Esto se logra mediante un sensor infrarrojo puesto delante del cesto. En el interior del cesto, se utilizó un sensor Ultrasónico para medir el nivel de la basura contenida, un Servo Motor para el movimiento de la tapa; un sensor de humedad que monitorea los niveles de líquidos contenidos dentro. Estas mediciones de nivel de llenado o liquido se realizan siempre con la tapa cerrada. Cuando la distancia medida por el sensor Ultrasónico se reduzca a cierto valor establecido con anterioridad, o el nivel de humedad super el umbral definido, el cesto se bloqueará totalmente y obligara al usuario a realizar un mantenimiento. Cuando esto sucede, el sensor de proximidad que abre la tapa se anulará y solamente se podrá abrir la tapa desde la aplicación Mobile. Una vez abierta la tapa desde la aplicación, se realizará el mantenimiento correspondiente y para finalizarlo se Deberá presionar el botón “finalizar mantenimiento” para que automáticamente se cierre la tapa del cesto. Una vez cerrada la tapa, el Sistema volverá a verificar los valores medidos por los sensores para evaluar si debe Volver a mantenimiento o no.

Para la comunicación con la aplicación Mobile se utilizó un sensor Bluetooth para el intercambio de datos. La aplicación Mobile para Android es el sistema que provee la comunicación del smartphone con el cesto, la que se establece mediante el uso del Bluetooth. La aplicación tiene una interfaz sencilla con el objetivo de que sea fácil de usar para el usuario. Da la bienvenida mediante una pantalla de inicio, luego en el menú principal, permite activar/desactivar el Bluetooth, muestra los dispositivos emparejados al smartphone y permite realizar una búsqueda de dispositivos Bluetooth cercanos. Una vez emparejado con el cesto, se desplegará la pantalla de monitoreo a partir de la cual se podrá observar en tiempo real los siguientes datos:

*	 Tapa abierta (SI/NO)
*	 líquidos en el cesto (SI/NO)
*	 Estado de mantenimiento (SI/NO)
*	 Tacho lleno (SI/NO)
*	 Nivel de llenado


Además, cuando se encuentra en estado de mantenimiento debido a que el tacho está lleno o tiene humedad, se habilitan los botones de iniciar mantenimiento (abre la tapa) y finalizar mantenimiento (Cierra la tapa). 
La aplicación hace uso del sensor acelerómetro del Framework de Android para permitir al usuario activar un modo oscuro en la pantalla de monitoreo. A continuación, se especifica la máquina de estados construida para el Sistema embebido.

### Diagrama de estados
![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/63716dc7-3b54-460f-b61a-3688cc2b5573)

### Diagrama de conexiones
![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/2721f762-e12c-4685-b1eb-5ad69cf0567e)

### Descripcion fisico-electronico de los componentes utilizados
*	Placa de microcontrolador Arduino UNO

Se trata de una placa de microcontrolador de código abierto. Programable. Está basada en el chip ATmega328P y se encuentra equipada con un conjunto de 14 pines de entrada y salida digitales (algunos compatibles con PWM) y 6 pines de entrada analógica. Su alimentación es a través de cable USB o fuente externa.
La placa concentrara la gestión de los sensores, actuadores junto con el sistema embebido. 

![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/d3e27038-6ca0-4628-95bd-8911912ed18d)


* Sensor Ultrasonido HC-SR04

Este sensor tiene 4 pines, dos son de alimentacion, mientras que los otros dos son Trigger y Echo.
Por el pin Trigger el sensor recibe una señal de duracion de 10 micro seg, que produce una transmision de ultrasonido en el transductor electroacustico T, para que luego esta señal, rebote en algun obstaculo dentro de un rango aceptable por el sensor, y sea captado por el transductor electroacustico R. 
Utilizando los conceptos de Física Clásica [Distancia= Vel. x Tiempo] y diviendo dicho calculo por 2, podemos obtener la distancia aproximada del obstaculo.
Este sensor tendrá por finalidad, detectar el llenado del Cesto en su capacidad máxima. 

![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/09e2d18c-c724-4e6d-848a-450b7091825b)

*	Servomotor 

Es un motor electrónico, con similares características a los motores de Corriente Continua convencionales, pero con la capacidad de mantener una posicion que se indique. 
Este actuador será utilizado, para la apertura y cierre de la tapa del Cesto, como así tambien el bloqueo del mismo, cuando la condicion de bloqueo se presente.

![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/01563081-401b-49fd-aaf8-fadf1de2e4e1)

*	Sensor Infrarrojo FC-51

Este sensor se compone de un transmisor que emite infrarrojos IR y un receptor que detecta la energía reflejada por un objeto.
De esta manera detecta la presencia de un objeto mediante el reflejo de esta luz. 
Este sensor se encontrara en el cesto, en su periferia, para detectar el acercamiento de la persona que quiere arrojar residuos en el Cesto.

![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/33812fe5-264e-4aef-9bc2-6fc6af42a613)

*	Sensor de humedad Yl-69

Este sensor tiene dos modos de funcionamiento, dependiendo de si utilizamos el PIN de salida digital o el PIN de salida analógico: la primera forma de funcionamiento utiliza el PIN digital, el cual cambia de estado entre 0 V y 5 V cuando el sensor supera un cierto límite de humedad; la segunda que utiliza el PIN Salida Analógica, donde a través de la variación de la resistencia se puede obtener un proporcional de humedad.
La utilización del mismo permitirá al sistema detectar la existencia de líquidos en los residuos, provocando el bloqueo del Cesto. 

![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/7adb1431-0bcd-4d7c-a5e3-9d1781cebacb)

*	Display LCD 16×2

El display LCD 16×2 es ideal para utilizarse como dispositivo de salida e interfaz de usuario en proyectos con Arduino, Raspberry Pi y otros microcontroladores. Es compatible con el juego de comandos estándar del controlador HD44780 en el que se basan la mayoría de los displays de este tipo y permite visualizar hasta 2 lineas de texto de 16 caracteres cada una.

![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/52d24735-cfb0-4e2a-aaf3-c87baac1c7bd)


Como se mencionó en la introduccion, el proposito del embebido es mantener un control de los cestos de residuos en el espacio donde sea instalado. Para ello, se cuenta con diferentes sensores cuyo objetivo es verificar los estadios de cada uno de los cestos y ejecutar ciertas acciones a partir de los mismos, a saber:
Para el control de apertura del cesto, se utiliza un sensor infrarrojo (FC-51, en tinkercad simulado mediante un pulsador debido a la ausencia de este sensor en la web), dado el caso de que el usuario se acerque lo suficiente como para activar el sensor la tapa da apertura de forma automática, esto a través del servomotor instalado en la misma, una vez el sensor detecta presencia nuevamente el servomotor actua tambien como cierre de tapa.
Aunque debe tenerse en cuenta, que pueden sucederse ciertas condiciones bajo las cuales, a pesar de la prescencia del usuario, la tapa del cesto no da apertura:

* CASO 1 - Nivel del cesto lleno:
 En este caso, mediante el sensor de ultrasonido (HC-SR04), instalado en el interior de la tapa de cada cesto, se mide la capacidad restante de almacenamiento del cesto, en caso de que esta sea igual o menor a 10cm, el embebido toma un estado de bloqueo, en el cual dara apertura unicamente mediante la aplicación android, pero no por el uso habitual del usario al activarse el sensor infrarrojo.

* CASO 2 - Humedad en el cesto:
 Para este caso, se cuenta con un sensor de humedad (Yl-69) al fondo del cesto, para que dado el caso de que la bolsa presente orificios por los cuales se haya filtrado liquido al interior del tacho, se realice un bloqueo de tapa, igual que el caso anterior, donde se apertura bajo la aplicación mobile.
	
* Apertura de tapa en caso de bloqueo:
para poder abrir la tapa en el caso de que esta se encuentre bloqueada (sea por la condicion de cesto lleno o prescencia de humedad), se utilzará una aplicación Android que tendrán instalada los encargados de los cestos o individuos para realizar el mantenimiento de los mismos. Para ello el individuo presiona un boton en la app: “INICIAR MANTENIMIENTO” y la tapa se abre, una vez realizado, presionará “FINALIZAR MANTENIMIENTO” y la tapa se cerrará, el embebido vuelve a verificar los sensores de humedad y ultrasonido, y en caso de que los parametros detectados no sean los adecuados para continuar el bloqueo, el cesto vuelve a funcionar normarlmente y puede volver a darse la apertura de la tapa por parte del usuario (mediante el sensor infrarrojo).

A su vez, el embebido cuenta con un display LCD que brinda informacion del estado actual del cesto: presencia o no de humedad, tacho lleno o no, tapa abierta o cerrada o si se encuentra o no en mantenimiento.
 
Para realizar la apertura por mantenimiento desde tinkercad debe ingresarse la letra ‘a’, posteriormente para dar cierre por mantenimiento se ingresa la letra ‘b’. Estos mismos comandos son los que envia la aplicación Android al embebido cuando se presionan los botones en pantalla.



## Conclusiones

Hemos recorrido los cambios realizados en el proyecto SmartBin para su correcto funcionamiento. Se ha observado que al permitir monitorear los niveles de llenado remotamente, se ahorra el costo de tener que estar constantemente yendo físicamente al tacho para determinar cuando está listo para sacar la basura. Esto mejorar la calidad de vida de las personas, como así también, el hecho de no tener que estar constantemente agachándose para abrir una tapa de un cesto. También comienzan a asomar variables importantes que antes no habían sido observadas con detenimiento para la experiencia de usuario: envío de notificaciones cada vez que se encuentra en mantenimiento y demás ayudas visuales.  
Se deja a entrever también, que existen campos para continuar avanzando sobre una base: la comunicación embebido-aplicación Mobile. Si bien la aplicación utiliza una conexión bluetooth, se tiene la limitación del alcance de señal. Pensando mejoras a futura, se puede implementar una comunicación vía Wifi que permita al usuario monitorear su tacho desde cualquier rincón del mundo. 

## Referencias
•	Banzi, Massimo (24 de marzo de 2009). Getting Started with Arduino (en inglés) (1ª edición). Make Books. p. 128. ISBN 9781449363291.
•	Oxer, jyry; Blemings, Hugh (28 de diciembre de 2009). Practical Arduino: Cool Projects for Open Source Hardware (1ª edición). Apress. p. 500. ISBN 1430224770. 

## ANEXO: MANUAL DE USUARIO
1.	Una vez iniciada la app “SmartBin” se muestra la pantalla de bienvenida

 ![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/934a44f1-7445-4d92-ba8a-19f16ade17f7)

2.	En el menu principal, tiene 3 botones con distintas opciones: Activar/Desactivar el bluetooth, ver dispositivos bluetooth emparejados en el celular, o buscar dispositivos bluetooth.

![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/9eed0bb8-c334-4315-86df-1862c3ef952b)

3.	Cuando se selecciona “dispositivos emparejados” se puede visualizar mediante una lista, los distintos dispositivos conectados junto con su MAC Address. Si su tacho Smartbin se encuentra en la lista y desea monitorearlo, debera presionar “Unpair” y posteriormente “Pair” para volver a emparejarlo y monitorearlo.

 ![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/f41b9378-21d4-4284-8f3f-dffc0c591ef5)

 ![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/bca82876-eb56-468f-9f33-3d5fb8008cd5)

4.	Una vez emparejado, se despliega la pantalla principal de monitoreo. A traves de la misma, podra ver el estado actual del tacho, monitorear en tiempo real las mediciones de los sensores y en caso de que el tacho se encuentre en mantenimiento, se habilitaron los botones de “iniciar manteimiento” y “finalizar mantenimiento”.

 ![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/2770ce4d-c4fe-4f9f-97bc-46d63ba24977)

## ANEXO: Circuito Tinkercard
 
![image](https://github.com/SOAUnlamGrupo1/SmartBin/assets/62450950/f0336df4-99cd-422e-bfb7-c4962e7ff139)

*	Link al proyecto: [SMART BIN - Tinkercad](https://www.tinkercad.com/things/aQk6Tqo42Hm?sharecode=qwME2j9pxT-5vejmroLWObfJKjLj2Rxy_0mhcZdRGXM)





