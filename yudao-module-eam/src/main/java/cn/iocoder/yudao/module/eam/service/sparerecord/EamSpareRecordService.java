package cn.iocoder.yudao.module.eam.service.sparerecord;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSpareRecordDO;

import java.util.List;

/**
 * EAM 备件出入库流水 Service 接口。
 *
 * 流水由备件 Service 在入库/出库/退库时写入；演示仓库未给流水配独立 Controller，
 * 查询方法供备件详情页/对账场景内部调用。
 */
public interface EamSpareRecordService {

    /** 写入一条出入库流水 */
    Long createRecord(EamSpareRecordDO record);

    /** 根据 ID 查询 */
    EamSpareRecordDO getRecord(Long id);

    /** 按备件 ID 查询流水列表（出入库时间线） */
    List<EamSpareRecordDO> getRecordListByPartId(Long partId);

    /** 流水分页查询 */
    PageResult<EamSpareRecordDO> getRecordPage(Long partId, Integer type, PageParam pageParam);
}
