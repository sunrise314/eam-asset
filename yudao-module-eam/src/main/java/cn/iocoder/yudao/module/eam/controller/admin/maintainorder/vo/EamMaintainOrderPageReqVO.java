package cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 保养工单分页 Request VO。
 */
@Schema(description = "管理后台 - EAM 保养工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EamMaintainOrderPageReqVO extends PageParam {

    @Schema(description = "工单号", example = "MO-20261110-1")
    private String orderNo;

    @Schema(description = "保养计划 ID", example = "1")
    private Long planId;

    @Schema(description = "设备 ID", example = "1")
    private Long assetId;

    @Schema(description = "状态: 1=待执行 2=执行中 3=已完成 4=已取消", example = "1")
    private Integer status;
}
