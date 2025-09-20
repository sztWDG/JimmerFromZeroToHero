package org.lionhead.advancestarter.entity;

import org.babyfish.jimmer.sql.*;

import java.time.LocalDateTime;

@Entity
public interface Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id();

    @Key
    String name();

    @Key
    int edition();

    double price();

    int storeId();

    String tenant();

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
