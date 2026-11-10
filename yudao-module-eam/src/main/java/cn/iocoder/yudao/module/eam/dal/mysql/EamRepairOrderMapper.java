package cn.iocoder.yudao.module.eam.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderPageReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamRepairOrderDO;
import cn.iocoder.yudao.module.eam.enums.EamRepairStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface EamRepairOrderMapper extends BaseMapperX<EamRepairOrderDO> {

    default PageResult<EamRepairOrderDO> selectPage(EamRepairOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EamRepairOrderDO>()
                .eqIfPresent(EamRepairOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(EamRepairOrderDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(EamRepairOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(EamRepairOrderDO::getPriority, reqVO.getPriority())
                .orderByDesc(EamRepairOrderDO::getId));
    }

    /** 按工单号查询 */
    default EamRepairOrderDO selectByOrderNo(String orderNo) {
        return selectOne(new LambdaQueryWrapperX<EamRepairOrderDO>()
                .eq(EamRepairOrderDO::getOrderNo, orderNo)
                .eq(EamRepairOrderDO::getDeleted, false));
    }

    /** 查询设备在统计窗口内已完成的维修工单（供 MtbfCalculator 计算 MTBF/MTTR） */
    default List<EamRepairOrderDO> selectDoneList(Long assetId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return selectList(new LambdaQueryWrapperX<EamRepairOrderDO>()
                .eq(EamRepairOrderDO::getAssetId, assetId)
                .eq(EamRepairOrderDO::getStatus, EamRepairStatus.DONE.getCode())
                .between(EamRepairOrderDO::getFinishTime, windowStart, windowEnd)
                .eq(EamRepairOrderDO::getDeleted, false)
                .orderByAsc(EamRepairOrderDO::getFinishTime));
    }
}
