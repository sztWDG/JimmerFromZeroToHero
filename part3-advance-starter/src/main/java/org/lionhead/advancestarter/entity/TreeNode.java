package org.lionhead.advancestarter.entity;

import org.babyfish.jimmer.sql.*;

import java.time.LocalDateTime;

@Entity
public interface TreeNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    int id();

    String name();

    Integer parentId();

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();
}
