#define INPUT_SIZE 7

int IN3 = 5; // Input3 подключен к выводу 5 
int IN4 = 4;
int ENB = 3; // левый двигатель
int IN6 = 6; // Input3 подключен к выводу 5 
int IN7 = 7;
int ENA = 9; // правый двигатель

// ultra-sonic sensor
int TRIG = 11;
int ECHO = 12;

long distance = 0;

struct MOTOR_SPEED {
  int left;
  int right;
};

MOTOR_SPEED speed = { 0, 0 };

void setup()
{
 // motor driver
 pinMode (ENB, OUTPUT); 
 pinMode (IN3, OUTPUT);
 pinMode (IN4, OUTPUT);
 pinMode (ENA, OUTPUT); 
 pinMode (IN6, OUTPUT);
 pinMode (IN7, OUTPUT);
 
 // ultra-sonic sensor
 pinMode(TRIG, OUTPUT);
 pinMode(ECHO, INPUT);
  
 Serial.begin(9600);

 analogWrite(ENB,0);
 analogWrite(ENA,0);

 getDistance();
}

void loop()
{
  String left = Serial.readStringUntil(':');
  if (left != "") {      
    String right = Serial.readStringUntil(';');
    if (right != "") {
      speed.right = right.toInt();
      speed.left = left.toInt();
    } else {
      speed.left = 0;
      speed.right = 0;  
    }
  } else {
    speed.left = 0;
    speed.right = 0;
  }

  drive();
}

/**
 * uses simple rounding of three values
 */
void getDistance() {
  long measures[] = {0, 0, 0};
  long sum = 0;
  for (int i = 0; i <=2; i++) {
    measures[i] = measureDistance();
    delayMicroseconds(250);
  }
  for (int i = 0; i <=2; i++) {
    sum += measures[i];
  }
  distance = sum / 3;
}

long measureDistance() {
  digitalWrite(TRIG, LOW);
  delayMicroseconds(5);
  digitalWrite(TRIG, HIGH);

  delayMicroseconds(10);
  digitalWrite(TRIG, LOW);

  long duration = pulseIn(ECHO, HIGH);
  return (duration / 2) / 29.1;
}

void drive() {
 
  if ((speed.left > 0) && (speed.right > 0)) {
    moveFwd();
  }

  if ((speed.left < 0) && (speed.right < 0)) {
    moveBack();
  }

  if ((speed.left <= 0) && (speed.right > 0)) {
    moveRight();
  }

  if ((speed.left > 0) && (speed.right <= 0)) {
    moveLeft();
  }

  speed.left = abs(speed.left);
  speed.right = abs(speed.right);

  if (speed.left > 254) {
    speed.left = 255;
  }
  if (speed.right > 254) {
    speed.right = 255;
  }
  
  analogWrite(ENB, speed.left);
  analogWrite(ENA, speed.right);
}


// На пару выводов "IN" поданы разноименные сигналы, мотор готов к вращению
void moveFwd()
{
  digitalWrite (IN3, LOW);
  digitalWrite (IN4, HIGH);
  digitalWrite (IN6, LOW);
  digitalWrite (IN7, HIGH);
}

void moveBack() {
  analogWrite(ENB,0);
  analogWrite(ENA,0);
  
  digitalWrite (IN3, HIGH);
  digitalWrite (IN4, LOW);
  digitalWrite (IN6, HIGH);
  digitalWrite (IN7, LOW);
}

void moveLeft()
{
  analogWrite(ENB,0);
  analogWrite(ENA,0);
  
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  digitalWrite (IN6, LOW);
  digitalWrite (IN7, HIGH);
}

void moveRight()
{
  analogWrite(ENB,0);
  analogWrite(ENA,0);
  
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  digitalWrite (IN6, HIGH);
  digitalWrite (IN7, LOW);
}
