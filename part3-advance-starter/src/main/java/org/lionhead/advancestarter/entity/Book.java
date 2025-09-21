package org.lionhead.advancestarter.entity;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;

import java.time.LocalDateTime;
import java.util.List;

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

    // int storeId();

    String tenant();

    LocalDateTime createdTime();

    LocalDateTime modifiedTime();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "store_id", foreignKeyType = ForeignKeyType.FAKE)
    @OnDissociate(DissociateAction.DELETE)
    BookStore bookStore();

    @ManyToMany
    List<Author> authors();

}
