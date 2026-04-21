-- 출석 테이블
CREATE TABLE attendance_checkins (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    checkin_at DATETIME NOT NULL,
    message VARCHAR(200),
    message_type VARCHAR(20) NOT NULL,
    suggested_phrase_id BIGINT,
    stamp_style VARCHAR(30) NOT NULL,
    is_winner BOOLEAN NOT NULL DEFAULT FALSE,
    winner_drawn_at DATETIME,
    drops_awarded INT NOT NULL DEFAULT 10,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_attendance_user_date (user_id, checkin_date),
    KEY idx_attendance_user_date (user_id, checkin_date DESC),
    KEY idx_attendance_date (checkin_date DESC),
    KEY idx_attendance_winner (checkin_date, is_winner),
    CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_phrases (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    phrase VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    KEY idx_phrase_active_category (is_active, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gacha_prizes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    cash_value INT NOT NULL,
    total_stock INT NOT NULL,
    remaining_stock INT NOT NULL,
    tier VARCHAR(20) NOT NULL,
    ev_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_prize_active_order (is_active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE gacha_draws (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    prize_id BIGINT NOT NULL,
    prize_name VARCHAR(100) NOT NULL,
    prize_cash_value INT NOT NULL,
    drops_spent INT NOT NULL,
    win_probability DECIMAL(7,5) NOT NULL,
    rng_value DECIMAL(7,5) NOT NULL,
    is_winner BOOLEAN NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    delivered_at DATETIME,
    delivered_by BIGINT,
    delivery_memo VARCHAR(500),
    created_at DATETIME NOT NULL,
    KEY idx_draw_user_created (user_id, created_at DESC),
    KEY idx_draw_winner_delivery (is_winner, delivery_status),
    KEY idx_draw_prize (prize_id),
    CONSTRAINT fk_draw_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_draw_prize FOREIGN KEY (prize_id) REFERENCES gacha_prizes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plant_growth (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stage INT NOT NULL DEFAULT 0,
    growth_score INT NOT NULL DEFAULT 0,
    likes_received INT NOT NULL DEFAULT 0,
    comments_received INT NOT NULL DEFAULT 0,
    praises_received INT NOT NULL DEFAULT 0,
    last_grown_at DATETIME,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_plant_growth_user (user_id),
    CONSTRAINT fk_plant_growth_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
