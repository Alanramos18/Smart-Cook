// C++ code
//

//BIBLIOTECA PARA MANEJAR EL SERVOMOTOR
#include <Servo.h>
#include <SoftwareSerial.h>


//:::::::::::::::EVENTOS:::::::::::::::::::::::::::::
#define TIPO_EVENTO_CONTINUE 0
#define TIPO_EVENTO_START 1
#define TIPO_EVENTO_TIMEOUT 2
#define TIPO_EVENTO_ABRIR_PUERTA 3
#define TIPO_EVENTO_CERRAR_PUERTA 4
#define TIPO_EVENTO_PRESIONA_PAUSA 5
#define TIPO_EVENTO_CANCELAR 6
#define TIPO_EVENTO_DESCONOCIDO 7
#define TIPO_EVENTO_PRESIONA_REANUDAR 8

//:::::::::::::::ESTADOS:::::::::::::::::::::::::::::
#define ESTADO_INICIAL 0
#define ESTADO_ENCENDIDO 1
#define ESTADO_CALENTANDO 2
#define ESTADO_PAUSADO 3
#define ESTADO_ERROR 4

//:::::::::::::::PINES:::::::::::::::::::::::::::::::
//:::::::::::::::actuadores::::::::::::::::::::::::::
#define PIN_BUZZER 9
#define PIN_LED 13

//:::::::::::::::Melodia::::::::::::::::::::::::::
#define NOTE_C4 262
#define NOTE_G3 196
#define NOTE_A3 220
#define NOTE_B3 247


//::::::::::::::::::MicroServomotor:::::::::::::::::
#define PIN_MICROSERVOMOTOR 10
#define ANCHO_PULSO_MIN_MICROSERVOMOTOR 500
#define ANCHO_PULSO_MAX_MICROSERVOMOTOR 2500
#define PUERTA_ABIERTA 0
#define PUERTA_CERRADA 90
Servo microservomotor;

//:::::::::::::::::Constantes:::::::::::::::
#define PUERTA_BLUETOOTH -1
#define PAUSA_BLUETOOTH -2
#define CANCELAR_BLUETOOTH -3

//:::::::::::::::sensores::::::::::::::::::::::::::::
#define PIN_BOTON 12
#define PIN_POTENCIOMETRO A3

//:::::::::::::::VARIABLES:::::::::::::::::::::::::::
// VARIABLE GLOBAL DE ESTADO DEL SISTEMA EMBEBIDO
int estado;
int pausa = 0;
int tiempo = 0;
int tiempo_restante;
int tipo_evento = PUERTA_CERRADA;
int tipo_evento_temp;
int valor_temp = LOW;
int estado_puerta;
char lecturaConsola;
int timerCalentando;
unsigned long timerCalentandoInicio;
unsigned long timerCalentandoAhora;
int temperaturaTemp = 0;
int temperatura = 0;

int melodia [] = { NOTE_C4, NOTE_G3, NOTE_G3, NOTE_A3, NOTE_G3, 0, NOTE_B3, NOTE_C4 };
int duraciones[] = { 4, 8, 8, 4, 4, 4, 4, 4 };

struct stEvento
{
    int tipo;
    int valor_lectura;
};

stEvento evento;

  
String estados[] = {"ESTADO_INICIO", "ESTADO_ENCENDIDO", "ESTADO_CALENTANDO", "ESTADO_PAUSADO", "ESTADO_ERROR"};
String eventos[] = {"TIPO_EVENTO_CONTINUE", "TIPO_EVENTO_START", "TIPO_EVENTO_TIMEOUT", "TIPO_EVENTO_ABRIR_PUERTA", "TIPO_EVENTO_CERRAR_PUERTA", "TIPO_EVENTO_PRESIONA_PAUSA", "TIPO_EVENTO_CANCELAR", "TIPO_EVENTO_DESCONOCIDO","TIPO_EVENTO_PRESIONA_REAUNDAR"};

SoftwareSerial BT(5,6); // RX, TX recordar que se cruzan

//-----------------------------------------------
int verificarConsola()
{
    if(Serial.available() > 0 && tiempo == 0){ 	 //se verifica si hay un dato en el puerto serial
      iniciarTiempo(Serial.parseInt());

      return tiempo;
    }

    if(BT.available() > 0 && tiempo == 0)
    {
      int action = BT.parseInt();

      // Se llama siempre con 0 luego de un llamado correcto. Ver esto con los profes
      if(action == 0)
      {
        return 0;
      }

      if (action == PUERTA_BLUETOOTH)
      {
        cambiarEstadoPuerta();
      } else {
        iniciarTiempo(action);
        
        return tiempo;
      }
    }
    
  	return 0;
}

void iniciarTiempo(int t)
{
    // Multiplico por 1000 asi lo transformo en milisegundos.
    tiempo = t * 1000;
    timerCalentandoInicio = millis();
    
    // Si la puerta estaba abierta, la cierro automaticamente.
    if(estado_puerta == PUERTA_ABIERTA)
    {
        cambiarEstadoPuerta();
    }

    tipo_evento_temp = TIPO_EVENTO_START;
}

// Verifica si el boton se presiono.
bool verificarBoton()
{
    int valor_actual = digitalRead(PIN_BOTON);
    int res = false;

    if (valor_actual == HIGH && valor_temp == LOW)
    {
        res = true;
    }
    valor_temp = valor_actual;

    return res;
}

// Si la puerta esta abierta la cierra y viceversa.
void cambiarEstadoPuerta()
{
    if(estado_puerta == PUERTA_ABIERTA)
    {
        microservomotor.write(PUERTA_CERRADA);
        tipo_evento_temp = TIPO_EVENTO_CERRAR_PUERTA;
        estado_puerta = PUERTA_CERRADA;
    }
    else
    {
        microservomotor.write(PUERTA_ABIERTA);
        tipo_evento_temp = TIPO_EVENTO_ABRIR_PUERTA;
        estado_puerta = PUERTA_ABIERTA;
    }
}

bool verificarTimerCalentando()
{
    // Tomo el tiempo de ahora y lo resto al milisegundo en el que se ingreso el tiempo de cocina.
    timerCalentandoAhora = millis();
    unsigned long tiempo_temp = timerCalentandoAhora - timerCalentandoInicio; 

    if(tiempo_temp >= tiempo)
    {
        tiempo_restante = 0;
        return true;
    }

    tiempo_restante = tiempo - tiempo_temp; 

    return false;
}

void recalculoTiempo()
{
  // Seteo el tiempo como el tiempo que faltaba y reseteo el tiempo restante
  tiempo = tiempo_restante;
  tiempo_restante = 0;
  timerCalentandoInicio = millis();
}

void generarEvento()
{
    // Verifica si se presiono el boton para abrir/cerrar la puerta
    if(verificarBoton())
    {
        cambiarEstadoPuerta();
    }

    // Setea la temperatura
    temperaturaTemp = analogRead(PIN_POTENCIOMETRO);
    // Consultar porque creo anda mal el potenciometro.
    if(temperatura == 0 || temperatura - 150 > temperaturaTemp || temperatura + 150 < temperaturaTemp)
    {
      temperatura = temperaturaTemp;
      Serial.print("\nLa temperatura es: ");
      Serial.print(temperatura);
      BT.println(temperatura);
    }

    // Comparo si hubo un cambio de evento
    if(tipo_evento_temp != tipo_evento)
    {
        tipo_evento = tipo_evento_temp;
    }
    else
    {
        tipo_evento = TIPO_EVENTO_CONTINUE;
        tipo_evento_temp = TIPO_EVENTO_CONTINUE;
    }
}


void maquinaDeEstados()
{
    generarEvento();

    switch (estado)
    {
        case ESTADO_INICIAL:
        {
            switch (tipo_evento)
            {
                case TIPO_EVENTO_CONTINUE:
                {
                    estado = ESTADO_ENCENDIDO;
                }
                break;
                default:
                {
                    evento.tipo = TIPO_EVENTO_DESCONOCIDO;
                    estado = ESTADO_ERROR;
                }
                break;
            }
        }
        break;

        case ESTADO_ENCENDIDO:
        {
            verificarConsola();

            // Apago el led si no esta calentando.
            digitalWrite(PIN_LED, LOW);
            switch (tipo_evento)
            {
                case TIPO_EVENTO_CONTINUE: case TIPO_EVENTO_ABRIR_PUERTA: case TIPO_EVENTO_CERRAR_PUERTA:
                {
                    estado = ESTADO_ENCENDIDO;
                    tipo_evento = TIPO_EVENTO_CONTINUE;
                }
                break;
                case TIPO_EVENTO_START:
                {            
                    estado = ESTADO_CALENTANDO;
                }
                break;
                default:
                {
                    evento.tipo = TIPO_EVENTO_DESCONOCIDO;
                    estado = ESTADO_ERROR;
                }
                break;
            }
        }
        break;

        case ESTADO_CALENTANDO:
        {
            switch (tipo_evento)
            {
                case TIPO_EVENTO_CONTINUE:
                {
                    if(verificarTimerCalentando())
                    {
                        tipo_evento_temp = TIPO_EVENTO_TIMEOUT;
                    }

                    if(BT.available() > 0)
                    {
                      int action = BT.parseInt();

                      switch(action)
                      {
                        case PUERTA_BLUETOOTH:
                          cambiarEstadoPuerta();
                          break;
                        case PAUSA_BLUETOOTH:
                          tipo_evento_temp = TIPO_EVENTO_PRESIONA_PAUSA;
                          break;
                        case CANCELAR_BLUETOOTH:
                          tipo_evento_temp = TIPO_EVENTO_CANCELAR;
                          break;
                        default:
                          break;
                      }
                    }

                    // Simulo que calienta prendiendo el led.
                    digitalWrite(PIN_LED, HIGH);

                    if(estado_puerta == PUERTA_ABIERTA)
                    {
                        tipo_evento_temp = TIPO_EVENTO_ABRIR_PUERTA;
                    }

                    estado = ESTADO_CALENTANDO;
                }
                break;
                case TIPO_EVENTO_CANCELAR: case TIPO_EVENTO_TIMEOUT:
                {
                    // Suena el buzzer indicando que termino (Preguntar bien esto para que suene bien)
                    tone(PIN_BUZZER, NOTE_C4, 2000);
                    tone(PIN_BUZZER, 0, 2000);
                    tone(PIN_BUZZER, NOTE_C4, 2000);

                    // Abre la puerta cuando termina de calentar
                    microservomotor.write(PUERTA_ABIERTA);
                    estado_puerta = PUERTA_ABIERTA;

                    tiempo = 0;

                    digitalWrite(PIN_LED, LOW);
                    estado = ESTADO_ENCENDIDO;
                }
                break;
                case TIPO_EVENTO_PRESIONA_PAUSA: case TIPO_EVENTO_ABRIR_PUERTA:
                {
                    digitalWrite(PIN_LED, LOW);
                    estado = ESTADO_PAUSADO;
                }
                break;
                default:
                {
                    evento.tipo = TIPO_EVENTO_DESCONOCIDO;
                    digitalWrite(PIN_LED, LOW);
                    estado = ESTADO_ERROR;
                }
                break;
            }
        }
        break;

        case ESTADO_PAUSADO:
        {
            switch (tipo_evento)
            {
              case TIPO_EVENTO_CONTINUE: case TIPO_EVENTO_ABRIR_PUERTA: case TIPO_EVENTO_CERRAR_PUERTA: 
              {
                  if(BT.available() > 0)
                    {
                      int action = BT.parseInt();

                      switch(action)
                      {
                        case PUERTA_BLUETOOTH:
                          cambiarEstadoPuerta();
                          break;
                        case PAUSA_BLUETOOTH:
                          tipo_evento_temp = TIPO_EVENTO_PRESIONA_REANUDAR;
                          break;
                        case CANCELAR_BLUETOOTH:
                          tipo_evento_temp = TIPO_EVENTO_CANCELAR;
                          break;
                        default:
                          break;
                      }
                    }

                  estado = ESTADO_PAUSADO;
              }
              break;
              case TIPO_EVENTO_PRESIONA_REANUDAR:
                if(estado_puerta == PUERTA_ABIERTA)
                {
                  cambiarEstadoPuerta();
                }
                
                recalculoTiempo();

                estado = ESTADO_CALENTANDO;
                tipo_evento_temp = TIPO_EVENTO_CONTINUE;
              break;
              case TIPO_EVENTO_CANCELAR:
                if(estado_puerta == PUERTA_ABIERTA)
                {
                  cambiarEstadoPuerta();
                }
                estado = ESTADO_CALENTANDO;
                tipo_evento_temp = TIPO_EVENTO_TIMEOUT;
                break;
              default:
              {
                  evento.tipo = TIPO_EVENTO_DESCONOCIDO;
                  estado = ESTADO_ERROR;
              }
              break;
            }
        }
        break;

        case ESTADO_ERROR:
        {
            // Tono de error
            tone(PIN_BUZZER, NOTE_C4, 2000);
            
            switch (tipo_evento)
            {
                case TIPO_EVENTO_TIMEOUT:
                    estado = ESTADO_INICIAL;
                    break;
            }
        
        }
        break;
    }
    evento.tipo = TIPO_EVENTO_CONTINUE;
}

// Arduino
//----------------------------------------------
void setup()
{
    Serial.begin(9600);
    BT.begin(9600);
  
    pinMode(PIN_LED, OUTPUT);
    pinMode(PIN_POTENCIOMETRO, INPUT);
    pinMode(PIN_BOTON, INPUT);
  	pinMode(PIN_BUZZER, OUTPUT);

    // Cierra la puera por defecto
  	microservomotor.attach(PIN_MICROSERVOMOTOR, ANCHO_PULSO_MIN_MICROSERVOMOTOR, ANCHO_PULSO_MAX_MICROSERVOMOTOR);  
    microservomotor.write(PUERTA_CERRADA);
  	estado_puerta = PUERTA_CERRADA;

    // Inicializo en estado inicial y evento continue
    estado = ESTADO_INICIAL;
    tipo_evento = TIPO_EVENTO_CONTINUE;
    tipo_evento_temp = TIPO_EVENTO_CONTINUE;

}

void loop()
{
    maquinaDeEstados();
}