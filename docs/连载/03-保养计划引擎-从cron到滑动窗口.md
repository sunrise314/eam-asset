# 保养计划引擎：从 cron 到滑动窗口

## 一、场景：每月 1 号早上 8 点，60 张工单同时砸下来

第 2 章把六张表建好之后，我做的第一件事是写保养提醒。那时候的想法很简单：保养是"每月一次"，那就用一个 cron，每月 1 号早上 8 点扫一遍，把到期的计划转成工单。Spring 里一行注解的事：

```java
@Scheduled(cron = "0 0 8 1 * ?")
public void scan() {
    // 扫全厂计划，该保养的出单
}
```

代码上线第一个月，11 月 1 日早上 8 点，李师傅的手机响了 60 多次。系统一次性生成了 60 多张保养工单，全厂 200 台设备里有三分之一"同时到期"。维修班只有 6 个人，一天最多干 8 台，这 60 张单排到月底也干不完。更荒唐的是，其中 7 台设备 10 月 28 号刚做完保养——因为那几台是 10 月初拖到月底才做的，cron 不管这些，它只知道"今天是 1 号"。

李师傅跑到设备科，把打印出来的工单往桌上一拍："王主任，你这系统是帮我排活还是帮我添乱？"

我当天下午就到现场，把问题拆开看，发现根子在一个语义错位上：**cron 表达的是"日历上的某个点"，而保养周期表达的是"距上次完成多久"。**

举个具体的例子。EQ-CNC-001 的月度保养，上一次是 2026 年 10 月 10 日 09:00 做完的，周期 1 个月。那么下一次应该是 11 月 10 日 09:00——这是"相对上次完成时间"的滑动窗口。如果 10 月那次拖到 10 月 15 日才做完，下一次就应该整体后移到 11 月 15 日。cron 做不到这件事，因为 cron 里没有"上次完成时间"这个变量，它只有"每月 1 号"这个日历常量。

这就是我用 `NextDateCalculator` + `MaintainPlanEngine` 替换 `@Scheduled` 的原因。不是 cron 不好，是 cron 的语义和保养的语义不是一回事。

## 二、为什么不用 Quartz / XXL-Job 做业务周期

有人会问：既然 `@Scheduled` 太简陋，那上 Quartz 或者 XXL-Job 行不行？它们有 `CalendarIntervalTrigger`，可以"每 30 天执行一次"。

不行。原因是这两个东西解决的不是同一个问题。

**调度框架解决的是"何时触发扫描"。** 它回答："每天凌晨 1 点，叫醒保养扫描任务。"这是基础设施层的问题，Quartz、XXL-Job、Spring `@Scheduled`、Kubernetes CronJob 都能干，选哪个看运维习惯。

**业务周期引擎解决的是"这条计划的下一期是哪天"。** 它回答："MP-2026-001 这条计划，上次 10 月 10 号做完，周期 1 个月，下一次是 11 月 10 号；今天 11 月 8 号，提前 3 天，该出单了。"这是领域层的问题，调度框架一概不管。

把这两件事混在一起，就会出现我第一版的那种翻车：用 cron 的"每月 1 号"去近似"每台设备各自的月度周期"，结果所有设备被拉到同一天，摩擦系数瞬间爆表。

正确的分工是这样的：

| 层 | 组件 | 职责 | 频率 |
|----|------|------|------|
| 调度层 | XXL-Job / `@Scheduled` | 每天凌晨叫醒扫描任务 | 每天 1 次 |
| 领域层 | `MaintainPlanEngine` | 算出哪些计划到期、下一期是哪天 | 每次扫描 |
| 数据层 | `eam_maintain_plan` | 存每台设备自己的 last/next | 持久化 |
| 执行层 | `eam_maintain_order` | 承接生成的工单 | 按需 |

Quartz 的 `CalendarIntervalTrigger` 看着像"每 30 天"，但它算的是"从第一次触发开始，每 30 天一次"，对**同一个触发器**有效。我们这里有 200 台设备、200 条计划、200 个不同的"上次完成时间"，需要 200 个独立的滑动基准。给每条计划建一个 Quartz Trigger？那是把业务数据塞进调度框架的元数据表，查询、修改、审计全都别扭，而且计划一改就要重建 Trigger。

所以我的结论是：**调度框架只负责"叫醒"，业务周期必须由领域代码自己算。** 这也是 README 取舍表里那条"保养周期表达用 (cycleType, cycleValue) 滑动窗口 + NextDateCalculator"的由来。

## 三、数据模型：一个二元组 + 一个基准时间

滑动窗口的数学模型其实很简单：给定一个基准时间 `base` 和一个周期 `(type, value)`，下一次就是 `base + value 个 type`。

- `type = DAYS`，`value = 30`：下次 = 基准 + 30 天。
- `type = WEEKS`，`value = 2`：下次 = 基准 + 2 周。
- `type = MONTHS`，`value = 1`：下次 = 基准 + 1 个月。

落到表上就是三个字段：`cycle_type`、`cycle_value`、`next_maintain_time`。其中 `next_maintain_time` 是"当前这一期的到期时间"，也是下一次计算的基准。每次扫描命中、生成工单之后，引擎把 `next_maintain_time` 往后推一个周期，同时把旧的 `next` 记到 `last_maintain_time`。

这里有个容易混淆的点：**基准到底是"计划日期"还是"实际完成日期"？** 理想语义是"实际完成日期"，因为保养周期说的是"距上次做完多久"。但演示版本用的是"计划日期"——引擎推进时，`base` 取的是旧的 `next_maintain_time`，不是工单的实际 `finish_time`。这个差异我在第六节会专门讲，先记住结论：当前实现是"计划滚动"，不是严格的"完成滚动"。

为什么用 `(cycle_type, cycle_value)` 而不是直接存一个 `interval_days`？因为"月"和"天"不是固定换算。1 个月可能是 28、29、30、31 天，用天数存"1 个月"会漂移；而 `plusMonths(1)` 会正确处理月末（1 月 31 日 + 1 个月 = 2 月 28 日或 29 日），这正是"每月保养"想要的自然月语义。同理，"每季度"用 `MONTHS × 3` 比 `DAYS × 90` 准确。

## 四、NextDateCalculator：四十行，三种周期

先看完整的计算器代码。这是整个引擎的地基，短，但每一行都有讲究：

```java
package cn.iocoder.yudao.module.eam.framework.eam.maintain;

import cn.iocoder.yudao.module.eam.enums.EamCycleType;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

/**
 * 保养周期下次日期计算器。
 *
 * 为什么不用 Cron 表达式：Cron 表达的是"日历定点"（每月 1 号、每周一），
 * 而设备保养周期是"相对上次完成时间"的滑动窗口——设备 1 月 5 号做完保养、周期 30 天，
 * 下次就是 2 月 4 号；若延期到 1 月 10 号才做完，下次也跟着滑到 2 月 9 号。
 * 用 Cron 会把全厂设备挤到固定日期扎堆保养，且无法表达"完成时间滑动"语义，
 * 所以这里用 (cycleType, cycleValue) 二元组 + 基准时间做 plus 运算。
 */
@UtilityClass
public class NextDateCalculator {

    /**
     * 计算下次保养时间。
     *
     * @param type       周期类型：按天/按周/按月
     * @param cycleValue 周期数值，必须为正整数
     * @param base       基准时间（通常是上次保养完成时间；首次为计划录入时间）
     * @return 下次保养时间
     */
    public static LocalDateTime next(EamCycleType type, int cycleValue, LocalDateTime base) {
        if (cycleValue <= 0) {
            throw new IllegalArgumentException("保养周期数值必须为正整数: " + cycleValue);
        }
        if (type == null || base == null) {
            throw new IllegalArgumentException("周期类型与基准时间不能为空");
        }
        switch (type) {
            case DAYS:
                return base.plusDays(cycleValue);
            case WEEKS:
                return base.plusWeeks(cycleValue);
            case MONTHS:
                return base.plusMonths(cycleValue);
            default:
                throw new IllegalArgumentException("未知保养周期类型: " + type);
        }
    }
}
```

这段代码值得逐点说。

**为什么用 `@UtilityClass`？** Lombok 的 `@UtilityClass` 会把类变成 final、构造器私有、所有成员静态。保养周期计算是纯函数，不需要任何状态，也不该被 Spring 管理。做成工具类，单测里直接 `NextDateCalculator.next(...)`，不用启动容器、不用 mock 任何东西。这是"把算法从框架里解放出来"的第一步。

**为什么参数校验放在最前面？** 因为这是一个会被批处理调用的函数。如果 `cycleValue = 0` 不拦，`plusDays(0)` 会返回原时间，引擎就会"每天扫描都命中、每天出单"，形成死循环式的重复工单。抛 `IllegalArgumentException` 是让脏数据在源头炸掉，而不是让它悄悄污染数据。注意：引擎在调用它之前还有一层过滤（`cycleValue <= 0` 直接跳过），两层防御，因为引擎面对的是数据库里的脏数据，不能假设调用方一定守规矩。

**`plusMonths` 的月末行为是有意的。** 假设基准是 2026 年 1 月 31 日，`plusMonths(1)` 返回 2026 年 2 月 28 日（平年），不是 3 月 3 日。Java 的 `LocalDateTime.plusMonths` 会把日期钳到目标月的最后一天。对保养来说这是对的：1 月底做的保养，2 月底再做一次，合理；如果钳到 3 月 3 日，就变成了"每 31 天"，反而失真。这个细节如果你自己用 `TimeUnit.DAYS` 换算，永远踩不到，但一旦用 `plusMonths`，必须知道它的语义。

**没有"按年"分支。** 年度保养用 `MONTHS × 12` 表达，不需要第四个枚举值。枚举值越少，数据库里的 code 越稳定，前端字典越好维护。

**没有"按运行小时"分支。** 这是个真实的缺口，第八节会用"空压机 500 小时保养"的例子专门讲。

## 五、MaintainPlanEngine：扫描、判定、出单、推进

计算器只解决"下一次是哪天"，引擎解决"今天该不该出单"。完整代码如下：

```java
package cn.iocoder.yudao.module.eam.framework.eam.maintain;

import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainOrderDO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainPlanDO;
import cn.iocoder.yudao.module.eam.enums.EamCycleType;
import cn.iocoder.yudao.module.eam.enums.EamMaintainOrderStatus;
import cn.iocoder.yudao.module.eam.enums.EamPlanStatus;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 保养计划引擎：扫描启用中的计划，对"该保养了"的计划生成保养工单。
 *
 * 触发条件：nextMaintainTime.toLocalDate() - advanceDays <= today，
 * 即"提前 N 天"窗口已进入——比如下周三该保养、提前 3 天，则本周日扫描就会出工单，
 * 给维修班留出领料/排人的缓冲。
 *
 * 纯函数式设计：只计算"要生成哪些工单"并就地推进计划的下一次时间，
 * 不落库（insert/update 由 Service 层在事务里完成），方便脱离 Spring 单测。
 * 单测时注入固定的 today 即可覆盖"窗口边界/停用计划/周期推进"等分支。
 */
@UtilityClass
public class MaintainPlanEngine {

    /** 工单号日期段格式：MO-yyyyMMdd-planId */
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 扫描计划列表，生成到期保养工单。
     *
     * 副作用：会就地修改入参 plan 的 lastMaintainTime / nextMaintainTime（推进到下一周期），
     * 调用方需把被推进过的 plan 一并 update 落库（可根据返回工单的 planId 集合过滤）。
     *
     * @param plans 全部保养计划（引擎内部再按"启用"过滤，双保险）
     * @param today 扫描日期（通常传 LocalDate.now()，测试时注入固定日期）
     * @return 本次扫描应生成的保养工单列表
     */
    public static List<EamMaintainOrderDO> scanAndGenerate(List<EamMaintainPlanDO> plans, LocalDate today) {
        List<EamMaintainOrderDO> orders = new ArrayList<>();
        for (EamMaintainPlanDO plan : plans) {
            // 1. 过滤：停用计划、未初始化下次时间的计划不参与扫描（引擎不替脏数据猜测）
            if (!EamPlanStatus.ENABLED.getCode().equals(plan.getStatus())) {
                continue;
            }
            if (plan.getNextMaintainTime() == null) {
                continue;
            }
            EamCycleType cycleType = cycleTypeOf(plan.getCycleType());
            if (cycleType == null || plan.getCycleValue() == null || plan.getCycleValue() <= 0) {
                continue; // 周期配置是脏数据：跳过这一条，不让它搞挂整次扫描
            }
            // 2. 判定：nextMaintainTime - advanceDays 是否已进入今天（提前窗口）
            int advanceDays = plan.getAdvanceDays() != null ? plan.getAdvanceDays() : 0;
            LocalDate triggerDate = plan.getNextMaintainTime().toLocalDate().minusDays(advanceDays);
            if (triggerDate.isAfter(today)) {
                continue; // 还没到出工单的时机
            }
            // 3. 生成工单：工单号 "MO-yyyyMMdd-planId"，同一天同一计划只会出一张
            EamMaintainOrderDO order = new EamMaintainOrderDO();
            order.setOrderNo("MO-" + today.format(ORDER_NO_DATE) + "-" + plan.getId());
            order.setPlanId(plan.getId());
            order.setAssetId(plan.getAssetId());
            order.setPlanDate(plan.getNextMaintainTime().toLocalDate());
            order.setStatus(EamMaintainOrderStatus.PENDING.getCode());
            orders.add(order);
            // 4. 推进计划：last=原 next，next=按周期滑到下一窗口
            LocalDateTime base = plan.getNextMaintainTime();
            plan.setLastMaintainTime(base);
            plan.setNextMaintainTime(NextDateCalculator.next(cycleType, plan.getCycleValue(), base));
        }
        return orders;
    }

    /** 按 code 找周期类型枚举；找不到返回 null（由调用方按脏数据跳过，而不是抛异常中断扫描）。 */
    private static EamCycleType cycleTypeOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (EamCycleType type : EamCycleType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
```

这段代码不长，但四个段落每一段都有"为什么这么写"。

### 5.1 第一段：过滤，坏一条跳过一条

三个 `continue`，对应三种"不该参与扫描"的计划：

- **停用的计划**：`status != ENABLED`。设备大修、封存、报废时，计划应该停用，否则会继续出单。
- **`nextMaintainTime` 为空的计划**：说明录入时没初始化，引擎不猜。如果引擎拿 `now()` 兜底，那就是替脏数据编造了一个到期日，后面所有的"提前几天"都失去意义。
- **周期脏数据**：`cycleType` 找不到、`cycleValue` 为空或小于等于 0。直接跳过。

注意这里是 `continue` 而不是 `throw`。批处理的第一原则是"坏一条，跳过一条，别搞挂一批"。200 台设备里有 1 条周期写错了，不应该让另外 199 台都不出单。这是个很朴素但很容易写错的地方——很多人的第一反应是"数据错了必须报错"，但在批处理里，报错意味着整批回滚，现场那天就没有保养工单了，损失比跳过一条大得多。

### 5.2 第二段：判定，提前窗口怎么算

```java
LocalDate triggerDate = plan.getNextMaintainTime().toLocalDate().minusDays(advanceDays);
if (triggerDate.isAfter(today)) {
    continue;
}
```

`triggerDate` 是"该出单的日子"，等于"下次保养日 - 提前天数"。只要 `triggerDate <= today`，就出单。

用种子数据算一遍：MP-2026-001 的 `next_maintain_time = 2026-11-10 09:00`，`advance_days = 3`，那么 `triggerDate = 2026-11-07`。如果今天是 11 月 7 日、8 日、9 日、10 日，都会出单；如果是 11 月 6 日，`11-07 > 11-06`，不出。

这里有个容易忽略的细节：**只取 `toLocalDate()`，丢掉时分秒。** 因为扫描是"按天"的，一天只跑一次，没必要精确到 9 点 00 分。如果保留时分秒，那么"11 月 7 日 08:00 扫描"会发现 `triggerDate = 11-07 09:00` 还没到，要等到 9 点以后才出单——同一个早上跑两次，结果不一样，这会让重跑和测试都很别扭。统一到日期粒度，行为就确定了。

### 5.3 第三段：出单，字段从计划带过来

工单号 `MO-yyyyMMdd-planId`，`planDate` 取 `nextMaintainTime` 的日期，`status` 初始为"待执行"，`assetId` 从计划带过来（第 2 章讲过，这是冗余字段的唯一可信来源）。

注意这里**没有设置 `content`**。保养内容在计划表里是 `remark`，工单表里有 `content`，但引擎没有搬过来。这是个已知的小缺口：理想情况下应该把计划的作业清单复制到工单，因为计划后来可能被改，而工单应该保留"当时要求做什么"。演示版本把它留给手工填写或后续扩展。

### 5.4 第四段：推进，last 和 next 就地改

```java
LocalDateTime base = plan.getNextMaintainTime();
plan.setLastMaintainTime(base);
plan.setNextMaintainTime(NextDateCalculator.next(cycleType, plan.getCycleValue(), base));
```

三行，做了一次"窗口滑动"：

- `base` 是**旧的 next**，也就是"这一期的计划日期"。
- `last = base`：把刚过去的这一期记为"上次保养时间"。
- `next = base + 周期`：下一期往后滑一个周期。

关键点：`base` 取的是旧 `next`，不是 `today`，也不是工单的实际完成时间。这意味着即使扫描晚了两天（比如 11 月 12 日才跑 11 月 10 日该跑的任务），下一期仍然是 12 月 10 日，不会被扫描延迟"推着走"。这个设计是对的——**扫描延迟是运维问题，不应该改变计划的节奏**。如果拿 `today` 当基准，那么每次运维晚跑一天，全厂设备的保养日期就整体后移一天，一年下来能漂移十几天。

## 六、为什么引擎是纯函数，不落库

`MaintainPlanEngine.scanAndGenerate` 有一个很特别的地方：它接收的是 `List<EamMaintainPlanDO>`，返回的是 `List<EamMaintainOrderDO>`，中间**不碰数据库**。它只做两件事：算出要生成哪些工单、把入参 plan 的 last/next 就地改掉。

这个设计有三个好处。

**第一，可以脱离 Spring 单测。** 引擎不依赖 Mapper、不依赖事务、不依赖 `LocalDate.now()`（today 是入参）。测试"窗口边界"只需要构造两个 plan、传一个固定的 today：

```java
LocalDate today = LocalDate.of(2026, 11, 10);
List<EamMaintainOrderDO> orders = MaintainPlanEngine.scanAndGenerate(plans, today);
assertEquals(2, orders.size());
assertEquals("MO-20261110-1", orders.get(0).getOrderNo());
```

不需要 `@SpringBootTest`，跑一次几十毫秒。这是"纯函数"最直接的价值：**边界条件可以穷举，不用起环境。**

**第二，事务边界清晰。** 引擎不知道事务的存在，Service 层负责把"插工单 + 改计划"包在一个事务里。如果落库中途失败，事务回滚，plan 的内存修改也随之丢弃（因为没有被持久化），下次扫描重新来过，不会出现"工单插了、计划没推进"的半截状态。

**第三，可以重放。** 同样的 plans + 同样的 today，永远得到同样的结果。没有隐藏的 `now()`、没有随机数、没有数据库读。这对排查问题极其重要——现场说"今天少出了一张单"，你可以把当天的计划快照拿出来，传进去重跑，立刻知道是引擎算错了还是数据不对。

落库的部分在 Service：

```java
// service/maintainplan/EamMaintainPlanServiceImpl.java（节选）
@Override
@Transactional(rollbackFor = Exception.class)
public int generateOrders() {
    // 1. 捞启用中的计划（引擎内部会再过滤一次，双保险）
    List<EamMaintainPlanDO> plans = maintainPlanMapper.selectListByStatus(EamPlanStatus.ENABLED.getCode());
    if (plans.isEmpty()) {
        return 0;
    }
    // 2. 引擎纯函数计算：返回工单列表，并就地推进命中计划的 last/next 时间
    List<EamMaintainOrderDO> orders = MaintainPlanEngine.scanAndGenerate(plans, LocalDate.now());
    // 3. 落库：批量插工单 + 只更新被推进过的计划（未命中的计划不动，避免无效 UPDATE）
    Set<Long> advancedPlanIds = new HashSet<>();
    for (EamMaintainOrderDO order : orders) {
        maintainOrderMapper.insert(order);
        advancedPlanIds.add(order.getPlanId());
    }
    for (EamMaintainPlanDO plan : plans) {
        if (advancedPlanIds.contains(plan.getId())) {
            maintainPlanMapper.updateById(plan);
        }
    }
    return orders.size();
}
```

这段代码有两个值得学的细节。

**细节一：`advancedPlanIds` 过滤。** 引擎会把所有 plan 都遍历一遍，但只有命中的 plan 的 last/next 被改了。如果无脑 `for (plan : plans) updateById(plan)`，那些没命中的 plan 也会被 UPDATE 一次，`update_time` 全部刷新，审计日志里全是无意义的变更。用一个 `Set<Long>` 收集被推进的 planId，只更新它们，干净。

**细节二：`updateById(plan)` 传的是整单。** 因为 plan 是从数据库查出来的，带 `id`，MyBatis-Plus 会按 id 更新非空字段。这里没有乐观锁（`eam_maintain_plan` 没有 `version` 字段），所以并发扫描是有风险的——两个线程同时读到旧 next，都命中，都插工单，都推进。这个问题第 8 章讲，这里先记住：**纯函数解决了"算得对"，解决不了"并发下只算一次"。**

Controller 只有一个手动触发入口：

```java
// controller/admin/maintainplan/EamMaintainPlanController.java（节选）
@PostMapping("/generate-orders")
@Operation(summary = "扫描启用计划，生成到期保养工单")
public CommonResult<Integer> generateOrders() {
    return success(maintainPlanService.generateOrders());
}
```

`POST /api/admin/eam/maintain-plan/generate-orders`，返回本次生成的工单数量。演示阶段手动点，商用阶段挂 XXL-Job 每天凌晨调一次——调度层和领域层的分工，第二节已经讲过了。

## 七、工单号规则与幂等：为什么重复扫描不会重复出单

工单号是 `MO-yyyyMMdd-planId`，比如 `MO-20261110-1`。这个规则有三个好处：一眼看出扫描日期、一眼看出计划来源、天然带日期分区属性。

但它也带来一个必须回答的问题：**如果同一天扫描两次，会不会出两张单？**

答案是：正常情况下不会。因为第一次扫描命中后，`next_maintain_time` 已经推进到下一期，第二次扫描时 `triggerDate` 落在未来，`isAfter(today)` 成立，直接 `continue`。这是"状态推进"带来的天然幂等——**幂等不是靠去重表，是靠"做过的事留下痕迹"**。

但"正常情况"之外，还有几种边界，必须逐个想清楚：

**边界一：扫描事务失败回滚。** 工单插入和计划推进在同一个事务里，要么都成功，要么都回滚。回滚后 next 还是旧的，下次扫描会重新命中、重新出单。不会漏，也不会重（因为上一次的工单也没插进去）。这是事务带来的正确性。

**边界二：跨天补跑。** 11 月 10 日该跑没跑，11 月 11 日才跑。`today = 11-11`，`triggerDate = 11-07 <= 11-11`，命中，工单号变成 `MO-20261111-1`（不是 `MO-20261110-1`）。`planDate` 仍然是 `11-10`，next 推进到 `12-10`。结果是：工单晚了一天，但计划节奏没乱。这个行为是合理的——工单号记录"什么时候生成的"，`planDate` 记录"本该什么时候做"。

**边界三：同一天并发两次扫描。** 两个线程同时读到旧 next，都命中，都生成 `MO-20261110-1`。插入时 `UNIQUE (tenant_id, order_no)` 会让第二个失败，事务回滚。但如果两个线程都成功插入了不同的工单（比如一个读到旧数据、一个读到新数据），就可能出两张。这种情况需要分布式锁，或者把"读计划"改成 `SELECT ... FOR UPDATE`。演示版本没做，第 8 章讲。

**边界四：工单被手工删除后再扫描。** next 已经推进了，不会补单。这是"状态推进"的副作用：它假设"出过单 = 这件事已经安排了"。如果工单被误删，需要人工补录，系统不会自动重出。这个取舍可以接受——自动补单反而可能和人工补录打架。

**边界五：计划被改成停用又启用。** 停用期间不扫描，next 停留在停用前的值。重新启用后，如果 next 已经过去很久，会立刻命中，出一张"迟到很久"的单。这是符合预期的：停用不等于"这笔保养不用做了"。

所以工单号规则本身不保证幂等，**真正保证幂等的是"推进 next + 唯一键兜底"这一对组合**。工单号只是让重复出单在数据库层"撞车"时更容易被发现。

## 八、边界场景：五个必须先想清楚的问题

### 8.1 停机设备要不要保养？

当前引擎**不看设备状态**，只看计划状态。也就是说，一台设备停机了，只要它的保养计划还是"启用"，照样出单。

这个行为对不对？要看停机的原因：

- **没订单、等料导致的短期停机**：应该保养，因为机器停了更容易受潮、生锈、油液沉淀。
- **故障待修导致的停机**：不该出保养单，因为修都修不好，保养没意义。
- **长期封存**：应该停用计划，否则每个月出一张没人干的单。
- **已报废**：必须停用计划。

所以正确的做法不是"引擎过滤停机设备"，而是"设备状态变化时，联动停用/恢复计划"。演示版本没做这个联动，只在 README 的检查清单里留了一条"设备报废后关联计划是否自动停用"。这是个典型的"引擎不该知道的业务规则"——引擎只管周期，设备该不该保养是设备科的判断。**把业务判断塞进引擎，引擎就会越来越重，最后变成一个什么都管、什么都管不好的上帝类。**

### 8.2 计划改了周期怎么处理？

`updateMaintainPlan` 会直接改 `cycleType`、`cycleValue`、`advanceDays`，但**不会重算 `nextMaintainTime`**。

这意味着：如果一条计划原来是"1 个月"，next 是 11 月 10 日，现在改成"3 个月"，next 仍然是 11 月 10 日；等 11 月 10 日这一期做完，next 才会按 3 个月推进到 2 月 10 日。

这个行为是合理的——改周期不应该让"已经排好的这一期"凭空跳走。但有个坑：如果用户改周期时**同时**改了 next（前端表单里 next 是可编辑的），那就要小心，因为两者会一起生效，可能推出一个谁也没预期的日期。商用版本应该在更新时做校验：如果 next 早于今天，提示"这条计划已经逾期"；如果新周期小于已过去的时间，提示"会产生补课式出单"。

### 8.3 扫描失败重跑会不会重复出单？

不会，前提是事务生效。`generateOrders()` 上有 `@Transactional(rollbackFor = Exception.class)`，插工单和改计划同生共死。失败回滚后，next 没变，重跑会重新命中、重新出单。

但要注意一个陷阱：如果异常被 `catch` 吞掉了（比如在循环里 `try-catch` 每条工单），事务不会回滚，已经插入的工单会留下，而 next 可能没推进——下一次扫描就会重复出单。所以**批处理里不要随便 catch**，要么让异常抛出去触发整体回滚，要么用"幂等键 + 唯一索引"兜底。

### 8.4 逾期很久的计划会怎样？

假设一条计划的 next 停在 2026 年 8 月 1 日，今天是 11 月 10 日，周期 1 个月。引擎会怎么走？

第一次扫描：命中，生成 `MO-20261110-1`（planDate = 08-01），next 推进到 09-01。
第二次扫描（第二天）：`triggerDate = 09-01 - 3 = 08-29 <= 11-11`，又命中，生成 `MO-20261111-1`（planDate = 09-01），next 推进到 10-01。
第三次扫描：再出一张……
要连出 4 张单，才能把 8、9、10、11 四期补齐。

这个行为叫"补课"，它不一定是你想要的。如果设备已经三个月没保养，正确做法可能是"只做最近一期 + 一次全面检查"，而不是把三个月的单补出来让维修班挨个签字。商用版本应该加一个策略：逾期超过 N 期时，只生成一张"补做"工单，并在内容里注明"逾期 X 期"。当前引擎没做，这是个已知缺口。

### 8.5 `advanceDays` 大于等于周期会怎样？

假设周期是 3 天，提前 5 天出单。第一次扫描命中，next 推进 3 天；第二天扫描，新的 `triggerDate = next - 5`，因为 5 > 3，`triggerDate` 仍然小于今天，又命中。结果是**每天出一张单**，直到有人把计划停掉。

这是个配置错误，但系统不该让它悄悄发生。商用版本应该在录入/更新时校验 `advanceDays < cycle 折算天数`，或者在引擎里加一道保护：如果 `advanceDays >= cycleDays`，按 `cycleDays - 1` 处理。当前没有校验，属于"参数组合能造出坏行为"的典型。

## 九、一个手算例子：空压机 500 小时保养，今天该不该出单

现在回到一个真实的场景。三车间有一台空压机，厂家手册写的是"每运行 500 小时保养一次"，内容是全合成油更换、油气分离器检查、皮带张紧。设备科想把它放进系统，问我怎么配。

这里立刻撞上一个模型缺口：`EamCycleType` 只有按天、按周、按月，没有"按运行小时"。500 小时是**运行时长**，不是日历时间。一台两班倒的空压机，500 小时可能 1 个月就到了；一台偶尔开机的空压机，500 小时可能半年才到。用日历近似，一定会不准。

演示版本的折中办法是"按时钟折算"：这台空压机每天运行 8 小时，500 ÷ 8 = 62.5 天，向上取整 63 天。所以配一条 `cycle_type = DAYS`、`cycle_value = 63` 的计划，提前 3 天出单。

假设上次保养是 2026 年 9 月 11 日 08:00，那么：

| 项 | 值 | 算法 |
|----|----|------|
| 上次完成 | 2026-09-11 08:00 | 手工录入 |
| 周期 | 63 天 | 500 ÷ 8 = 62.5，向上取整 |
| 下次到期 | 2026-11-13 08:00 | 09-11 + 63 天 |
| 提前天数 | 3 天 | 给维修班留缓冲 |
| 出单触发日 | 2026-11-10 | 11-13 - 3 天 |

今天如果是 2026 年 11 月 10 日，`triggerDate = 11-10`，`isAfter(today)` 为 false，**该出单**。生成的工单是 `MO-20261110-<planId>`，`planDate = 2026-11-13`。出单后 next 推进到 2027-01-15 08:00（11-13 + 63 天）。

如果今天是 11 月 9 日，`triggerDate = 11-10 > 11-09`，**不出单**，等到明天。

如果扫描拖到 11 月 15 日才跑，仍然会出单（`11-10 <= 11-15`），但 `planDate` 还是 11-13，next 还是推到 2027-01-15——扫描晚了，计划节奏不变。

这个例子的价值在于，它把"折算误差"暴露出来了：63 天是按"每天 8 小时"折算的，如果这台空压机某段时间两班倒，实际运行小时早就超过 500 了，但日历还没到 63 天。反过来，如果它停产了半个月，63 天到了但只运行了 380 小时，保养就做早了。

真正的解法是接 SCADA 的累计运行小时。芋道的 SCADA 模块（我另一个项目）里有设备运行时长，EAM 可以：

1. 给 `eam_maintain_plan` 加一个 `cycle_type = 4`（按运行小时），`cycle_value = 500`；
2. 建一张 `eam_asset_runtime` 表，存每台设备的累计运行小时和上次保养时的读数；
3. 引擎判定时，改成"当前累计小时 - 上次保养读数 >= 500"；
4. 这样就不需要 `next_maintain_time` 了，或者说 `next_maintain_time` 变成"预估时间"，只用于展示。

这就是 EAM 和 SCADA 的边界：SCADA 提供"读数"，EAM 消费"读数"做业务判断。当前的演示版本没有这条链路，所以只能用日历近似。**承认模型的边界，比假装它能覆盖一切更重要。**

## 十、怎么验证：用种子数据跑一遍

`eam-init.sql` 里的两条计划，正好可以在 2026 年 11 月 10 日这一天验证：

| 计划 | next | advance | triggerDate | 今天 11-10 是否出单 |
|------|------|---------|-------------|-------------------|
| MP-2026-001（数控车床月度） | 2026-11-10 09:00 | 3 | 2026-11-07 | 是 |
| MP-2026-002（加工中心季度） | 2026-11-15 14:00 | 7 | 2026-11-08 | 是 |

两条都命中，`POST /api/admin/eam/maintain-plan/generate-orders` 返回 2，生成 `MO-20261110-1` 和 `MO-20261110-2`。计划推进后：

- MP-2026-001：last = 2026-11-10 09:00，next = 2026-12-10 09:00。
- MP-2026-002：last = 2026-11-15 14:00，next = 2027-02-15 14:00（`plusMonths(3)`）。

注意 MP-2026-002 的 last 被设成了 11-15，而今天是 11-10——"上次保养时间"比"今天"还晚 5 天。这看起来别扭，但它是"计划口径"的必然结果：这一期的计划日期是 11-15，工单已经提前出了，计划认为"这一期已经安排"，所以 last 记 11-15。这也再次说明：**当前实现记的是"计划日期"，不是"实际完成日期"。**

要验证边界，可以写一组纯函数单测，不需要数据库：

- `today = 2026-11-06`，两条都不出单（trigger 分别是 11-07、11-08）。
- `today = 2026-11-07`，只出 MP-2026-001。
- `status = 0`（停用），跳过。
- `nextMaintainTime = null`，跳过。
- `cycleValue = 0`，跳过，且不抛异常。
- `cycleValue = -1`，跳过（引擎层过滤），`NextDateCalculator` 本身会抛 `IllegalArgumentException`（第二层防御）。

这六个用例覆盖了引擎的全部分支，加起来不到 50 行测试代码。这就是把引擎做成纯函数的好处——**边界条件可以穷举，不用起数据库、不用等调度。**

## 十一、商用化检查清单（保养引擎版）

- 扫描是否接了调度？当前是手动 `POST /generate-orders`，商用应挂 XXL-Job 每日凌晨执行。
- 并发扫描是否有分布式锁？两个实例同时跑会重复出单，唯一键只能兜住一部分。
- `advanceDays >= 周期天数` 是否有校验？没有的话会每天出单。
- 逾期多期的计划，是"补课式"逐期出单，还是只出一张"补做"单？当前是前者。
- `nextMaintainTime` 的初始化规则是否写进录入规范？引擎不猜，录入侧必须给值。
- 设备报废/长期封存时，计划是否自动停用？
- 周期改动后，`nextMaintainTime` 是否要重算？当前不重算，靠下一期自然生效。
- "按运行小时"的保养怎么支持？需要 SCADA 累计时长或独立的运行时长表。
- 扫描用的 `LocalDate.now()` 是服务器时区，容器如果是 UTC，会出现"差一天"。
- 工单号 `MO-yyyyMMdd-planId` 是否有唯一索引兜底？`UNIQUE (tenant_id, order_no)` 必须有。
- 工单的 `content` 是否从计划复制？当前没复制，计划改了历史工单不会跟着变（这其实是对的，但要有意为之）。
- 扫描失败时异常是否被吞？批处理里 catch 住异常会导致"工单插了、计划没推进"。

## 十二、下一章预告：维修工单的六状态与合法迁移

保养引擎解决的是"计划到点自动派工"，第二个动作是"维修状态合法流转"。

这一章会讲一张被改回"待派工"的工单是怎么把 MTBF 算废的：李师傅修完注塑机，徒弟在后台点了完工，另一个调度员又把状态改回"待派工"重新派给别人，结果 `start_time` 被覆盖、`finish_time` 还在，时间线成了负数。月底算 MTTR，算出来一个"负 3 小时"。

下一章我会把 `RepairOrderStateMachine` 的迁移表拆开讲：六个状态、五条迁移边、为什么"待验收"不允许取消、为什么不用 Spring StateMachine、`assertTransit` 怎么在 Service 每个方法的第一行拦住非法流转、`@Version` 乐观锁怎么和状态机形成双保险，以及一张完整工单的生命周期流水长什么样。
