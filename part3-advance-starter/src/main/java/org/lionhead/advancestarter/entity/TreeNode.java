package org.lionhead.advancestarter.entity;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
public interface TreeNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    int id();

    String name();

    // 已经有下方的 parent 对应数据库表中的 parent_id 字段
    // Integer parentId();

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();

    @ManyToOne
    @Nullable
    TreeNode parent();

    @OneToMany(mappedBy = "parent")
    List<TreeNode> child();
}
