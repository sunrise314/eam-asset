package cn.iocoder.yudao.module.eam.service.sparepart;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSparePartDO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSpareRecordDO;
import cn.iocoder.yudao.module.eam.dal.mysql.EamSparePartMapper;
import cn.iocoder.yudao.module.eam.enums.EamSpareRecordType;
import cn.iocoder.yudao.module.eam.service.sparerecord.EamSpareRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EAM 备件 Service 实现。
 *
 * 库存变动只走 inbound/outbound（改库存 + 写流水在同一事务），不允许直接 update stock，
 * 否则流水与库存会对不上账。
 */
@Service
@RequiredArgsConstructor
public class EamSparePartServiceImpl implements EamSparePartService {

    private static final ErrorCode ERROR_CODE_NOT_EXISTS = new ErrorCode(1_003_300, "备件不存在: id={}");
    private static final ErrorCode ERROR_CODE_STOCK_NOT_ENOUGH = new ErrorCode(1_003_301, "备件库存不足: 当前库存={}, 需出库={}");

    private final EamSparePartMapper sparePartMapper;
    private final EamSpareRecordService spareRecordService;

    @Override
    public Long createSparePart(EamSparePartSaveReqVO createReqVO) {
        EamSparePartDO part = new EamSparePartDO();
        part.setPartNo(createReqVO.partNo());
        part.setName(createReqVO.name());
        part.setSpec(createReqVO.spec());
        part.setUnit(createReqVO.unit());
        part.setStock(createReqVO.stock() != null ? createReqVO.stock() : 0);
        part.setSafetyStock(createReqVO.safetyStock() != null ? createReqVO.safetyStock() : 0);
        part.setAssetIds(createReqVO.assetIds());
        part.setLocation(createReqVO.location());
        part.setRemark(createReqVO.remark());
        sparePartMapper.insert(part);
        return part.getId();
    }

    @Override
    public void updateSparePart(EamSparePartSaveReqVO updateReqVO) {
        validatePartExists(updateReqVO.id());
        EamSparePartDO updateObj = new EamSparePartDO();
        updateObj.setId(updateReqVO.id());
        updateObj.setPartNo(updateReqVO.partNo());
        updateObj.setName(updateReqVO.name());
        updateObj.setSpec(updateReqVO.spec());
        updateObj.setUnit(updateReqVO.unit());
        // 注意：更新主数据不动 stock，库存只能由出入库流水改变
        updateObj.setSafetyStock(updateReqVO.safetyStock());
        updateObj.setAssetIds(updateReqVO.assetIds());
        updateObj.setLocation(updateReqVO.location());
        updateObj.setRemark(updateReqVO.remark());
        sparePartMapper.updateById(updateObj);
    }

    @Override
    public void deleteSparePart(Long id) {
        validatePartExists(id);
        sparePartMapper.deleteById(id);
    }

    @Override
    public EamSparePartDO getSparePart(Long id) {
        EamSparePartDO part = sparePartMapper.selectById(id);
        if (part == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
        return part;
    }

    @Override
    public PageResult<EamSparePartDO> getSparePartPage(EamSparePartPageReqVO pageReqVO) {
        return sparePartMapper.selectPage(pageReqVO);
    }

    /**
     * 入库。退库（维修剩余件返还）复用本方法，bizOrderNo 带原出库单号即可对冲。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EamSparePartDO inbound(Long partId, Integer quantity, String bizOrderNo, String operator) {
        EamSparePartDO part = getSparePart(partId);
        part.setStock((part.getStock() != null ? part.getStock() : 0) + quantity);
        sparePartMapper.updateById(part);
        writeRecord(partId, EamSpareRecordType.IN.getCode(), quantity, bizOrderNo, operator);
        return part;
    }

    /**
     * 出库。库存不足直接抛 1_003_301，不允许扣成负数。
     *
     * 并发取舍：演示仓库先读后写；商用高并发应改原子 SQL
     * UPDATE eam_spare_part SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}，
     * 影响行数为 0 即库存不足，避免两个并发出库都读到旧库存导致超卖。
     *
     * 低库存预警：后端不内嵌告警通道（无 MQ/通知依赖），把最新 stock/safetyStock 随返回值带出，
     * 前端比对 stock < safetyStock 即亮预警；商用可在此发领域事件接通知中心。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EamSparePartDO outbound(Long partId, Integer quantity, String bizOrderNo, String operator) {
        EamSparePartDO part = getSparePart(partId);
        int stock = part.getStock() != null ? part.getStock() : 0;
        if (stock < quantity) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_STOCK_NOT_ENOUGH, stock, quantity);
        }
        part.setStock(stock - quantity);
        sparePartMapper.updateById(part);
        writeRecord(partId, EamSpareRecordType.OUT.getCode(), quantity, bizOrderNo, operator);
        return part;
    }

    private void writeRecord(Long partId, Integer type, Integer quantity, String bizOrderNo, String operator) {
        EamSpareRecordDO record = new EamSpareRecordDO();
        record.setPartId(partId);
        record.setType(type);
        record.setQuantity(quantity);
        record.setBizOrderNo(bizOrderNo);
        record.setOperator(operator);
        spareRecordService.createRecord(record);
    }

    private void validatePartExists(Long id) {
        if (sparePartMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
    }
}
