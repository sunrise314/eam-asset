package cn.iocoder.yudao.module.eam.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * EAM 备件。维修换件的库存单元，出入库只通过 EamSparePartService 的
 * inbound/outbound 进行（写流水 + 校验库存），不允许直接改 stock 字段。
 *
 * assetIds 用 JSONB 存适用设备 ID 列表——一个备件可用于多台设备，
 * 避免为"备件-设备"多对多关系单建关联表。
 */
@Data
@TableName(value = "eam_spare_part", autoResultMap = true)
public class EamSparePartDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 备件编号，租户内唯一，如 SP-BEAR-001 */
    private String partNo;

    /** 备件名称，如 主轴轴承 */
    private String name;

    /** 规格型号，如 SKF 7010C */
    private String spec;

    /** 计量单位，如 套/件/包 */
    private String unit;

    /** 当前库存 */
    private Integer stock;

    /** 安全库存（低于此值触发预警） */
    private Integer safetyStock;

    /** 适用设备 ID 列表（JSONB），如 [1,2] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> assetIds;

    /** 货架位置，如 A库-01-03 */
    private String location;

    private String remark;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
