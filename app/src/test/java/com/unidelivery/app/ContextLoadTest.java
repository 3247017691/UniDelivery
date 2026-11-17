package com.unidelivery.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** H2 + Flyway validate：schema 与 JPA 实体一致性、Bean 装配完整性。 */
@SpringBootTest
class ContextLoadTest {

    @Test
    void contextLoads() {
        // 启动即校验：Flyway migrate + hibernate ddl-auto=validate
    }
}
