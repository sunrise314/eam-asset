package cn.iocoder.yudao.module.eam.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanPageReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainPlanDO;

import java.util.List;

public interface EamMaintainPlanMapper extends BaseMapperX<EamMaintainPlanDO> {

    default PageResult<EamMaintainPlanDO> selectPage(EamMaintainPlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EamMaintainPlanDO>()
                .eqIfPresent(EamMaintainPlanDO::getPlanNo, reqVO.getPlanNo())
                .eqIfPresent(EamMaintainPlanDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(EamMaintainPlanDO::getStatus, reqVO.getStatus())
                .orderByDesc(EamMaintainPlanDO::getId));
    }

    /** 按计划编号查询（租户内唯一） */
    default EamMaintainPlanDO selectByPlanNo(String planNo) {
        return selectOne(new LambdaQueryWrapperX<EamMaintainPlanDO>()
                .eq(EamMaintainPlanDO::getPlanNo, planNo)
                .eq(EamMaintainPlanDO::getDeleted, false));
    }

    /** 按状态捞计划列表（保养扫描时取"启用"的计划喂给 MaintainPlanEngine） */
    default List<EamMaintainPlanDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<EamMaintainPlanDO>()
                .eq(EamMaintainPlanDO::getStatus, status)
                .eq(EamMaintainPlanDO::getDeleted, false));
    }
}
