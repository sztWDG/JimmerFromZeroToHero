package org.lionhead.jimmersql.sqlclient;


import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.lionhead.jimmersql.entity.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class App {
    private static final String URL = "jdbc:postgresql://43.138.210.244:5432/postgres?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8";
    private static final String USERNAME = "user_CPKmph";
    private static final String PASSWORD = "password_baQimQ";
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        // 通过DriverManager获取⼀个数据库连接
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            // 最简单的JSqlClient，传⼊⼀个数据库连接就⾏
            JSqlClient sqlClient = JSqlClient
                    .newBuilder()
                    // 第⼀步：SqlClient执⾏时需要⼀个connect，这⾥需要传⼊⼀个ConnectionManager
                    // 注意singleConnectionManager通常只⽤于简单场景，实际业务中⼀般不会使⽤它
                    .setConnectionManager(ConnectionManager.singleConnectionManager(connection))
                    // 第⼆步：需要设置数据库⽅⾔
                    .setDialect(new PostgresDialect())
                    .setExecutor(Executor.log(LOGGER))
                    .build();

            Author author = sqlClient.findById(Author.class, 1);
            LOGGER.info("author: {}", author);
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage(), e);
        }

    }
}
