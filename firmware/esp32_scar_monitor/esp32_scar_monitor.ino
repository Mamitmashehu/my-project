/*
 * ScarGuard - ESP32 sensor node reference firmware
 * ---------------------------------------------------------------------------
 * Runs as a BLE peripheral exposing one service with two characteristics that
 * match com.scarguard.app.ble.BleConstants in the Android app:
 *
 *   Service UUID:        6e400001-b5a3-f393-e0a9-e50e24dcca9e
 *   Temperature (notify): 6e400002-b5a3-f393-e0a9-e50e24dcca9e
 *     -> 4-byte little-endian IEEE-754 float, degrees Celsius, sent every
 *        SAMPLE_INTERVAL_MS.
 *   Command (write):      6e400003-b5a3-f393-e0a9-e50e24dcca9e
 *     -> app writes a single byte; 0x01 = blink the onboard/status LED, which
 *        is handy for lining the phone camera up at a consistent moment.
 *
 * Default sensor wiring below is a simple NTC thermistor voltage divider
 * (cheapest, zero extra libraries) with notes on swapping in a DS18B20
 * (contact digital sensor) or an MLX90614 (non-contact IR sensor) instead --
 * either is a better long-term choice for skin temperature than a bare NTC.
 *
 * Board: any ESP32 dev board (this does NOT need an ESP32-CAM; the app uses
 * the phone's own camera for photos, so no camera wiring is required here).
 * Library: uses the built-in "ESP32 BLE Arduino" library that ships with the
 * arduino-esp32 core -- no extra library installation needed for this file
 * as written.
 */

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// ---- GATT UUIDs: must match BleConstants.kt in the Android app -----------
#define SERVICE_UUID        "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define TEMPERATURE_CHAR_UUID "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
#define COMMAND_CHAR_UUID   "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

// ---- Pins -------------------------------------------------------------
#define THERMISTOR_PIN      34   // ADC1 input; NTC voltage divider midpoint
#define STATUS_LED_PIN       2   // Most dev boards have a status LED on GPIO2

// ---- Thermistor math (skip/replace this block if using DS18B20/MLX90614) --
const float SERIES_RESISTOR_OHMS = 10000.0;   // fixed resistor in the divider
const float NOMINAL_RESISTANCE_OHMS = 10000.0; // NTC resistance at 25C
const float NOMINAL_TEMPERATURE_C = 25.0;
const float B_COEFFICIENT = 3950.0;            // from the thermistor datasheet
const float ADC_MAX = 4095.0;                  // ESP32 ADC is 12-bit

const unsigned long SAMPLE_INTERVAL_MS = 2000;

BLECharacteristic *temperatureCharacteristic;
BLECharacteristic *commandCharacteristic;
bool deviceConnected = false;
unsigned long lastSampleAt = 0;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *server) override {
    deviceConnected = true;
  }
  void onDisconnect(BLEServer *server) override {
    deviceConnected = false;
    // Restart advertising so the app can reconnect after it disconnects.
    BLEDevice::startAdvertising();
  }
};

class CommandCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *characteristic) override {
    std::string value = characteristic->getValue();
    if (value.length() > 0 && (uint8_t)value[0] == 0x01) {
      blinkStatusLed();
    }
  }
};

void blinkStatusLed() {
  digitalWrite(STATUS_LED_PIN, HIGH);
  delay(150);
  digitalWrite(STATUS_LED_PIN, LOW);
}

float readTemperatureC() {
  // --- Default: NTC thermistor voltage divider ---
  int adcValue = analogRead(THERMISTOR_PIN);
  if (adcValue <= 0) return NAN;

  float resistance = SERIES_RESISTOR_OHMS / ((ADC_MAX / (float)adcValue) - 1.0);
  float steinhart = resistance / NOMINAL_RESISTANCE_OHMS;
  steinhart = log(steinhart);
  steinhart /= B_COEFFICIENT;
  steinhart += 1.0 / (NOMINAL_TEMPERATURE_C + 273.15);
  steinhart = 1.0 / steinhart;
  steinhart -= 273.15; // Kelvin -> Celsius
  return steinhart;

  // --- To use a DS18B20 instead (needs OneWire + DallasTemperature libs) ---
  // sensors.requestTemperatures();
  // return sensors.getTempCByIndex(0);

  // --- To use an MLX90614 non-contact IR sensor instead (needs
  //     Adafruit_MLX90614 lib, I2C SDA/SCL wiring) ---
  // return mlx.readObjectTempC();
}

void setup() {
  Serial.begin(115200);
  pinMode(STATUS_LED_PIN, OUTPUT);
  analogReadResolution(12);

  BLEDevice::init("ScarGuard Sensor");
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService *service = server->createService(SERVICE_UUID);

  temperatureCharacteristic = service->createCharacteristic(
      TEMPERATURE_CHAR_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  temperatureCharacteristic->addDescriptor(new BLE2902());

  commandCharacteristic = service->createCharacteristic(
      COMMAND_CHAR_UUID,
      BLECharacteristic::PROPERTY_WRITE);
  commandCharacteristic->setCallbacks(new CommandCallbacks());

  service->start();

  BLEAdvertising *advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  BLEDevice::startAdvertising();

  Serial.println("ScarGuard sensor advertising, waiting for the app to connect...");
}

void loop() {
  unsigned long now = millis();
  if (now - lastSampleAt >= SAMPLE_INTERVAL_MS) {
    lastSampleAt = now;
    float tempC = readTemperatureC();

    if (!isnan(tempC)) {
      Serial.printf("Temperature: %.2f C\n", tempC);
      if (deviceConnected) {
        // Send as 4-byte little-endian float, matching EspBleManager.parseTemperature().
        uint8_t bytes[4];
        memcpy(bytes, &tempC, 4);
        temperatureCharacteristic->setValue(bytes, 4);
        temperatureCharacteristic->notify();
      }
    }
  }
}
