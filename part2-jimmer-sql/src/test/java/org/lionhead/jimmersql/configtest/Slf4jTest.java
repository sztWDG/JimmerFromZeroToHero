package org.lionhead.jimmersql.configtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jTest {
    private static final Logger logger = LoggerFactory.getLogger(LogTest.class);

    public static void main(String[] args) {
        logger.info("SLF4J 配置成功！");
        logger.debug("调试信息：{}", "这是一条调试日志");
    }
}
