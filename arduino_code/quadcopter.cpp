#include <Wire.h>
#include <ADXL345.h>
#include <L3G.h>
#include <Adafruit_BMP085.h>
#include <Servo.h>
#include <math.h>

int motor_pin[4] = {
  3, 5, 6, 9};

Servo motor[4];
int throttle[4] = {
  20, 20 ,20 ,20};

// sensors
L3G gyro;
ADXL345 adxl345;

int loseSignalCount;

// sensors data
double standardAcceData[3];
double currentAcceData[3];

float standardGyroData[3];
float currentGyroData[3];

// for PID
double errorX;
double errorY;
double tFix[4];

// for P control
double Kp;

// for I control
double Ki;
double integralX = 0;
double integralY = 0;

// for D control
double Kd;
double lastErrorX = 0;
double lastErrorY = 0;
double derivativeX = 0;
double derivativeY = 0;

int lastGyroTime;

void setup()
{
  int c;
  Serial.begin(9600);
  delay(10);
  Serial.println("initialize!");


  // init sensors
  if(!gyro.init()) {
    Serial.println("gyro init fail");
    while(true){
      delay(100);
      continue;
    };
  }

  adxl345.powerOn();
  initSensorValue();

  for(c=0; c<4; c++){
    throttle[c] = 15;
    motor[c].attach(motor_pin[c]);
  }

  // 5 sec low throttle for setup
  int d;
  for(d=0; d<500; d++){
    sendSignalToMotor();
    delay(10);
  }
  loseSignalCount = 0;

  // init PIC control
  initPIDParam();



  Serial.println("ready to start");
}


void loop()
{
  int c;

  getAcceData();
  getGyroData();

  calculatePID();

  if(Serial.available() >= 4) {
    loseSignalCount = 0;
    for(c=0; c<4; c++) {
      throttle[c] = Serial.read();
    }
  }
  else if(loseSignalCount < 1000){
    loseSignalCount++;		
  } 
  else if(loseSignalCount > 1000) { // lost signal about 10 sec
    decreasePowerOfMotors();
  }

  sendSignalToMotor();

  sendCopterInfo();
  delay(10);
}


void decreasePowerOfMotors() {
  double tsum = 0;
  int c;

  // calculate average
  for(c=0; c<4; c++) {
    tsum += throttle[c];
  }

  for(c=0; c<4; c++) {
    throttle[c] = (int)tsum/4;
  }

  // down
  for(c=0; c<4; c++) {
    throttle[c] -= 10;
  }
}

void sendSignalToMotor() {
  int c;
  for(c=0; c<4; c++) {
    int fix = (int)tFix[c];
    motor[c].write(throttle[c] + fix);
  }
}

void getAcceData () {
  adxl345.get_Gxyz(currentAcceData);
}

void getGyroData () {
  gyro.read();
  int currentGyroTime = millis();
  double deltaGyroTime;
  if(currentGyroTime < lastGyroTime) { // avoid overflow
    deltaGyroTime = ( currentGyroTime + ( 34359737 - lastGyroTime ));
  } 
  else {
    deltaGyroTime = currentGyroTime - lastGyroTime;
  }
  lastGyroTime = currentGyroTime;

  int deltaGX = (int)(gyro.g.x * (deltaGyroTime / 1000));
  int deltaGY = (int)(gyro.g.y * (deltaGyroTime / 1000));
  int deltaGZ = (int)(gyro.g.z * (deltaGyroTime / 1000));

  currentGyroData[0] = (currentGyroData[0] + deltaGX);
  currentGyroData[1] = (currentGyroData[1] + deltaGY);
  currentGyroData[2] = (currentGyroData[2] + deltaGZ);

  for(int c=0; c<3; c++){
    if(currentGyroData[c] > 360){
      currentGyroData[c] -= 360;
    } 
    else if(currentGyroData[c] < -360) {
      currentGyroData[c] += 360;
    }
  }
}

void initSensorValue() {
  // init acceleration data
  adxl345.get_Gxyz(currentAcceData);
  int c;
  for(c=0; c<3; c++){
    standardAcceData[c] = currentAcceData[c];
    currentGyroData[c] = 0;
  }
  lastGyroTime = millis();
}

void calculatePID() {
  int c;
  for(c=0; c<4; c++){
    tFix[c] = 0;
  }

  // calculate error
  errorX = currentAcceData[0] - standardAcceData[0];
  errorY = currentAcceData[1] - standardAcceData[1];


  // P control (X axis)
  tFix[0] -= Kp * errorX;
  tFix[1] += Kp * errorX;
  tFix[2] += Kp * errorX;
  tFix[3] -= Kp * errorX;

  // P control (Y axis)
  tFix[0] += Kp * errorY;
  tFix[1] += Kp * errorY;
  tFix[2] -= Kp * errorY;
  tFix[3] -= Kp * errorY;

  // I control (X axis)
  integralX = integralX * 2/3 + errorX;
  tFix[0] -= Ki * integralX;
  tFix[1] += Ki * integralX;
  tFix[2] += Ki * integralX;
  tFix[3] -= Ki * integralX;

  // I control (Y axis)
  integralY = integralY * 2/3 + errorY;
  tFix[0] += Ki * integralY;
  tFix[1] += Ki * integralY;
  tFix[2] -= Ki * integralY;
  tFix[3] -= Ki * integralY;

  // D control (X axis)
  derivativeX = errorX - lastErrorX;
  tFix[0] -= Kd * derivativeX;
  tFix[1] += Kd * derivativeX;
  tFix[2] += Kd * derivativeX;
  tFix[3] -= Kd * derivativeX;
  lastErrorX = errorX;

  // D control (Y axis)
  derivativeY = errorY - lastErrorY;
  tFix[0] += Kd * derivativeY;
  tFix[1] += Kd * derivativeY;
  tFix[2] -= Kd * derivativeY;
  tFix[3] -= Kd * derivativeY;
  lastErrorY = errorY;  
}

void initPIDParam() {
  int c;
  double Kc = 18;
  double dT = 0.012833;
  double Pc = 0.5;
//  Kp = Kc * 0.6;
//  Ki = 2 * Kp * dT / Pc;
//  Kd = Kp * Pc / (8 * dT);
  Kp = 18;
  Ki = 0.9;
  Kd = 2;

  for(c=0; c<4; c++){
    tFix[c] = 0;
  }

}

void sendCopterInfo() {
  // use csv format
  // throttle[0],throttle[1],throttle[2],throttle[3],acceX,acceY,acceZ,gyroX,gyroY,gyroZ,magneticX,magneticY,magneticZ,tempture,pressure

  int c;
  for(c=0; c<4; c++) {
    Serial.print(throttle[c] + tFix[c]);
    Serial.print(',');
  }
  for(c=0; c<3; c++) {
    Serial.print(currentAcceData[c]);
    Serial.print(',');
  }

  for(c=0; c<3; c++) {
    Serial.print(currentGyroData[c]);
    Serial.print(',');
  }

  for(c=0; c<3; c++) {
    Serial.print(0);
    Serial.print(',');
  }
  Serial.print(0);
  Serial.print(',');
  Serial.print(0);

  Serial.println();
}