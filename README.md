# eam-asset — EAM 设备资产管理系统

基于芋道 ruoyi-vue-pro 二次开发，覆盖中小制造工厂 **设备台账 + 保养计划 + 维修工单 + 备件库存** 场景。

## 定位

EAM（Enterprise Asset Management，设备资产管理）是生产车间里的"设备管家"——每台设备从采购入册开始建台账，日常按周期保养、坏了走维修工单闭环，换件从备件库存扣减并留流水，最后用 MTBF/MTTR 回答"这台设备到底靠不靠谱"。

差异化核心是四个机制：

- **保养计划引擎**：周期是"相对上次完成时间"的滑动窗口（不是 Cron 日历定点），扫描到期的启用计划自动生成保养工单
- **维修工单状态机**：Map 驱动的 6 状态迁移（待派工→已派工→维修中→待验收→已完成/已取消），非法流转直接拒绝
- **MTBF/MTTR 可靠性指标**：纯函数计算，0 故障返回 null 而非除零，前端显示 "—"
- **备件库存**：出入库写流水 + 安全库存预警，一个备件可挂多台适用设备（JSONB）

## 核心取舍

| 取舍 | 决策 | 理由 |
|------|------|------|
| 保养周期表达 | (cycleType, cycleValue) 滑动窗口 + NextDateCalculator | 保养语义是"距上次完成 N 天/周/月"，Cron 是日历定点会让所有设备扎堆保养，也无法跟随延期滑动 |
| 维修流转 | Map<Integer, Set<Integer>> 状态机 | 6 个状态 5 条迁移边，Spring StateMachine 的配置/持久化/监听器是杀鸡用牛刀 |
| 工单生成 | MaintainPlanEngine 纯函数，不落库 | 计算与落库分离，引擎可脱离 Spring 单测；写库由 Service 层在事务里完成 |
| 备件扣减 | 先读后写 + 出入库流水 | 演示级实现；商用高并发改原子 SQL `UPDATE ... SET stock = stock - ? WHERE stock >= ?` |
| 可靠性指标 | MtbfCalculator 随查随算，不落库 | 统计窗口灵活（月/季/年任选），指标表反而限制口径 |
| 设备扩展属性 | JSONB attributes / 备件适用设备 JSONB asset_ids | 避免为每个行业加字段、为每件备件建关联表 |
| 数据模型 | 6 张核心表 | 台账/保养计划/保养工单/维修工单/备件/流水，无多余关联表 |

## 代码结构

```
yudao-module-eam/
├── framework/eam/
│   ├── maintain/        保养周期引擎（NextDateCalculator 滑动计算 + MaintainPlanEngine 扫描生成工单）
│   ├── repair/          维修工单状态机（Map 驱动迁移表 + assertTransit 校验）
│   └── metrics/         可靠性指标（MtbfCalculator → MetricsResult: MTBF/MTTR）
├── service/             6 组 Service（台账/保养计划/保养工单/维修工单/备件/流水）
├── dal/                 MyBatis-Plus Mapper + DO
├── controller/admin/    5 个 Controller（台账/保养计划/保养工单/维修工单/备件）
└── enums/               7 个业务枚举
```

## 快速开始

```bash
# 1. 建表 + 种子数据
psql -d your_db -f docs/sql/eam-init.sql

# 2. 扫描启用计划，生成到期保养工单（返回生成数量）
# POST /api/admin/eam/maintain-plan/generate-orders

# 3. 维修工单流转：派工 → 开工 → 完工 → 验收
# PUT /api/admin/eam/repair-order/dispatch?id=1&assigneeId=1002
# PUT /api/admin/eam/repair-order/start?id=1
# PUT /api/admin/eam/repair-order/finish?id=1&repairNote=更换液压泵密封件
# PUT /api/admin/eam/repair-order/accept?id=1&acceptNote=试机2小时无异常

# 4. 备件出库（库存不足直接报错，低于安全库存由返回值带出预警）
# POST /api/admin/eam/spare-part/outbound?partId=1&quantity=2&bizOrderNo=RO-20261108-001&operator=张三

# 5. 设备可靠性指标（MTBF/MTTR，单位小时）
# GET /api/admin/eam/repair-order/metrics?assetId=3&windowStart=2026-10-01T00:00:00&windowEnd=2026-11-01T00:00:00
```

## 商用化检查清单

- [ ] 备件扣减是否改原子 SQL（`stock >= ?` 条件更新）？当前先读后写只适用于演示，并发扣减会超卖
- [ ] 保养扫描是否接了调度？当前是手动触发 generate-orders，商用应挂 XXL-Job/Quartz 每日凌晨扫描
- [ ] 工单号 "MO-yyyyMMdd-planId" 是否有唯一索引兜底？引擎推进 nextMaintainTime 后同日不会重复命中，但脏数据/手工初始化需在插入侧兜底
- [ ] MTBF 窗口内未闭环工单是否纳入？当前只统计 DONE，进行中的工单会低估维修时长
- [ ] 维修验收后设备 REPAIRING→RUNNING 联动如何实现？当前只在注释说明、未跨 Service 调用，商用建议领域事件解耦
- [ ] 保养计划 nextMaintainTime 初始化时机是否明确？当前由录入时给定，引擎不替脏数据猜测

## 依赖

- Java 8+（yudao 父工程 jdk8-snapshot）
- cn.iocoder.boot:yudao-spring-boot-starter-biz-*（Web/Security/MyBatis/Tenant）
- 无第三方算法依赖（保养周期/状态机/MTBF 均纯 Java 实现）

## License

LGPL-3.0（与 ruoyi-vue-pro 保持一致，商用友好）
