package cn.iocoder.yudao.module.eam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备状态。台账主数据的状态字段，随报修/维修完成/报废等业务操作流转。
 */
@Getter
@AllArgsConstructor
public enum EamAssetStatus {

    RUNNING(1, "运行"),
    DOWN(2, "停机"),
    REPAIRING(3, "维修中"),
    SCRAPPED(4, "报废");

    private final Integer code;
    private final String name;
}
