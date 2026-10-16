-- ============================================================
-- Seed data cho hệ thống IoT Dashboard
-- Spring Boot sẽ tự chạy khi khởi động (spring.sql.init.mode=always)
-- ON DUPLICATE KEY UPDATE để cập nhật khi restart
-- ============================================================

-- Seed 1 bản ghi User mẫu
INSERT INTO `user` (
    id,
    student_code,
    name,
    `class`,
    link_figma,
    link_postman,
    link_github,
    link_report
)
VALUES (
    1,
    'B23DCCN359',
    'Nguyễn Chí Hùng',
    'D23CNPM04',
    'https://www.figma.com/design/MbnXNmzyCE9dDX82c0gB7A/IoT?node-id=2-2',
    'https://documenter.getpostman.com/view/57902168/2sBYAvupfr?utm_source=postman-web-app#fb96ecb9-0b9d-4693-8d28-ce2af2a8dcf0',
    'https://github.com/Hunguyenobugs/BTL-IoT',
    'https://drive.google.com/file/d/1er5xbBGQEGRrCE3pjmINtnPvuimgubSL/view?usp=sharing'
)
ON DUPLICATE KEY UPDATE
    student_code = VALUES(student_code),
    name         = VALUES(name),
    `class`      = VALUES(`class`),
    link_figma   = VALUES(link_figma),
    link_postman = VALUES(link_postman),
    link_github  = VALUES(link_github),
    link_report  = VALUES(link_report);


-- Seed 2 thiết bị LED
INSERT IGNORE INTO devices (id, name, type, current_status) VALUES (1, 'LED 1', 'LED', 'OFF');
INSERT IGNORE INTO devices (id, name, type, current_status) VALUES (2, 'LED 2', 'LED', 'OFF');

-- Seed 3 loại cảm biến
INSERT IGNORE INTO sensors (id, name, unit) VALUES (1, 'Nhiệt độ', '°C');
INSERT IGNORE INTO sensors (id, name, unit) VALUES (2, 'Độ ẩm', '%');
INSERT IGNORE INTO sensors (id, name, unit) VALUES (3, 'Ánh sáng', 'lux');
