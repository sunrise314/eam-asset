package cn.iocoder.yudao.module.eam.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSpareRecordDO;

import java.util.List;

public interface EamSpareRecordMapper extends BaseMapperX<EamSpareRecordDO> {

    /** 流水分页（演示仓库未给流水配独立 Controller，入参直接透传） */
    default PageResult<EamSpareRecordDO> selectPage(Long partId, Integer type, PageParam pageParam) {
        return selectPage(pageParam, new LambdaQueryWrapperX<EamSpareRecordDO>()
                .eqIfPresent(EamSpareRecordDO::getPartId, partId)
                .eqIfPresent(EamSpareRecordDO::getType, type)
                .orderByDesc(EamSpareRecordDO::getId));
    }

    /** 按备件 ID 查全部流水（出入库时间线） */
    default List<EamSpareRecordDO> selectListByPartId(Long partId) {
        return selectList(new LambdaQueryWrapperX<EamSpareRecordDO>()
                .eq(EamSpareRecordDO::getPartId, partId)
                .eq(EamSpareRecordDO::getDeleted, false)
                .orderByDesc(EamSpareRecordDO::getId));
    }
}
