package cn.iocoder.yudao.module.eam.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * EAM 维修工单。设备故障的闭环载体：报修→派工→维修→验收→完成。
 *
 * 状态迁移合法性由 RepairOrderStateMachine 校验；startTime/finishTime 之间
 * 是"维修时长"，MTBF/MTTR 指标只统计 status=DONE 的工单。
 * version 乐观锁：派工/完工并发提交防覆盖。
 */
@Data
@TableName("eam_repair_order")
public class EamRepairOrderDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 工单号，如 RO-20261108-001 */
    private String orderNo;

    /** 故障设备 ID */
    private Long assetId;

    /** 故障描述 */
    private String faultDesc;

    /** 优先级：1=低 2=中 3=高 4=紧急 */
    private Integer priority;

    /** 状态：1=待派工 2=已派工 3=维修中 4=待验收 5=已完成 6=已取消 */
    private Integer status;

    /** 报修人 */
    private String reporter;

    /** 维修人用户 ID（派工时指定） */
    private Long assigneeId;

    /** 派工时间 */
    private LocalDateTime dispatchTime;

    /** 维修开始时间 */
    private LocalDateTime startTime;

    /** 维修完成时间 */
    private LocalDateTime finishTime;

    /** 维修记录（完工时回填） */
    private String repairNote;

    /** 验收记录（验收时回填） */
    private String acceptNote;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
