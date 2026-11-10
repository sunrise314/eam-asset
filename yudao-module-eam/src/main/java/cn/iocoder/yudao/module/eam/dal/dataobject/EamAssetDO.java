package cn.iocoder.yudao.module.eam.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * EAM 设备台账。设备资产的主数据，保养计划/维修工单/备件都围绕它挂接。
 *
 * attributes 用 JSONB 存设备扩展属性（功率/电压/厂家/序列号等），避免为每个行业建字段。
 * status 随业务联动：报修可置 REPAIRING，验收通过回 RUNNING（联动取舍见维修工单 Service 注释）。
 */
@Data
@TableName(value = "eam_asset", autoResultMap = true)
public class EamAssetDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 设备编号，租户内唯一，如 EQ-CNC-001 */
    private String assetNo;

    /** 设备名称，如 数控车床 */
    private String name;

    /** 规格型号，如 CKA6150 */
    private String model;

    /** 设备分类，如 机加/注塑/装配 */
    private String category;

    /** 安装位置，如 一车间-A线 */
    private String location;

    /** 1=运行 2=停机 3=维修中 4=报废 */
    private Integer status;

    /** 采购日期 */
    private LocalDate purchaseDate;

    /** 质保到期日 */
    private LocalDate warrantyUntil;

    /** 责任人用户 ID */
    private Long responsibleUserId;

    /** 设备扩展属性（JSONB），如 {"power":"15kW","vendor":"沈阳机床","serialNo":"SY20240315"} */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> attributes;

    private String remark;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
