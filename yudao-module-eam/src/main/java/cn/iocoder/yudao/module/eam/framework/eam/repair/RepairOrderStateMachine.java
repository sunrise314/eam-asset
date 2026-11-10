package cn.iocoder.yudao.module.eam.framework.eam.repair;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.eam.enums.EamRepairStatus;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Set;

/**
 * 维修工单状态机：定义"待派工→已派工→维修中→待验收→已完成 / 已取消"的合法迁移。
 *
 * 为什么不用 Spring StateMachine：Spring StateMachine 面向"事件-动作-守卫"的复杂场景，
 * 配置/持久化/监听器一套下来要引入数个大件；维修工单一共 6 个状态、5 条迁移边，
 * 一张不可变 Map 就足以表达，且 canTransit 是纯查表——单测无需启动状态机容器。
 * 杀鸡不用牛刀：10 个状态以内 Map 驱动足够，状态再膨胀（加驳回/挂起/升级）时再评估升级。
 *
 * 迁移语义说明：
 * - 待派工/已派工/维修中 三个阶段允许取消（客户误报、备件不到位）
 * - 待验收不允许取消：活已经干完，有争议应走验收驳回而不是抹掉工单（演示仓库未展开驳回边）
 * - 已完成/已取消是终态，无任何出边
 */
@UtilityClass
public class RepairOrderStateMachine {

    /** 非法状态流转错误码（对外暴露，Service 层与全局异常处理统一文案） */
    public static final ErrorCode ERROR_CODE_ILLEGAL_TRANSITION =
            new ErrorCode(1_003_401, "维修工单状态流转非法: {} -> {}");

    /** 合法迁移表：key=源状态，value=允许到达的目标状态集合；5/6 为终态，无出边。 */
    private static final Map<Integer, Set<Integer>> TRANSITIONS = Map.of(
            EamRepairStatus.PENDING_DISPATCH.getCode(),
                    Set.of(EamRepairStatus.DISPATCHED.getCode(), EamRepairStatus.CANCELLED.getCode()),
            EamRepairStatus.DISPATCHED.getCode(),
                    Set.of(EamRepairStatus.REPAIRING.getCode(), EamRepairStatus.CANCELLED.getCode()),
            EamRepairStatus.REPAIRING.getCode(),
                    Set.of(EamRepairStatus.PENDING_ACCEPT.getCode(), EamRepairStatus.CANCELLED.getCode()),
            EamRepairStatus.PENDING_ACCEPT.getCode(),
                    Set.of(EamRepairStatus.DONE.getCode()),
            EamRepairStatus.DONE.getCode(), Set.of(),
            EamRepairStatus.CANCELLED.getCode(), Set.of()
    );

    /**
     * 判断是否允许从 from 流转到 to。
     *
     * @param from 源状态（当前工单状态）
     * @param to   目标状态
     * @return true=合法迁移
     */
    public static boolean canTransit(Integer from, Integer to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * 断言 from -> to 是合法迁移，非法直接抛业务异常（ErrorCode 1_003_401）。
     * Service 层每个流转方法的第一行都应调用它。
     */
    public static void assertTransit(Integer from, Integer to) {
        if (!canTransit(from, to)) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_ILLEGAL_TRANSITION, from, to);
        }
    }
}
