package cn.iocoder.yudao.module.eam.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartPageReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSparePartDO;

public interface EamSparePartMapper extends BaseMapperX<EamSparePartDO> {

    default PageResult<EamSparePartDO> selectPage(EamSparePartPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EamSparePartDO>()
                .eqIfPresent(EamSparePartDO::getPartNo, reqVO.getPartNo())
                .likeIfPresent(EamSparePartDO::getName, reqVO.getName())
                .orderByDesc(EamSparePartDO::getId));
    }

    /** 按备件编号查询（租户内唯一） */
    default EamSparePartDO selectByPartNo(String partNo) {
        return selectOne(new LambdaQueryWrapperX<EamSparePartDO>()
                .eq(EamSparePartDO::getPartNo, partNo)
                .eq(EamSparePartDO::getDeleted, false));
    }
}
