package cn.iocoder.yudao.module.eam.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EAM 模块自动配置。
 *
 * yudao 父工程的 @SpringBootApplication 会扫描 cn.iocoder.yudao.module.eam 包，
 * 所以本类的主要作用是：
 * 1. 确保 eam 包被 Spring 扫描到（组件注册）
 * 2. 明确 @MapperScan 指定 MyBatis Mapper 扫描路径
 *
 * 保养周期计算、维修状态机、MTBF 指标均内置在 framework 层（纯 Java），
 * 无需在此注册额外 Bean；商用扩展点（如保养扫描接 XXL-Job 调度、维修完成发领域事件）可在此引入。
 */
@SpringBootApplication(scanBasePackages = "cn.iocoder.yudao.module.eam")
@MapperScan("cn.iocoder.yudao.module.eam.dal.mysql")
public class EamConfiguration {
}
