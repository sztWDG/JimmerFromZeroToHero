package org.lionhead.basicjimmercore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Part1BasicJimmerCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(Part1BasicJimmerCoreApplication.class, args);

        System.out.println("damn");

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL 驱动加载成功");
        } catch (ClassNotFoundException e) {
            System.out.println("PostgreSQL 驱动未找到");
        }
    }

}
