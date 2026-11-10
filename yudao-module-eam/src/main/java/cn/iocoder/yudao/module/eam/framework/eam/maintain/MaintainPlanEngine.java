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
