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

-- AWAI NAIL 菜单数据（4分类12项）
-- 卸甲
INSERT IGNORE INTO menu_item (category, category_ja, name, name_ja, price, duration, sort_order) VALUES
('卸甲', 'ジェルオフ', '换款卸甲·本甲', '付け替えオフ・自爪', 0, 15, 10),
('卸甲', 'ジェルオフ', '他店来·本甲', '他店ジェルオフ・自爪', 600, 20, 11),
('卸甲', 'ジェルオフ', '他店来·甲片', '他店ジェルオフ・チップ', 1000, 30, 12);
-- 本甲
INSERT IGNORE INTO menu_item (category, category_ja, name, name_ja, price, duration, sort_order) VALUES
('本甲', '自爪ジェル', '单色', 'シンプルカラー', 2600, 60, 20),
('本甲', '自爪ジェル', '裸色', 'ヌードカラー', 3000, 70, 21),
('本甲', '自爪ジェル', '简约·猫眼', 'キャッツアイ', 3600, 80, 22),
('本甲', '自爪ジェル', '轻奢·法式', 'フレンチ', 5600, 100, 23);
-- 延长
INSERT IGNORE INTO menu_item (category, category_ja, name, name_ja, price, duration, sort_order) VALUES
('延长', 'ネイルチップ', '高位半贴', 'ハーフチップ（高め）', 1000, 30, 30),
('延长', 'ネイルチップ', '塑形半贴', 'シェイプハーフチップ', 2000, 45, 31),
('延长', 'ネイルチップ', '加长浅贴', 'ロングスタイル', 3000, 60, 32);
-- 款式甲
INSERT IGNORE INTO menu_item (category, category_ja, name, name_ja, price, duration, sort_order) VALUES
('款式甲', 'アートネイル', '叠加层次款式', 'レイヤーアート', 800, 30, 40),
('款式甲', 'アートネイル', '人物手绘', 'ハンドペイント', 2500, 90, 41),
('款式甲', 'アートネイル', '手工造型', '3Dネイル', 2800, 90, 42);

-- 唯一约束（已手动添加，防止 schema 重复插入）
-- 若需重新添加：ALTER TABLE menu_item ADD UNIQUE INDEX uk_menu_name_category (name, category);

-- ─── 新字段：已在数据库手动添加 total_amount / cancel_reason ──
-- 如需在新数据库中使用，执行：
-- ALTER TABLE reservation ADD COLUMN total_amount INT DEFAULT 0 COMMENT '服务总价(日元)';
-- ALTER TABLE reservation ADD COLUMN cancel_reason VARCHAR(255) COMMENT '取消原因';

-- 默认定金金额设置
INSERT IGNORE INTO store_settings (setting_key, setting_value) VALUES ('deposit_amount', '500');
-- AWAI 新增设置
INSERT IGNORE INTO store_settings (setting_key, setting_value) VALUES ('deposit_percentage', '30');
INSERT IGNORE INTO store_settings (setting_key, setting_value) VALUES ('time_slot_duration', '90');
INSERT IGNORE INTO store_settings (setting_key, setting_value) VALUES ('closed_days', 'MONDAY');

-- ─── 菜单子选项表（贴钻、珍珠等加购项）─────────────────────
CREATE TABLE IF NOT EXISTS menu_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id BIGINT NOT NULL COMMENT '关联主菜单项目ID',
    name VARCHAR(100) NOT NULL COMMENT '选项名称',
    name_ja VARCHAR(100) COMMENT '日文名称',
    price INT NOT NULL COMMENT '额外加收金额(日元)',
    duration INT NOT NULL COMMENT '额外增加时长(分钟)',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_menu_item_id (menu_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单子选项表';

-- reservation 新增字段：关联菜单项目和选择的子选项
-- 已手动添加，MySQL 不支持 ADD COLUMN IF NOT EXISTS
-- ALTER TABLE reservation ADD COLUMN menu_item_id BIGINT COMMENT '关联的主菜单项目ID';
-- ALTER TABLE reservation ADD COLUMN selected_options TEXT COMMENT 'JSON: 选择的子选项ID列表';

-- ─── 作品集图片表 ─────────────────────────────────
CREATE TABLE IF NOT EXISTS portfolio_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL COMMENT '图片路径',
    tag VARCHAR(100) COMMENT '标签，如 猫眼/法式/渐变',
    category VARCHAR(50) COMMENT '分类，对应菜单分类',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active INT DEFAULT 1 COMMENT '1显示, 0隐藏',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品集图片表';
