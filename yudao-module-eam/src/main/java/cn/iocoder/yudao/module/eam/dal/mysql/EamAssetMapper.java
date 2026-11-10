package cn.iocoder.yudao.module.eam.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetPageReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamAssetDO;

public interface EamAssetMapper extends BaseMapperX<EamAssetDO> {

    default PageResult<EamAssetDO> selectPage(EamAssetPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EamAssetDO>()
                .eqIfPresent(EamAssetDO::getAssetNo, reqVO.getAssetNo())
                .likeIfPresent(EamAssetDO::getName, reqVO.getName())
                .eqIfPresent(EamAssetDO::getCategory, reqVO.getCategory())
                .eqIfPresent(EamAssetDO::getStatus, reqVO.getStatus())
                .orderByDesc(EamAssetDO::getId));
    }

    /** 按设备编号查询（租户内唯一） */
    default EamAssetDO selectByAssetNo(String assetNo) {
        return selectOne(new LambdaQueryWrapperX<EamAssetDO>()
                .eq(EamAssetDO::getAssetNo, assetNo)
                .eq(EamAssetDO::getDeleted, false));
    }
}
