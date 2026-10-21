-- ============================================================
-- Seed data cho hệ thống IoT Dashboard
-- Spring Boot tự chạy khi khởi động:
-- spring.sql.init.mode=always

-- ============================================================
-- 1. Seed tài khoản User mẫu
-- ============================================================
INSERT INTO `user` (
    id,
    username,
    password,
    student_code,
    name,
    avatar_url,
    `class`,
    link_figma,
    link_postman,
    link_github,
    link_report
)
VALUES (
    1,
    'B23DCCN359',
    '$2a$10$rtmV0jHA7bVz6y9sSBZAeuuBykEiMf7uwI.LtVvUau/OmVHgbDqJW',
    'B23DCCN359',
    'Nguyễn Chí Hùng',
    'assets/avatar.png',
    'D23CNPM04',
    'https://www.figma.com/design/MbnXNmzyCE9dDX82c0gB7A/IoT?node-id=2-2',
    'https://documenter.getpostman.com/view/57902168/2sBYAvupfr?utm_source=postman-web-app#fb96ecb9-0b9d-4693-8d28-ce2af2a8dcf0',
    'https://github.com/Hunguyenobugs/BTL-IoT',
    'https://drive.google.com/file/d/1er5xbBGQEGRrCE3pjmINtnPvuimgubSL/view?usp=sharing'
);

-- ============================================================
-- 2. Seed 2 thiết bị LED
-- ============================================================
INSERT INTO devices (id, name, type, current_status)
VALUES
    (1, 'LED 1', 'LED', 'OFF'),
    (2, 'LED 2', 'LED', 'OFF');

-- ============================================================
-- 3. Seed 3 loại cảm biến
-- ============================================================
INSERT INTO sensors (id, name, unit)
VALUES
    (1, 'Nhiệt độ', '°C'),
    (2, 'Độ ẩm', '%'),
    (3, 'Ánh sáng', 'lux');