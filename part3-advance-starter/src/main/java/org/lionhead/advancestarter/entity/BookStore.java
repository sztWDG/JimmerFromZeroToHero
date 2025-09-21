    package org.lionhead.advancestarter.entity;

    import jakarta.annotation.Nullable;
    import org.babyfish.jimmer.sql.*;

    import java.time.LocalDateTime;
    import java.util.List;

    @Entity
    public interface BookStore {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        int id();

        @Key
        String name();

        @Nullable
        String website();

        LocalDateTime createdTime();

        LocalDateTime modifiedTime();

        @OneToMany(mappedBy = "bookStore")
        List<Book> books();

    }
