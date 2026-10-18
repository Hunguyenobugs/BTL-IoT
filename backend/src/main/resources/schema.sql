-- ============================================================
-- Schema cho hệ thống IoT Dashboard
--
-- Lưu ý:
-- Spring Boot SQL init không hỗ trợ CREATE DATABASE / USE.
-- Hãy tạo database thủ công trước:
-- CREATE DATABASE iot
-- CHARACTER SET utf8mb4
-- COLLATE utf8mb4_unicode_ci;
--
-- Sau đó cấu hình đúng URL trong application.yml.
-- ============================================================


-- ============================================================
-- 1. Bảng User: thông tin người dùng và tài khoản đăng nhập
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    student_code  VARCHAR(20)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    `class`       VARCHAR(50),
    link_figma    VARCHAR(255),
    link_postman  VARCHAR(255),
    link_github   VARCHAR(255),
    link_report   VARCHAR(255)
);


-- ============================================================
-- 2. Bảng Devices: danh mục thiết bị (LED)
-- ============================================================
CREATE TABLE IF NOT EXISTS devices (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(50) NOT NULL,
    type           VARCHAR(50) NOT NULL,
    current_status VARCHAR(10) NOT NULL DEFAULT 'OFF'
);


-- ============================================================
-- 3. Bảng Sensors: danh mục loại cảm biến
-- ============================================================
CREATE TABLE IF NOT EXISTS sensors (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    unit VARCHAR(10) NOT NULL
);


-- ============================================================
-- 4. Bảng Datasensors: dữ liệu đo lường thực tế
-- ============================================================
CREATE TABLE IF NOT EXISTS datasensors (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    sensors_id  BIGINT NOT NULL,
    value       DOUBLE NOT NULL,
    measured_at DATETIME NOT NULL,

    CONSTRAINT fk_datasensors_sensors
        FOREIGN KEY (sensors_id) REFERENCES sensors(id),

    INDEX idx_ds_sensor_time (sensors_id, measured_at)
);


-- ============================================================
-- 5. Bảng History: lịch sử điều khiển thiết bị
-- ============================================================
CREATE TABLE IF NOT EXISTS history (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    device_id   BIGINT NOT NULL,
    action      VARCHAR(10) NOT NULL,
    status      VARCHAR(10) NOT NULL DEFAULT 'WAITING',
    executed_at DATETIME NOT NULL,

    CONSTRAINT fk_history_user
        FOREIGN KEY (user_id) REFERENCES `user`(id),

    CONSTRAINT fk_history_devices
        FOREIGN KEY (device_id) REFERENCES devices(id),

    INDEX idx_hist_user (user_id),
    INDEX idx_hist_device_stat (device_id, status),
    INDEX idx_hist_stat_time (status, executed_at)
);
