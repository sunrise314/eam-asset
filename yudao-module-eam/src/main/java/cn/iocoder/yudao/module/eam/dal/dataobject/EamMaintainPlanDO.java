package cn.iocoder.yudao.module.eam.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * EAM 保养计划。定义"哪台设备、按什么周期、提前几天"自动生成保养工单。
 *
 * 周期是"相对上次完成时间"的滑动窗口（cycleType × cycleValue），不是 Cron 日历定点；
 * 每次 MaintainPlanEngine 扫描命中后，用 NextDateCalculator 把 nextMaintainTime 推进到下一窗口。
 */
@Data
@TableName("eam_maintain_plan")
public class EamMaintainPlanDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 计划编号，租户内唯一，如 MP-2026-001 */
    private String planNo;

    /** 保养对象设备 ID */
    private Long assetId;

    /** 计划名称，如 数控车床月度保养 */
    private String planName;

    /** 周期类型：1=按天数 2=按周数 3=按月数 */
    private Integer cycleType;

    /** 周期数值，与 cycleType 组成滑动窗口，如 1 个月 */
    private Integer cycleValue;

    /** 提前几天生成工单（给维修班留领料/排人的缓冲），默认 0 */
    private Integer advanceDays;

    /** 上次保养时间（扫描命中后由引擎回填） */
    private LocalDateTime lastMaintainTime;

    /** 下次保养时间（录入时初始化，之后由引擎推进） */
    private LocalDateTime nextMaintainTime;

    /** 0=停用 1=启用；停用的计划不参与扫描 */
    private Integer status;

    private String remark;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
