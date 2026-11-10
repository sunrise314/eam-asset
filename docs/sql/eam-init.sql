-- ============================================================================
-- eam-asset 初始化 DDL + 种子数据（PostgreSQL）
-- EAM 设备资产管理：设备台账 / 保养计划 / 保养工单 / 维修工单 / 备件 / 备件出入库流水
-- 用法：psql -d your_db -f eam-init.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 表 1：设备台账。attributes 用 JSONB 存扩展属性（功率/厂家/序列号等），
-- 避免为每个行业加字段；status 随报修/维修完成/报废联动。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eam_asset (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL DEFAULT 1,
    asset_no            VARCHAR(64)  NOT NULL,                -- 设备编号，租户内唯一，如 EQ-CNC-001
    name                VARCHAR(128) NOT NULL,                -- 设备名称
    model               VARCHAR(128),                         -- 规格型号
    category            VARCHAR(64),                          -- 设备分类，如 机加/注塑/装配
    location            VARCHAR(128),                         -- 安装位置，如 一车间-A线
    status              SMALLINT     NOT NULL DEFAULT 1,      -- 1=运行 2=停机 3=维修中 4=报废
    purchase_date       DATE,                                 -- 采购日期
    warranty_until      DATE,                                 -- 质保到期日
    responsible_user_id BIGINT,                               -- 责任人用户 ID
    attributes          JSONB        NOT NULL DEFAULT '{}',   -- 扩展属性，如 {"power":"15kW","vendor":"沈阳机床"}
    remark              VARCHAR(512) NOT NULL DEFAULT '',
    creator             VARCHAR(64),
    create_time          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updater             VARCHAR(64),
    update_time          TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (tenant_id, asset_no)
);

CREATE INDEX IF NOT EXISTS idx_eam_asset_status   ON eam_asset(status);
CREATE INDEX IF NOT EXISTS idx_eam_asset_category ON eam_asset(category);

-- ---------------------------------------------------------------------------
-- 表 2：保养计划。周期是"相对上次完成时间"的滑动窗口（cycle_type × cycle_value），
-- next_maintain_time 由 MaintainPlanEngine 扫描命中后推进。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eam_maintain_plan (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          BIGINT       NOT NULL DEFAULT 1,
    plan_no            VARCHAR(64)  NOT NULL,                -- 计划编号，租户内唯一，如 MP-2026-001
    asset_id           BIGINT       NOT NULL REFERENCES eam_asset(id),
    plan_name          VARCHAR(128) NOT NULL,
    cycle_type         SMALLINT     NOT NULL,                -- 1=按天数 2=按周数 3=按月数
    cycle_value        INT          NOT NULL,                -- 周期数值，如 1（个月）
    advance_days       INT          NOT NULL DEFAULT 0,      -- 提前几天生成工单
    last_maintain_time TIMESTAMP,                            -- 上次保养时间
    next_maintain_time TIMESTAMP,                            -- 下次保养时间（录入时初始化）
    status             SMALLINT     NOT NULL DEFAULT 1,      -- 0=停用 1=启用
    remark             VARCHAR(512) NOT NULL DEFAULT '',
    creator            VARCHAR(64),
    create_time         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updater            VARCHAR(64),
    update_time         TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (tenant_id, plan_no)
);

CREATE INDEX IF NOT EXISTS idx_eam_maintain_plan_asset  ON eam_maintain_plan(asset_id);
CREATE INDEX IF NOT EXISTS idx_eam_maintain_plan_status ON eam_maintain_plan(status);

-- ---------------------------------------------------------------------------
-- 表 3：保养工单。由计划到期自动生成（order_no = MO-yyyyMMdd-planId），
-- 也可手工补录；version 乐观锁防移动端/PC 端并发执行覆盖。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eam_maintain_order (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL DEFAULT 1,
    order_no    VARCHAR(64)  NOT NULL,                -- 工单号
    plan_id     BIGINT       REFERENCES eam_maintain_plan(id),  -- 来源计划（手工补录可空）
    asset_id    BIGINT       NOT NULL REFERENCES eam_asset(id),
    plan_date   DATE,                                 -- 计划保养日期
    status      SMALLINT     NOT NULL DEFAULT 1,      -- 1=待执行 2=执行中 3=已完成 4=已取消
    assignee_id BIGINT,                               -- 执行人用户 ID
    start_time  TIMESTAMP,                            -- 实际开始时间
    finish_time TIMESTAMP,                            -- 实际完成时间
    content     VARCHAR(512),                         -- 保养内容
    result_note VARCHAR(512),                         -- 执行结果记录
    version     INT          NOT NULL DEFAULT 0,      -- 乐观锁
    creator     VARCHAR(64),
    create_time  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updater     VARCHAR(64),
    update_time  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (tenant_id, order_no)
);

CREATE INDEX IF NOT EXISTS idx_eam_maintain_order_plan   ON eam_maintain_order(plan_id);
CREATE INDEX IF NOT EXISTS idx_eam_maintain_order_asset  ON eam_maintain_order(asset_id);
CREATE INDEX IF NOT EXISTS idx_eam_maintain_order_status ON eam_maintain_order(status);

-- ---------------------------------------------------------------------------
-- 表 4：维修工单。状态机闭环：待派工→已派工→维修中→待验收→已完成/已取消。
-- start_time/finish_time 之间是维修时长，MTBF/MTTR 只统计 status=5 的工单。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eam_repair_order (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL DEFAULT 1,
    order_no      VARCHAR(64)  NOT NULL,                -- 工单号，如 RO-20261108-001
    asset_id      BIGINT       NOT NULL REFERENCES eam_asset(id),
    fault_desc    VARCHAR(512) NOT NULL,                -- 故障描述
    priority      SMALLINT     NOT NULL DEFAULT 2,      -- 1=低 2=中 3=高 4=紧急
    status        SMALLINT     NOT NULL DEFAULT 1,      -- 1=待派工 2=已派工 3=维修中 4=待验收 5=已完成 6=已取消
    reporter      VARCHAR(64),                          -- 报修人
    assignee_id   BIGINT,                               -- 维修人用户 ID
    dispatch_time TIMESTAMP,                            -- 派工时间
    start_time    TIMESTAMP,                            -- 维修开始时间
    finish_time   TIMESTAMP,                            -- 维修完成时间
    repair_note   VARCHAR(512),                         -- 维修记录
    accept_note   VARCHAR(512),                         -- 验收记录
    version       INT          NOT NULL DEFAULT 0,      -- 乐观锁
    creator       VARCHAR(64),
    create_time    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updater       VARCHAR(64),
    update_time    TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (tenant_id, order_no)
);

CREATE INDEX IF NOT EXISTS idx_eam_repair_order_asset  ON eam_repair_order(asset_id);
CREATE INDEX IF NOT EXISTS idx_eam_repair_order_status ON eam_repair_order(status);

-- ---------------------------------------------------------------------------
-- 表 5：备件。asset_ids 用 JSONB 存适用设备 ID 列表（一个备件可用于多台设备），
-- 避免为"备件-设备"多对多关系单建关联表。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eam_spare_part (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL DEFAULT 1,
    part_no      VARCHAR(64)  NOT NULL,                -- 备件编号，租户内唯一，如 SP-BEAR-001
    name         VARCHAR(128) NOT NULL,                -- 备件名称
    spec         VARCHAR(128),                         -- 规格型号，如 SKF 7010C
    unit         VARCHAR(16),                          -- 计量单位，如 套/件/包
    stock        INT          NOT NULL DEFAULT 0,      -- 当前库存（只能由出入库流水改变）
    safety_stock INT          NOT NULL DEFAULT 0,      -- 安全库存（低于此值触发预警）
    asset_ids    JSONB,                                -- 适用设备 ID 列表，如 [1,2]
    location     VARCHAR(128),                         -- 货架位置，如 A库-01-03
    remark       VARCHAR(512) NOT NULL DEFAULT '',
    creator      VARCHAR(64),
    create_time   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updater      VARCHAR(64),
    update_time   TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (tenant_id, part_no)
);

CREATE INDEX IF NOT EXISTS idx_eam_spare_part_name ON eam_spare_part(name);

-- ---------------------------------------------------------------------------
-- 表 6：备件出入库流水。库存变动的审计轨迹，biz_order_no 关联业务工单号
-- （维修工单/采购单），支撑"这批件用到哪台设备"的反查。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eam_spare_record (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL DEFAULT 1,
    part_id      BIGINT       NOT NULL REFERENCES eam_spare_part(id),
    type         SMALLINT     NOT NULL,                -- 1=入库 2=出库 3=退库
    quantity     INT          NOT NULL,                -- 数量（恒为正数，方向由 type 表达）
    biz_order_no VARCHAR(64),                          -- 关联业务工单号，如 RO-20261108-001
    operator     VARCHAR(64),                          -- 操作人
    remark       VARCHAR(512) NOT NULL DEFAULT '',
    creator      VARCHAR(64),
    create_time   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updater      VARCHAR(64),
    update_time   TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_eam_spare_record_part ON eam_spare_record(part_id);
CREATE INDEX IF NOT EXISTS idx_eam_spare_record_biz  ON eam_spare_record(biz_order_no);

-- ============================================================================
-- 种子数据
-- ============================================================================

-- 3 台设备：2 台机加在运行，1 台注塑机刚修完液压故障
INSERT INTO eam_asset (asset_no, name, model, category, location, status, purchase_date, warranty_until, responsible_user_id, attributes, remark)
VALUES ('EQ-CNC-001', '数控车床', 'CKA6150', '机加', '一车间-A线', 1, '2024-03-15', '2027-03-14', 1001,
        '{"power":"15kW","vendor":"沈阳机床","serialNo":"SY20240315"}'::jsonb, '主力车床'),
       ('EQ-CNC-002', '加工中心', 'VMC850', '机加', '一车间-B线', 1, '2023-11-02', '2026-11-01', 1001,
        '{"power":"22kW","vendor":"大连机床","serialNo":"DL20231102"}'::jsonb, ''),
       ('EQ-INJ-001', '注塑机', 'HTF160', '注塑', '二车间', 1, '2022-06-20', '2025-06-19', 1002,
        '{"power":"37kW","clampingForce":"160T"}'::jsonb, '上周液压故障已修复');

-- 2 个保养计划：月度（提前 3 天出工单）+ 季度（提前 7 天）
INSERT INTO eam_maintain_plan (plan_no, asset_id, plan_name, cycle_type, cycle_value, advance_days, last_maintain_time, next_maintain_time, status, remark)
VALUES ('MP-2026-001', 1, '数控车床月度保养', 3, 1, 3, '2026-10-10 09:00:00', '2026-11-10 09:00:00', 1, '导轨/丝杠润滑 + 精度校验'),
       ('MP-2026-002', 2, '加工中心季度保养', 3, 3, 7, '2026-08-15 14:00:00', '2026-11-15 14:00:00', 1, '主轴/刀库/冷却系统检查');

-- 2 条备件：轴承库存正常；密封件包 2 < 安全库存 5，演示低库存预警
INSERT INTO eam_spare_part (part_no, name, spec, unit, stock, safety_stock, asset_ids, location, remark)
VALUES ('SP-BEAR-001', '主轴轴承', 'SKF 7010C', '套', 8, 3, '[1,2]'::jsonb, 'A库-01-03', ''),
       ('SP-SEAL-002', '液压密封件包', 'HTF160 通用', '包', 2, 5, '[3]'::jsonb, 'A库-02-01', '低于安全库存，需补货');

-- 1 张已完成维修工单：注塑机液压故障，维修 4 小时（10:00 → 14:00）
INSERT INTO eam_repair_order (order_no, asset_id, fault_desc, priority, status, reporter, assignee_id, dispatch_time, start_time, finish_time, repair_note, accept_note, version)
VALUES ('RO-20261108-001', 3, '液压系统压力不足，合模无力', 3, 5, '张三', 1002,
        '2026-11-08 09:30:00', '2026-11-08 10:00:00', '2026-11-08 14:00:00',
        '更换液压泵密封件，压力恢复正常', '试机 2 小时无异常，验收通过', 2);

-- 维修领料流水：上述工单出库 1 包密封件（与 SP-SEAL-002 库存 2 对应，出库前为 3）
INSERT INTO eam_spare_record (part_id, type, quantity, biz_order_no, operator, remark)
VALUES (2, 2, 1, 'RO-20261108-001', '李四', '维修领料：更换液压泵密封件');
