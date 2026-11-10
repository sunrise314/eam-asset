package cn.iocoder.yudao.module.eam.service.sparerecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSpareRecordDO;
import cn.iocoder.yudao.module.eam.dal.mysql.EamSpareRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EAM 备件出入库流水 Service 实现。
 */
@Service
@RequiredArgsConstructor
public class EamSpareRecordServiceImpl implements EamSpareRecordService {

    private static final ErrorCode ERROR_CODE_NOT_EXISTS = new ErrorCode(1_003_302, "备件流水不存在: id={}");

    private final EamSpareRecordMapper spareRecordMapper;

    @Override
    public Long createRecord(EamSpareRecordDO record) {
        spareRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public EamSpareRecordDO getRecord(Long id) {
        EamSpareRecordDO record = spareRecordMapper.selectById(id);
        if (record == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
        return record;
    }

    @Override
    public List<EamSpareRecordDO> getRecordListByPartId(Long partId) {
        return spareRecordMapper.selectListByPartId(partId);
    }

    @Override
    public PageResult<EamSpareRecordDO> getRecordPage(Long partId, Integer type, PageParam pageParam) {
        return spareRecordMapper.selectPage(partId, type, pageParam);
    }
}
