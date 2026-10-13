package com.example.iotdashboard.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    /**
     * Tạo và expose MqttClient bean.
     * Các class khác (MqttSubscriber, MqttGateway) inject bean này.
     * Kết nối thật sẽ được thực hiện trong MqttSubscriber (@PostConstruct).
     */
    @Bean
    public MqttClient mqttClient() throws MqttException {
        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        opts.setConnectionTimeout(10);
        opts.setKeepAliveInterval(30);
        // Gán credentials nếu được cấu hình
        if (username != null && !username.isBlank()) {
            opts.setUserName(username);
        }
        if (password != null && !password.isBlank()) {
            opts.setPassword(password.toCharArray());
        }
        try {
            client.connect(opts);
            log.info("✅ MQTT connected to {}", brokerUrl);
        } catch (MqttException e) {
            log.warn("⚠️  MQTT connection failed at startup: {}. Will retry automatically.", e.getMessage());
        }
        return client;
    }
}
