package org.lionhead.advancestarter.entity;

import org.babyfish.jimmer.sql.*;

import java.time.LocalDateTime;
import java.util.List;

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

//    String tenant();

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();

    @ManyToMany(mappedBy = "authors")
    List<Book> books();
}
