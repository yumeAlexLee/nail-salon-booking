CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_type VARCHAR(20) NOT NULL COMMENT '新客/老客',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    contact_id VARCHAR(100) NOT NULL COMMENT 'VX/LINE ID',
    removal_type VARCHAR(20) NOT NULL COMMENT '卸甲类型：本甲/甲片/无',
    remarks VARCHAR(500) COMMENT '备注',
    reference_image VARCHAR(500) COMMENT '款式参考图路径',
    reserve_date DATE NOT NULL COMMENT '预约日期',
    time_slot VARCHAR(20) NOT NULL COMMENT '预约时间段，如 10:00-12:00',
    status VARCHAR(20) DEFAULT 'BOOKED' COMMENT '状态',
    is_deleted INT DEFAULT 0 COMMENT '0正常, 1已取消',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deposit_amount INT DEFAULT 500 COMMENT '定金金额（日元）',
    deposit_status VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE/CUSTOMER_PAID/PAID/REFUNDED/FORFEITED',
    deposit_paid_at TIMESTAMP NULL COMMENT '定金支付时间',
    UNIQUE KEY uk_reserve_date_time (reserve_date, time_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

CREATE TABLE IF NOT EXISTS locked_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lock_date DATE NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    reason VARCHAR(255) COMMENT '锁定原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lock_date_time (lock_date, time_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='锁定时间表';

CREATE TABLE IF NOT EXISTS store_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺配置表';

-- 插入默认营业时间
INSERT IGNORE INTO store_settings (setting_key, setting_value) VALUES ('open_time', '10:00');
INSERT IGNORE INTO store_settings (setting_key, setting_value) VALUES ('close_time', '20:00');

-- ─── 菜单项目表 ──────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL COMMENT '分类，如 美甲/美睫/手足护理',
    category_ja VARCHAR(50) COMMENT '分类(日文)',
    name VARCHAR(100) NOT NULL COMMENT '服务项目名称',
    name_ja VARCHAR(100) COMMENT '服务项目名称(日文)',
    price INT NOT NULL COMMENT '价格(日元)',
    duration INT COMMENT '所需时长(分钟)',
    description VARCHAR(500) COMMENT '说明',
    description_ja VARCHAR(500) COMMENT '说明(日文)',
    sort_order INT DEFAULT 0 COMMENT '排序',
    image_url VARCHAR(500) COMMENT '示意图URL',
    is_active INT DEFAULT 1 COMMENT '1显示, 0隐藏',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单项目表';

-- 插入示例数据（仅保留美甲，已删除美睫）
INSERT IGNORE INTO menu_item (category, category_ja, name, name_ja, price, duration, description, description_ja, sort_order) VALUES
('美甲', 'ネイル', '单色美甲', '単色ネイル', 3000, 60, '纯色简约美甲，多种颜色可选', 'シンプルな単色ネイル、豊富なカラーからお選びいただけます', 1),
('美甲', 'ネイル', '猫眼美甲', 'キャッツアイ', 4500, 90, '猫眼效果，优雅闪耀', '高級感のあるキャッツアイデザイン', 2),
('美甲', 'ネイル', '法式美甲', 'フレンチネイル', 4000, 75, '经典法式，优雅大方', 'クラシックなフレンチデザイン', 3),
('美甲', 'ネイル', '渐变美甲', 'グラデーション', 5000, 90, '渐变色彩，精致过渡', '美しいグラデーションカラー', 4);

-- 唯一约束（已手动添加，防止 schema 重复插入）
-- 若需重新添加：ALTER TABLE menu_item ADD UNIQUE INDEX uk_menu_name_category (name, category);

-- ─── 定金功能 ──────────────────────────────────────
-- deposit_amount / deposit_status / deposit_paid_at 已在 reservation 表定义中包含。
-- 如果是已有数据库，请手动执行：
-- ALTER TABLE reservation
--     ADD COLUMN IF NOT EXISTS deposit_amount INT DEFAULT 500 COMMENT '定金金额（日元）',
--     ADD COLUMN IF NOT EXISTS deposit_status VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE/CUSTOMER_PAID/PAID/REFUNDED/FORFEITED',
--     ADD COLUMN IF NOT EXISTS deposit_paid_at TIMESTAMP NULL COMMENT '定金支付时间';

-- 默认定金金额设置
INSERT IGNORE INTO store_settings (setting_key, setting_value) VALUES ('deposit_amount', '500');
