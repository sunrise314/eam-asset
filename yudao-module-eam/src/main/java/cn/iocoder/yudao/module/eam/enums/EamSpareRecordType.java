package cn.iocoder.yudao.module.eam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 备件出入库流水类型。退库（维修剩余件返还）复用入库通道，
 * 在 bizOrderNo 带原出库单号即可对冲，不单独开出库接口。
 */
@Getter
@AllArgsConstructor
public enum EamSpareRecordType {

    IN(1, "入库"),
    OUT(2, "出库"),
    RETURN(3, "退库");

    private final Integer code;
    private final String name;
}
