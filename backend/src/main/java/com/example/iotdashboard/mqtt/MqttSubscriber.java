package com.example.iotdashboard.mqtt;

import com.example.iotdashboard.service.DeviceService;
import com.example.iotdashboard.service.SensorService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MQTT subscriber: lắng nghe 2 topic:
 *  - data/sensors  → SensorService.saveSensorData()
 *  - device/status → DeviceService.handleDeviceStatus()
 *
 * Dùng MqttCallbackExtended để nhận sự kiện connectComplete(),
 * từ đó re-subscribe sau mỗi lần Paho tự reconnect.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MqttSubscriber {

    private final MqttClient mqttClient;
    private final SensorService sensorService;
    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.topics.sensor-data}")
    private String topicSensorData;

    @Value("${mqtt.topics.device-status}")
    private String topicDeviceStatus;

    @PostConstruct
    public void subscribe() {
        // MqttCallbackExtended có thêm connectComplete() – quan trọng để re-subscribe
        // sau khi Paho tự động reconnect (setAutomaticReconnect = true)
        mqttClient.setCallback(new MqttCallbackExtended() {

            /**
             * Gọi khi kết nối được thiết lập (lần đầu HOẶC sau khi reconnect).
             * reconnect = false: kết nối lần đầu
             * reconnect = true : vừa reconnect thành công → phải subscribe lại
             */
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    log.info("MQTT reconnected to {}. Re-subscribing topics...", serverURI);
                } else {
                    log.info("MQTT connected to {}. Subscribing topics...", serverURI);
                }
                doSubscribe();
            }

            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost: {}. Paho will retry automatically.", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                log.debug("MQTT message on [{}]: {}", topic, payload);
                try {
                    JsonNode json = objectMapper.readTree(payload);
                    if (topicSensorData.equals(topic)) {
                        sensorService.saveSensorData(json);
                    } else if (topicDeviceStatus.equals(topic)) {
                        deviceService.handleDeviceStatus(json);
                    }
                } catch (Exception e) {
                    log.error("Error processing MQTT message on {}: {}", topic, e.getMessage(), e);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // publish confirm – không cần xử lý
            }
        });

        // Subscribe ngay lần đầu (nếu đã kết nối trong MqttConfig)
        doSubscribe();
    }

    /** Subscribe cả 2 topic với QoS 1 */
    public void doSubscribe() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.subscribe(
                    new String[]{ topicSensorData, topicDeviceStatus },
                    new int[]   { 1,               1 }
                );
                log.info("MQTT subscribed → [{}], [{}]", topicSensorData, topicDeviceStatus);
            } else {
                log.warn("MQTT not connected, subscribe skipped (will retry via connectComplete)");
            }
        } catch (MqttException e) {
            log.error("Failed to subscribe: {}", e.getMessage(), e);
        }
    }
}
