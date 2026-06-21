package com.smartticket;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SmartTicket 智工单 · 应用入口。
 * 单体应用（禁 MQ / Spring Cloud / 对象存储，见 ARCHITECTURE §1.1）。
 * 按 @Mapper 注解扫描，覆盖各模块 mapper 包与 common/audit，且不误扫 service 接口。
 */
@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = "com.smartticket", annotationClass = Mapper.class)
public class SmartTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTicketApplication.class, args);
    }
}
