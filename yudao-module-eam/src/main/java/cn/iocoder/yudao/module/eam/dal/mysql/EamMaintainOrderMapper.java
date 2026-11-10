package cn.iocoder.yudao.module.eam.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderPageReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainOrderDO;

public interface EamMaintainOrderMapper extends BaseMapperX<EamMaintainOrderDO> {

    default PageResult<EamMaintainOrderDO> selectPage(EamMaintainOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EamMaintainOrderDO>()
                .eqIfPresent(EamMaintainOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(EamMaintainOrderDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(EamMaintainOrderDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(EamMaintainOrderDO::getStatus, reqVO.getStatus())
                .orderByDesc(EamMaintainOrderDO::getId));
    }

    /** 按工单号查询 */
    default EamMaintainOrderDO selectByOrderNo(String orderNo) {
        return selectOne(new LambdaQueryWrapperX<EamMaintainOrderDO>()
                .eq(EamMaintainOrderDO::getOrderNo, orderNo)
                .eq(EamMaintainOrderDO::getDeleted, false));
    }
}
