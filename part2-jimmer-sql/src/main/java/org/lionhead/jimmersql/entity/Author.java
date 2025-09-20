package org.lionhead.jimmersql.entity;

import org.babyfish.jimmer.sql.*;

import java.time.LocalDateTime;

@Entity
public interface Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id();

    @Key
    String firstName();

    @Key
    String lastName();

    String gender();

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
