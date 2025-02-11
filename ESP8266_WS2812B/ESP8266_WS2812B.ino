#include <Adafruit_NeoPixel.h>
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <CustomWiFiManager.h>   // 引入自定义WiFiManager库

#define LED_PIN     4    // GPIO4连接到WS2812B
#define NUM_LEDS    6    // LED灯的数量
#define UDP_PORT    10000 // UDP端口号

Adafruit_NeoPixel strip(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

WiFiUDP Udp;
char incomingPacket[255]; // 接收数据包的缓冲区
IPAddress remoteIp;       // 存储远程IP

void setup() {
  Serial.begin(115200);
  strip.begin();
  strip.show(); // 初始化LED，关闭

  // 启动自定义WiFiManager，使用自动配网模式
  WiFiManager wifiManager;

  // 如果没有保存Wi-Fi配置，进入配网页面
  if (!wifiManager.autoConnect("氛围灯")) {//wifi名称
    Serial.println("Failed to connect to Wi-Fi, entering AP mode...");
    // 如果连接失败，启动AP模式，让用户配置Wi-Fi
    if (!wifiManager.startConfigPortal()) {
      Serial.println("Failed to start config portal, entering deep sleep...");
      ESP.deepSleep(10 * 1000000);  // 如果配网失败，深度睡眠10秒后重启
    }
  }

  Serial.println("Connected to Wi-Fi");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  // 启动UDP
  Udp.begin(UDP_PORT);
  Serial.printf("UDP server started on port %d\n", UDP_PORT);
}

void loop() {
  int packetSize = Udp.parsePacket();
  if (packetSize) {
    // 获取数据包的远程IP地址
    remoteIp = Udp.remoteIP();
    int len = Udp.read(incomingPacket, 255);
    if (len > 0) {
      incomingPacket[len] = 0; // 确保数据包以NULL结尾
    }

    // 打印接收到的数据
    Serial.printf("Received packet from %s:%d\n", remoteIp.toString().c_str(), Udp.remotePort());
    Serial.printf("Data: %s\n", incomingPacket);

    // 解析JSON数据包
    StaticJsonDocument<200> doc;  // 创建一个200字节的JSON文档
    DeserializationError error = deserializeJson(doc, incomingPacket);
    
    if (error) {
      Serial.println("Failed to parse JSON");
      return;
    }

    // 获取JSON数据
    int brightness = doc["brightness"];
    JsonArray leds = doc["leds"].as<JsonArray>();

    for (int i = 0; i < NUM_LEDS; i++) {
      int r = leds[i][0];  // Red
      int g = leds[i][1];  // Green
      int b = leds[i][2];  // Blue

      // 设置LED颜色和亮度
      strip.setPixelColor(i, strip.Color(r * brightness / 255, g * brightness / 255, b * brightness / 255));
    }
    strip.show();
    Serial.printf("LED colors updated with brightness %d\n", brightness);
  }
}
