package cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 维修工单分页 Request VO。
 */
@Schema(description = "管理后台 - EAM 维修工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EamRepairOrderPageReqVO extends PageParam {

    @Schema(description = "工单号", example = "RO-20261108-001")
    private String orderNo;

    @Schema(description = "设备 ID", example = "3")
    private Long assetId;

    @Schema(description = "状态: 1=待派工 2=已派工 3=维修中 4=待验收 5=已完成 6=已取消", example = "1")
    private Integer status;

    @Schema(description = "优先级: 1=低 2=中 3=高 4=紧急", example = "3")
    private Integer priority;
}
