package cn.iocoder.yudao.module.eam.controller.admin.asset.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备台账分页 Request VO。
 */
@Schema(description = "管理后台 - EAM 设备台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EamAssetPageReqVO extends PageParam {

    @Schema(description = "设备编号", example = "EQ-CNC-001")
    private String assetNo;

    @Schema(description = "设备名称（模糊）", example = "数控车床")
    private String name;

    @Schema(description = "设备分类", example = "机加")
    private String category;

    @Schema(description = "状态: 1=运行 2=停机 3=维修中 4=报废", example = "1")
    private Integer status;
}
