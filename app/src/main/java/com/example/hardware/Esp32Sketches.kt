package com.example.hardware

object Esp32Sketches {
    val wifiHttpSketch = """
// SellMore Automated Yogurt Dispenser - ESP32 Wi-Fi HTTP + Serial Monitor Sketch
// Machine: SM-KLA-042 | GPIO 18 Servo | Immediate Payment Protocol

#include <WiFi.h>
#include <WebServer.h>
#include <ESP32Servo.h>

const char* AP_SSID = "SellMore-SM-KLA-042";
const char* AP_PASS = "sellmore1234";

const int SERVO_PIN = 18;
const int STOP_SPEED = 90;
const int DISPENSE_SPEED = 180;

Servo dispenserServo;
WebServer server(80);

int currentSpeed = STOP_SPEED;
int dispenseCycles = 0;
String lastEvent = "BOOT_READY";

void triggerDispenseCycle() {
  lastEvent = "DISPENSE_START";
  Serial.println("[SERVO][DISPENSE_TIMED] Spinning for 2000ms");
  currentSpeed = DISPENSE_SPEED;
  dispenserServo.write(DISPENSE_SPEED);
  delay(2000);
  currentSpeed = STOP_SPEED;
  dispenserServo.write(STOP_SPEED);
  dispenseCycles++;
  lastEvent = "DISPENSE_DONE";
  Serial.printf("ACK:SERVO:DISPENSE_DONE:CYCLES=%d\n", dispenseCycles);
}

void handleStatus() {
  String json = "{";
  json += "\"state\":\"READY\",";
  json += "\"servoAngle\":" + String(currentSpeed) + ",";
  json += "\"servoHomeAngle\":" + String(STOP_SPEED) + ",";
  json += "\"servoActionAngle\":" + String(DISPENSE_SPEED) + ",";
  json += "\"networkMode\":\"WIFI_AP\",";
  json += "\"lastEvent\":\"" + lastEvent + "\",";
  json += "\"cycles\":" + String(dispenseCycles);
  json += "}";
  server.send(200, "application/json", json);
}

void handleStart() {
  server.send(200, "application/json", "{\"status\":\"dispensing\",\"speed\":180}");
  triggerDispenseCycle();
}

void handleReset() {
  currentSpeed = STOP_SPEED;
  dispenserServo.write(STOP_SPEED);
  lastEvent = "STOPPED";
  Serial.println("ACK:SERVO:SPEED=90");
  server.send(200, "application/json", "{\"status\":\"stopped\",\"speed\":90}");
}

void setup() {
  Serial.begin(115200);
  dispenserServo.setPeriodHertz(50);
  dispenserServo.attach(SERVO_PIN, 500, 2400);
  dispenserServo.write(STOP_SPEED);

  WiFi.softAP(AP_SSID, AP_PASS);
  Serial.print("READY:WIFI_AP_IP=");
  Serial.println(WiFi.softAPIP());

  server.on("/api/status", HTTP_GET, handleStatus);
  server.on("/api/start", HTTP_POST, handleStart);
  server.on("/api/reset", HTTP_POST, handleReset);
  server.begin();
}

void loop() {
  server.handleClient();
  if (Serial.available()) {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd == "DISPENSE" || cmd == "TEST" || cmd == "SERVO:0_TO_180") {
      triggerDispenseCycle();
    } else if (cmd == "STATUS") {
      Serial.printf("STATUS:SPEED=%d:STOP=90:CYCLES=%d\n", currentSpeed, dispenseCycles);
    }
  }
}
    """.trimIndent()

    val bluetoothAndUsbOtgSketch = """
// SellMore Automated Yogurt Dispenser - Arduino Uno + HC-05/HC-06 Bluetooth Sketch
// Adapted for Continuous Rotation (360-degree) Servo

#include <SoftwareSerial.h>
#include <Servo.h>

// HC-05 / HC-06 Bluetooth connections:
const int BT_RX_PIN = 10;
const int BT_TX_PIN = 11;
const int SERVO_PIN = 9;

// Continuous Rotation Servo Constants:
// 90 = Stop, 180 = Full speed forward, 0 = Full speed reverse
const int STOP_SPEED = 90;
const int DISPENSE_SPEED = 180;
const int DISPENSE_DURATION_MS = 2000;

SoftwareSerial btSerial(BT_RX_PIN, BT_TX_PIN);
Servo dispenserServo;

int currentSpeed = STOP_SPEED;
int dispenseCycles = 0;

void replyAll(const String& msg) {
  Serial.println(msg);
  btSerial.println(msg);
}

void processCommand(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) return;

  if (cmd == "PING") {
    replyAll("PONG:SELLMORE_UNO_READY:PIN=" + String(SERVO_PIN) + ":BAUD=9600");
  } else if (cmd == "STATUS") {
    replyAll("STATUS:SPEED=" + String(currentSpeed) + ":STOP=90:CYCLES=" + String(dispenseCycles));
  } else if (cmd == "DISPENSE" || cmd == "TEST" || cmd == "SERVO:0_TO_180") {
    replyAll("[SERVO][DISPENSE_TIMED] Spinning for " + String(DISPENSE_DURATION_MS) + "ms");
    currentSpeed = DISPENSE_SPEED;
    dispenserServo.write(DISPENSE_SPEED);
    delay(DISPENSE_DURATION_MS);
    currentSpeed = STOP_SPEED;
    dispenserServo.write(STOP_SPEED);
    dispenseCycles++;
    replyAll("ACK:SERVO:DISPENSE_DONE:CYCLES=" + String(dispenseCycles));
  } else if (cmd.startsWith("SERVO:")) {
    int targetSpeed = constrain(cmd.substring(6).toInt(), 0, 180);
    currentSpeed = targetSpeed;
    dispenserServo.write(targetSpeed);
    replyAll("ACK:SERVO:SPEED=" + String(targetSpeed));
  }
}

void setup() {
  Serial.begin(115200);
  btSerial.begin(9600);
  dispenserServo.attach(SERVO_PIN, 500, 2400);
  dispenserServo.write(STOP_SPEED);
  replyAll("READY:SELLMORE_UNO:PIN=" + String(SERVO_PIN) + ":BT_BAUD=9600");
}

void loop() {
  if (Serial.available()) {
    processCommand(Serial.readStringUntil('\n'));
  }
  if (btSerial.available()) {
    processCommand(btSerial.readStringUntil('\n'));
  }
}
    """.trimIndent()
}
