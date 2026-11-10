package cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - EAM 维修工单 Response VO")
public record EamRepairOrderRespVO(

        @Schema(description = "工单 ID", example = "1")
        Long id,

        @Schema(description = "工单号", example = "RO-20261108-001")
        String orderNo,

        @Schema(description = "设备 ID", example = "3")
        Long assetId,

        @Schema(description = "故障描述")
        String faultDesc,

        @Schema(description = "优先级: 1=低 2=中 3=高 4=紧急", example = "3")
        Integer priority,

        @Schema(description = "状态: 1=待派工 2=已派工 3=维修中 4=待验收 5=已完成 6=已取消", example = "5")
        Integer status,

        @Schema(description = "报修人", example = "张三")
        String reporter,

        @Schema(description = "维修人用户 ID", example = "1002")
        Long assigneeId,

        @Schema(description = "派工时间")
        LocalDateTime dispatchTime,

        @Schema(description = "维修开始时间")
        LocalDateTime startTime,

        @Schema(description = "维修完成时间")
        LocalDateTime finishTime,

        @Schema(description = "维修记录")
        String repairNote,

        @Schema(description = "验收记录")
        String acceptNote,

        @Schema(description = "乐观锁版本号", example = "2")
        Integer version,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
