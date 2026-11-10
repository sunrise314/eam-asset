package cn.iocoder.yudao.module.eam.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * EAM 备件出入库流水。库存变动的审计轨迹：每次入库/出库/退库写一条，
 * bizOrderNo 关联业务工单号（维修工单/采购单），支撑"这批件用到哪台设备"的反查。
 */
@Data
@TableName("eam_spare_record")
public class EamSpareRecordDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 备件 ID */
    private Long partId;

    /** 类型：1=入库 2=出库 3=退库 */
    private Integer type;

    /** 数量（恒为正数，方向由 type 表达） */
    private Integer quantity;

    /** 关联业务工单号，如维修工单 RO-20261108-001 */
    private String bizOrderNo;

    /** 操作人 */
    private String operator;

    private String remark;

    private String creator;
    private LocalDateTime createTime;
    private String updater;
    private LocalDateTime updateTime;
    private Boolean deleted;
}
