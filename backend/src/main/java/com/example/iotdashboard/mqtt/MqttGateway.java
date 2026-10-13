package com.example.iotdashboard.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MQTT publisher: publish lệnh điều khiển lên topic device/control.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MqttGateway {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.topics.device-control}")
    private String topicDeviceControl;

    /**
     * Publish lệnh điều khiển LED lên device/control.
     *
     * @param deviceId ID thiết bị (1 = LED1, 2 = LED2)
     * @param deviceKey key dạng "led1", "led2"
     * @param action    "ON" hoặc "OFF"
     */
    public void publishDeviceControl(Long deviceId, String deviceKey, String action) {
        try {
            var payload = new java.util.LinkedHashMap<String, Object>();
            payload.put("deviceId", deviceId);
            payload.put("device", deviceKey);
            payload.put("action", action);

            String json = objectMapper.writeValueAsString(payload);
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(1);
            message.setRetained(false);

            mqttClient.publish(topicDeviceControl, message);
            log.info("MQTT published to [{}]: {}", topicDeviceControl, json);
        } catch (Exception e) {
            log.error("Failed to publish device control: {}", e.getMessage(), e);
        }
    }
}
