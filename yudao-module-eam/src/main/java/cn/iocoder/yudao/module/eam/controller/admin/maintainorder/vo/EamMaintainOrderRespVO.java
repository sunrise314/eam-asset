package cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - EAM 保养工单 Response VO")
public record EamMaintainOrderRespVO(

        @Schema(description = "工单 ID", example = "1")
        Long id,

        @Schema(description = "工单号", example = "MO-20261110-1")
        String orderNo,

        @Schema(description = "保养计划 ID", example = "1")
        Long planId,

        @Schema(description = "设备 ID", example = "1")
        Long assetId,

        @Schema(description = "计划保养日期")
        LocalDate planDate,

        @Schema(description = "状态: 1=待执行 2=执行中 3=已完成 4=已取消", example = "1")
        Integer status,

        @Schema(description = "执行人用户 ID", example = "1002")
        Long assigneeId,

        @Schema(description = "实际开始时间")
        LocalDateTime startTime,

        @Schema(description = "实际完成时间")
        LocalDateTime finishTime,

        @Schema(description = "保养内容")
        String content,

        @Schema(description = "执行结果记录")
        String resultNote,

        @Schema(description = "乐观锁版本号", example = "0")
        Integer version,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
