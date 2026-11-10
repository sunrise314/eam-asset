package cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保养计划分页 Request VO。
 */
@Schema(description = "管理后台 - EAM 保养计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EamMaintainPlanPageReqVO extends PageParam {

    @Schema(description = "计划编号", example = "MP-2026-001")
    private String planNo;

    @Schema(description = "设备 ID", example = "1")
    private Long assetId;

    @Schema(description = "状态: 0=停用 1=启用", example = "1")
    private Integer status;
}
