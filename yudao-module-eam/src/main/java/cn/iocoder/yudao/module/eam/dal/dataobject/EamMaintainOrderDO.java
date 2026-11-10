package cn.iocoder.yudao.module.eam.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EAM 保养工单。由保养计划到期自动生成（MaintainPlanEngine），也可手工补录。
 *
 * version 乐观锁：工单可能在移动端/PC 端同时打开执行页，
 * 开始/完成提交用版本号防并发覆盖，后到请求直接失败重试。
 */
@Data
@TableName("eam_maintain_order")
public class EamMaintainOrderDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 工单号，如 MO-20261110-1（MO-扫描日期-计划ID） */
    private String orderNo;

    /** 来源保养计划 ID（手工补录可空） */
    private Long planId;

    /** 保养对象设备 ID */
    private Long assetId;

    /** 计划保养日期（= 计划的 nextMaintainTime 当天） */
    private LocalDate planDate;

    /** 1=待执行 2=执行中 3=已完成 4=已取消 */
    private Integer status;

    /** 执行人用户 ID */
    private Long assigneeId;

    /** 实际开始时间 */
    private LocalDateTime startTime;

    /** 实际完成时间 */
    private LocalDateTime finishTime;

    /** 保养内容（从计划带过来的作业清单） */
    private String content;

    /** 执行结果记录（完成时回填） */
    private String resultNote;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
