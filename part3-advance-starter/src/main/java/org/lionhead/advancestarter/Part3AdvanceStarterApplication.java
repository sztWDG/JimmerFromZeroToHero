package org.lionhead.advancestarter;

import org.babyfish.jimmer.DraftObjects;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.DeleteResult;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.ast.tuple.Tuple6;
import org.babyfish.jimmer.sql.exception.SaveException;
import org.lionhead.advancestarter.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class Part3AdvanceStarterApplication {

    private static final Logger logger = LoggerFactory.getLogger(Part3AdvanceStarterApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(Part3AdvanceStarterApplication.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner(JSqlClient sqlClient) {
        return args -> {
             complexFetcher(sqlClient);
        };
    }

    private void singeTableCRUD(JSqlClient sqlClient) {
        // create
        Author author1 = AuthorDraft.$
                .produce(draft -> draft
                        .setFirstName("John")
                        .setLastName("Willian")
                        .setGender("M")
                        .setCreatedTime(LocalDateTime.now())
                        .setModifiedTime(LocalDateTime.now())

                );
        SimpleSaveResult<Author> insertResult = sqlClient.insert(author1);
        logger.info("insertResult: {}", insertResult);

        // read
        // SQL 书写顺序：SELECT -> FROM -> JOIN ON -> WHERE -> GROUP BY -> HAVING ->
        // SQL 执行顺序：FROM -> JOIN ON -> WHERE -> GROUP BY -> HAVING -> SELECT
        AuthorTable table = AuthorTable.$;
        List<Author> authors = sqlClient.createQuery(table)
                .where(table.firstName().eq("John"))
                .where(table.lastName().eq("Willian"))
                .select(table)
                .execute();
        logger.info("author: {}", authors);

        // update
        // 方式一：update 语句
        Integer updateCount = sqlClient.createUpdate(table)
                .set(table.gender(), "F")
                .where(table.firstName().eq("John"))
                .where(table.lastName().eq("Willian"))
                .execute();
        logger.info("updateCount: {}", updateCount);
        // 方式二：保存指令
        int insertId = insertResult.getModifiedEntity().id();
        Author author2 = AuthorDraft.$
                .produce(author1,
                        draft -> draft
                                .setId(insertId)
                                .setGender("F"));
        SimpleSaveResult<Author> updateResult = sqlClient.update(author2);
        logger.info("updateResult: {}", updateResult);

        // delete
        // 方式一： delete 语句
        Integer deleteCount = sqlClient.createDelete(table)
                .where(table.firstName().eq("John"))
                .where(table.lastName().eq("Willian"))
                .execute();
        logger.info("deletCount: {}", deleteCount);
        // 方式二： 删除指令
        DeleteResult deleteResult = sqlClient.deleteById(Author.class, 1);
        logger.info("deleteResult: {}", deleteResult);

    }

    private void saveMode(JSqlClient sqlClient) {

        Author author1 = AuthorDraft.$
                .produce(draft -> draft
                        .setFirstName("John")
                        .setLastName("Willian")
                        .setGender("M")
                        .setCreatedTime(LocalDateTime.now())
                        .setModifiedTime(LocalDateTime.now())
                );
        // INSERT_ONLY: 直接生成 INSERT 语句
        logger.info("SaveMode.INSERT_ONLY =======================");
        // 简写形式
        // sqlClient.insert(author1)
        // sqlClient.save(author1, SaveMode.INSERT_ONLY);
        SimpleSaveResult<Author> insertOnlyResult = sqlClient
                .getEntities()
                .saveCommand(author1)
                .setMode(SaveMode.INSERT_ONLY)
                .execute();
        int id1 = insertOnlyResult.getModifiedEntity().id();
        sqlClient.deleteById(Author.class, id1);

        // UPDATE_ONLY: 直接生成 UPDATE_ 语句
        logger.info("SaveMode.UPDATE_ONLY =======================");
        // 简写形式
        // sqlClient.insert(author1)
        // sqlClient.save(author1, SaveMode.UPDATE_ONLY);
        sqlClient
                .getEntities()
                .saveCommand(author1)
                .setMode(SaveMode.UPDATE_ONLY)
                .execute();

        // UPSERT
        // 先判断 id/key 是否已赋值，然后生成 SELECT 语句查询数据是否存在
        // 如果数据已存在，则生成 UPDATE 语句，反之生成 INSERT 语句
        // 简写形式
        // sqlClient.save(author1);
        // 第一次 UPSERT，数据不存在，会生成 INSERT
        logger.info("SaveMode.UPSERT =======================");
        SimpleSaveResult<Author> upsertResult = sqlClient
                .getEntities()
                .saveCommand(author1)
                .setMode(SaveMode.UPSERT)
                .execute();
        int id3 = upsertResult.getModifiedEntity().id();
        // 第二次 UPSERT, 此时数据已存在，会生成 UPDATE；
        sqlClient.save(author1, SaveMode.UPSERT);

        // 手动 unload 掉两个 Key 属性
        Author authorWithoutIdAndKey = AuthorDraft.$
                .produce(author1, draft -> {
                    DraftObjects.unload(draft, AuthorProps.FIRST_NAME);
                    DraftObjects.unload(draft, AuthorProps.LAST_NAME);
                });
        try {
            // 保存一个既没有 id 属性也没有 key 属性的对象时， UPSERT 模式认为该类对象是非法的，将会抛出异常
            sqlClient.save(authorWithoutIdAndKey, SaveMode.UPSERT);
        } catch (SaveException.NeitherIdNorKey e) {
            logger.error("save author neither id nor key, msg: {}", e.getMessage(), e);
        }
        sqlClient.deleteById(Author.class, id3);


        // INSERT_IF_ABSENT: 是 UPSERT 的变种，区别在于如果数据不存在则 INSERT, 否则不做任何事情
        logger.info("SaveMode.INSERT_IF_ABSENT ====================");
        // 第一次，数据不存在，所以生成 INSERT
        int id4 = sqlClient
                .getEntities()
                .saveCommand(author1)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .execute().getModifiedEntity().id();
        // 第二次，数据已存在，所以生成 INSERT
        sqlClient.save(author1, SaveMode.INSERT_IF_ABSENT);
        sqlClient.deleteById(Author.class, id4);


        // NON_IDEMPOTENT_UPSERT: 非幂等性 UPSERT, 也是 UPSERT 模式的变种
        // 区别在于认为既无 ID 也 无 Key 的对象是非法的，会抛出 SaveException.NeitherIdNorKey
        // 而 NON_IDEMPOTENT_UPSERT 认为既无 ID 也无 Key 的对象是合法的，会直接 INSERT
        logger.info("SaveMode.NON_IDEMPOTENT_UPSERT ====================");
        int id5 = sqlClient
                .save(author1, SaveMode.NON_IDEMPOTENT_UPSERT)
                .getModifiedEntity().id();
        sqlClient.deleteById(Author.class, id5);
        sqlClient.save(authorWithoutIdAndKey, SaveMode.NON_IDEMPOTENT_UPSERT);

    }

    private void dynamicWhere(JSqlClient sqlClient) {

        String website = "abc";
        BookStoreTable booStoreTable = BookStoreTable.$;
        // 方式一：使用 whereIf
        sqlClient.createQuery(booStoreTable)
                .whereIf(website != null, booStoreTable.website().eq(website))
                .select(booStoreTable)
                .execute();
        // 方式二：使用谓词 If；默认忽略了 null 和空字符串
        sqlClient.createQuery(booStoreTable)
                .where(booStoreTable.website().eqIf(website))
                .select(booStoreTable)
                .execute();

        // whereIf 的问题
        // 1. 模板代码太多影响可读性
        // 2. 会改写 SQL 对 null 的判断， where website = null
        website = null;
        sqlClient.createQuery(booStoreTable)
                // eq(NULL) --> isNull() --> is null
                .whereIf(website == null, booStoreTable.website().eq(website))
                // ne(NULL) --> isNotNull() --> is not null
                .whereIf(website == null, booStoreTable.website().ne(website))
                .select(booStoreTable)
                .execute();
        // 3. 在 Java 当中，对方法传参是立即求值的，所以 whereIf 两个参数都会立即执行
        // 这个时候，对非空属性进行 eq(null) 的判断就会导致参数非法异常
        // 解决：可以将参数改造成为 Lambda 表达式
        String name = null;
        sqlClient.createQuery(booStoreTable)
                .whereIf(name != null, () -> booStoreTable.name().eq(name))
                .select(booStoreTable)
                .execute();
    }

    private void queryFields(JSqlClient sqlClient) {
        AuthorTable table = AuthorTable.$;
        // select tuple 更适合 SQL 的书写习惯，但是可读性不好
        List<Tuple6<Integer, String, String, String, LocalDateTime, LocalDateTime>> selectByTuple
                = sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(
                        table.id(),
                        table.firstName(),
                        table.lastName(),
                        table.gender(),
                        table.createdTime(),
                        table.modifiedTime()
                ).execute();
        logger.info("queryFields: {}", selectByTuple);
        // 想要转成可读性好的对象，需要自己手动处理
        List<Author> tupleToList = selectByTuple.stream()
                .map(
                        tuple ->
                                AuthorDraft.$
                                        .produce(draft -> draft
                                                .setId(tuple.get_1())
                                                .setFirstName(tuple.get_2())
                                                .setLastName(tuple.get_3())
                                                .setGender(tuple.get_4())
                                                .setCreatedTime(tuple.get_5())
                                                .setModifiedTime(tuple.get_6())
                                        )).toList();
        logger.info("tupleToList: {}", tupleToList);

        // 对象抓取器，可读性非常好，而且用起来十分方便
        List<Author> selectByFetcher = sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(table.fetch(
                        AuthorFetcher.$
                                .firstName()
                                .lastName()
                                .gender()
                                .createdTime()
                                .modifiedTime())
                ).execute();
        // 等价于：
        // sqlClient.findById(Author.class, 1);
        logger.info("selectByFether: {}", selectByFetcher);
        // 缺少一些字段
        Author author = sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(table.fetch(
                        // 对象抓取器，自动且强制隐含抓取了 id 属性
                        AuthorFetcher.$
                                .firstName()
                                .lastName()
                                .gender()

                )).fetchOne();
        // 等价于
        // AuthorFetcher authorFetcher = AuthorFetcher.$.firstName().lastName().gender();
        // Author authorById = sqlClient.findById(authorFetcher, 1);
        logger.info("author: {}", author);

    }

    private void fetcher(JSqlClient sqlClient) {
        AuthorTable table = AuthorTable.$;
        sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(table.fetch(
                        // 对象抓取器，自动且强制隐含抓取了 id 属性
                        AuthorFetcher.$
                                .firstName()
                                .lastName()
                                .gender()
                                .createdTime()
                                .modifiedTime()

                )).fetchOne();

        // allScalarFields
        sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(table.fetch(
                        // allScalarFields 抓取所有标量属性（该表没有计算属性和关联属性，标量属性就是所有）
                        AuthorFetcher.$
                                .allScalarFields()
                )).fetchOne();

        // allTableFields
        sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(table.fetch(
                        // allTableFields 所有属性，包含关联属性
                        AuthorFetcher.$
                                .allTableFields()
                )).fetchOne();

        // false 取消某些属性
        sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(table.fetch(
                        // allScalarFields 所有属性，包含关联属性
                        AuthorFetcher.$
                                .allTableFields()
                                .gender(false)
                )).fetchOne();


    }

    private void andOrNot(JSqlClient sqlClient) {
        AuthorTable table = AuthorTable.$;
        // 逻辑与 and
        // 方式一： 链式 where
        sqlClient.createQuery(table)
                .where(table.firstName().eq("John"))
                .where(table.lastName().eq("Willian"))
                .select(table)
                .execute();
        // 方式二： 在一个 where 里面写多个调节
        sqlClient.createQuery(table)
                .where(
                        table.firstName().eq("John"),
                        table.lastName().eq("Willian")
                )
                .select(table)
                .execute();

        // 逻辑或 or
        sqlClient.createQuery(table)
                .where(
                        Predicate.or(
                                table.firstName().eq("John"),
                                table.lastName().eq("Willian")
                        )
                )
                .select(table)
                .execute();

        // 逻辑非 not
        sqlClient.createQuery(table)
                .where(Predicate.not(table.firstName().eq("John")))
                // .where(table.firstName().ne("John")) 一般这样更好
                .select(table)
                .execute();

    }

    // 多表保存
    private void complexSave(JSqlClient sqlClient) {
        // CREATE
        // 保存 BookStore 的同同，也保存 BookStore 关联的 Book
        // 方式一：从 BookStore 开始
        BookStore bookStore1 = BookStoreDraft.$
                .produce(draft -> draft
                        .setName("book store 3")
                        .addIntoBooks(bookDraft -> bookDraft
                                .setName("jimmer from zero to hero")
                                .setEdition(1)
                                .setPrice(100.0)
                                .setTenant("a")
                        )
                        .addIntoBooks(bookDraft -> bookDraft
                                .setName("jimmer from zero to hero")
                                .setEdition(2)
                                .setPrice(99.0)
                                .setTenant("a")
                        )
                );
         // sqlClient.save(bookStore1, SaveMode.UPSERT);
         // 下面是完整版
        sqlClient.getEntities()
                .saveCommand(bookStore1)
                .setMode(SaveMode.UPSERT)
                // 可以单独设置也可以统一设置
                // .setAssociatedMode(BookStoreProps.BOOKS, AssociatedSaveMode.REPLACE)
                .setAssociatedModeAll(AssociatedSaveMode.REPLACE)
                .execute();

        // 方式二：从 Book 方向
        Book book1 = BookDraft.$
                .produce(draft -> draft
                        .setName("jimmer from zero to hero")
                        .setEdition(3)
                        .setPrice(100.0)
                        .setTenant("a")
                        .applyBookStore(bookStoreDraft -> bookStoreDraft
                                .setName("book Store 4"))
                );
        Book book2 = BookDraft.$
                .produce(draft -> draft
                        .setName("jimmer from zero to hero")
                        .setEdition(4)
                        .setPrice(100.0)
                        .setTenant("a")
                        .applyBookStore(bookStoreDraft -> bookStoreDraft
                                .setName("book store 4")
                        )
                );
        List<Book> bookList = List.of(book1, book2);
        // sqlClient.saveEntities(bookList, SaveMode.INSERT_IF_ABSENT);
        sqlClient.getEntities()
                .saveEntitiesCommand(bookList)
                .setMode(SaveMode.UPSERT)
                .setAssociatedModeAll(AssociatedSaveMode.REPLACE)
                .execute();

    }

    // 五种关联对象保存模式
    private void associatedMode(JSqlClient sqlClient) {
        // 重点：关联对象的保存模式，仅演示，不设置所有属性
        // 演示前：给 Book.bookStore 先加上 @OnDissociate(DissociateAction.DELETE)，否则脱钩会报错
        // 先查出数据库中当前 id = 1 的 BookStore 以及其关联的 Book， books 从 1 到 9
        BookStoreFetcher bookStoreFetcher = BookStoreFetcher.$.books();
        BookStore existingBookStore = sqlClient.findById(bookStoreFetcher, 1);
        logger.info("existingBookStore: {}", existingBookStore);

        // REPLACE: 默认模式。参考聚合根保存模式-UPSERT，会对每一个关联对象进行 UPSERT 判定
        // 最后会将在数据库中已存在，但在被保存的对象中不存在的数据进行：脱钩操做
        // 适用于全量场景
        logger.info("关联对象保存模式：REPLACE=======================");
        BookStore bookStoreForReplace = BookStoreDraft.$
                .produce(draft -> draft.setId(1)
                        .addIntoBooks(book -> book.setId(1))
                        .addIntoBooks(book -> book.setId(2))
                        .addIntoBooks(book -> book.setId(3))
                        .addIntoBooks(book -> book.setId(4))
                        .addIntoBooks(book -> book.setId(5))
                        // 更新了 price
                        .addIntoBooks(book -> book.setId(6).setPrice(99.99))
                        // 特意将 7，8，9 注释掉，在 REPLACE 模式下这几个就会被脱钩
                        //.addIntoBooks(book -> book.setId(7))
                        //.addIntoBooks(book -> book.setId(8))
                        //.addIntoBooks(book -> book.setId(9))
                        // 然后新增一本 book
                        .addIntoBooks(book -> book
                                .setName("jimmer from zero to hero")
                                .setEdition(1)
                                .setPrice(100.0)
                                .setTenant("a")
                        )
                );
        // 简写形式，默认情况下：聚合根为 UPSERT 模式，关联属性为 REPLACE 模式
        //sqlClient.save(bookStoreForReplace, AssociatedSaveMode.REPLACE);
        sqlClient.getEntities()
                .saveCommand(bookStoreForReplace)
                .setAssociatedModeAll(AssociatedSaveMode.REPLACE)
                // 单独为指定关联属性设置保存模式为 REPLACE
                // 这里只是为了举例
                //.setAssociatedMode(BookProps.BOOK_STORE, AssociatedSaveMode.REPLACE)
                //.setAssociatedMode(BookProps.AUTHORS, AssociatedSaveMode.REPLACE)
                .execute();

        // MERGE: 相比于 REPLACE，不会脱钩数据。适用于增量场景
        logger.info("关联对象保存模式：MERGE=======================");
        BookStore bookStoreForMerge = BookStoreDraft.$
                .produce(draft -> draft.setId(1)
                        // 根据 ID 查询已存在，所以 price 被更新
                        .addIntoBooks(book -> book.setId(1).setPrice(99.99))
                        // 根据 Key 查询不存在，所以被新增
                        .addIntoBooks(book -> book
                                .setName("jimmer from zero to hero")
                                .setEdition(2)
                                .setPrice(200.0)
                                .setTenant("a")
                        )
                );
        // 观察 SQL，并不会将数据库中已存在的 name = "jimmer from zero to hero", edition=1 的 book 删掉
        sqlClient.getEntities()
                .saveCommand(bookStoreForMerge)
                .setAssociatedModeAll(AssociatedSaveMode.MERGE)
                .execute();

        // APPEND: 无脑 INSERT。 如果数据已存在，会抛出 SaveException.NotUnique 异常
        logger.info("关联对象保存模式：APPEND=======================");
        try {
            BookStore bookStoreForAppend1 = BookStoreDraft.$
                    .produce(draft -> draft.setId(1)
                            // 这里 id = 1 的 book 在数据库中已存在，所以会抛出 SaveException.NotUnique 异常
                            .addIntoBooks(book -> book.setId(1).setPrice(99.99))
                            .addIntoBooks(book -> book
                                    .setName("jimmer from zero to hero")
                                    .setEdition(3)
                                    .setPrice(200.0)
                                    .setTenant("a")
                            )
                    );
            sqlClient.getEntities()
                    .saveCommand(bookStoreForAppend1)
                    .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                    .execute();
        } catch (SaveException.NotUnique e) {
            logger.error("SaveException$NotUnique: {}", e.getMessage(), e);
        }

        // 关联数据中没有数据库中已存在的数据，此时才能保存成功
        BookStore booStoreForAppend2 = BookStoreDraft.$
                .produce(draft -> draft.setId(1)
                        .addIntoBooks(book -> book
                                .setName("jimmer from zero to hero")
                                .setEdition(3)
                                .setPrice(200.0)
                                .setTenant("a")
                        )
                );
        sqlClient.getEntities()
                .saveCommand(booStoreForAppend2)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND)
                .execute();

        // APPEND_IF_ABSENT: 如果数据不存在则 INSERT, 否则不做任何事
        logger.info("关联对象保存模式：APPEND_IF_ABSENT=======================");
        BookStore bookStoreForAppendIfAbsent = BookStoreDraft.$
                .produce(draft -> draft.setId(1)
                        // 这里 id=1 的book 在数据库中已存在，即使又给 price 设置了新的值，也会被无视
                        .addIntoBooks(book -> book.setId(1).setPrice(99.99))
                        // 根据 Key 查询数据不存在，所以被新增
                        .addIntoBooks(book -> book
                                .setName("jimmer from zero to hero")
                                .setEdition(4)
                                .setPrice(200.0)
                                .setTenant("a")
                        )
                );
        sqlClient.getEntities()
                .saveCommand(bookStoreForAppendIfAbsent)
                .setAssociatedModeAll(AssociatedSaveMode.APPEND_IF_ABSENT)
                .execute();

        // VIOLENTLY_REPLACE: 先暴力删除数据库中该聚合根的所有关联数据，然后再 INSERT 关联对象
        logger.info("关联对象保存模式：VIOLENTLY_REPLACE=======================");
        BookStore bookStoreForViolentlyReplace = BookStoreDraft.$
                .produce(draft -> draft.setId(1)
                        // bookStore 的关联 books 仅此一个，其他都会被先删除，再 INSERT 这个关联
                        .addIntoBooks(book -> book
                                .setName("jimmer from zero to hero")
                                .setEdition(5)
                                .setPrice(200.0)
                                .setTenant("a")
                        )
                );
        sqlClient.getEntities()
                .saveCommand(bookStoreForViolentlyReplace)
                .setAssociatedModeAll(AssociatedSaveMode.VIOLENTLY_REPLACE)
                .execute();


    }

    // 多表查询
    // step1：链式 join 和链表类型
    private void chainedJoinAndJoinType(JSqlClient sqlClient) {
        BookTable bookTable = BookTable.$;
        BookStoreTable bookStoreTable = BookStoreTable.$;
        // Jimmer 没有提供显式 join，所有关联需要在实体中配置关联属性
        //sqlClient.createQuery(bookTable)
        //        .join(bookStoreTable)
        //        .on(bookTable.bookStoreId().eq(bookStoreTable.id()));

        logger.info("join by where");
        var booksBuTuple1 = sqlClient.createQuery(bookTable)
                // from book join book_store on book.store_id = book_store.id
                // where book_store.id = 1
                // book.store_id = book_store.id 直接优化了
                //  where book.store_id = 1
                //  Table 类型上，可以直接点出引用关联的属性
                .where(bookTable.bookStore().id().eq(1))
                // 但不能直接点出集合关联的属性：Why：集合关联在部分场景下可能导致分页安全问题。
                // .where(bookTable.authors().id().eq(1))
                .select(
                        bookTable.id(),
                        bookTable.name(),
                        bookTable.edition(),
                        bookTable.price()
                ).execute();

        logger.info("join by select");
        var booksBuTuple2 = sqlClient.createQuery(bookTable)
                .select(
                        bookTable.id(),
                        bookTable.name(),
                        bookTable.edition(),
                        bookTable.price(),
                        // from book join book_store on book.store_id = book_store.id
                        bookTable.bookStore().id()
                ).execute();

        logger.info("集合关联潜在分页安全问题");
        // 前提：我们关注的 count 是聚合根的 count
        var query = sqlClient.createQuery(bookTable)
                .where(bookTable.id().eq(1))
                .select(
                        bookTable.id(),
                        bookTable.name(),
                        bookTable.edition(),
                        bookTable.price(),
                        // from book join book_store on book.store_id = book_store.id
                        bookTable.asTableEx().authors().id(),
                        bookTable.asTableEx().authors().firstName(),
                        bookTable.asTableEx().authors().lastName()
                );
        long count = query.fetchUnlimitedCount();
        logger.info("count: {}", count);
        var booksByTuple3 = query.execute();
        logger.info("booksByTuple3 : {}", booksByTuple3);

        var queryByFetcher = sqlClient.createQuery(bookTable)
                        .where(bookTable.id().eq(1))
                                .select(bookTable.fetch(BookFetcher.$.name().edition().price()
                                        .authors(AuthorFetcher.$.firstName().lastName())
                                ));
        long countByFetcher = queryByFetcher.fetchUnlimitedCount();
        logger.info("countByFetcher: {}", countByFetcher);

        List<Book> booksByFetcher = queryByFetcher.execute();
        logger.info("booksByFetcher: {}", booksByFetcher);


        logger.info("四种联表类型");
        //INNER JOIN: JoinType.INNER
        List<Book> booksInner = sqlClient.createQuery(bookTable)
                .where(bookTable.bookStore(JoinType.INNER).id().eq(1))
                .select(bookTable)
                .execute();
        //LEFT JOIN: JoinType.LEFT
        List<Book> booksLeft = sqlClient.createQuery(bookTable)
                .where(bookTable.bookStore(JoinType.LEFT).id().eq(1))
                .select(bookTable)
                .execute();
        //RIGHT JOIN: JoinType.RIGHT
        List<Book> booksRight = sqlClient.createQuery(bookTable)
                .where(bookTable.bookStore(JoinType.RIGHT).id().eq(1))
                .select(bookTable)
                .execute();
        //FULL JOIN: JoinType.FULL
        List<Book> booksFull = sqlClient.createQuery(bookTable)
                .where(bookTable.bookStore(JoinType.FULL).id().eq(1))
                .select(bookTable)
                .execute();

        logger.info("注意事项");
        // 对于引用关联，Jimmer 会在 Table 上自动生成一个 xxld()，
        // 当该引用关联是基于假外键时，table.xxld() 代表当前表当中的外键列，
        // 而table.xx().id() 则代表关联表的主键id。
        sqlClient.createQuery(bookTable)
                .where(bookTable.bookStoreId().eq(1))
                .where(bookTable.bookStore().id().eq(1))
                .select(bookTable)
                .execute();

        sqlClient.createQuery(bookTable)
                .where(bookTable.bookStore().id().eq(1))
                .select(bookTable)
                .execute();

        // 默认情况下，Jimmer 中认为 join 了的表一定要使用，否则会被优化掉。
        // 这导致在 Jimmer 中只联表但不使用的sql无法实现。
        sqlClient.createQuery(bookTable)
                // .where(bookTable.bookStore())  必须要使用一些属性才可以
                .select(bookTable.bookStore().id(), bookTable.bookStore().name())
                .execute();
    }

    // 多表查询
    // 动态 join
    private void dynamicJoin(JSqlClient sqlClient) {
        Integer bookStoreId1 = null;
        Integer bookStoreId2 = 1;

        BookTable table = BookTable.$;
        sqlClient.createQuery(table)
                .where(table.bookStore().id().eqIf(bookStoreId1))
                .select(table)
                .execute();

        sqlClient.createQuery(table)
                .where(table.bookStore().id().eqIf(bookStoreId2))
                .select(table)
                .execute();
    }

    // 表连接优化
    private void optimizeTableJoin(JSqlClient sqlClient) {
        BookTable table = BookTable.$;
        // 这个查询里使用了两次 table.bookStore() 代表两次 jion
        // 但是 Jimmer 会自动优化表连接，实际生成的 SQL 只有一个 jion
        // 所以放心大胆地联表，一切优化交给 Jimmer
        sqlClient.createQuery(table)
                .where(table.bookStore().id().eq(1))
                .where(table.bookStore().name().eq("MANNING"))
                .select(table)
                .execute();
    }

    // 子查询
    private void subQuery(JSqlClient sqlClient) {
        BookTable table = BookTable.$;
        // 更推荐使用 TableEx，防止分页查询问题
        BookTableEx tableEx = BookTableEx.$;

        sqlClient.createQuery(tableEx)
                .where(tableEx.bookStore().id().eqIf(1));

        // 单列子查询
        List<Book> books1 = sqlClient.createQuery(table)
                .where(
                        table.edition().eq(
                                sqlClient.createSubQuery(table)
                                        .select(table.edition().max())
                        )
                )
                .select(table)
                .execute();

        // 多列子查询
        List<Book> books2 = sqlClient.createQuery(table)
                .where(Expression.tuple(table.name(), table.edition())
                        .in(
                                sqlClient.createSubQuery(table)
                                        .groupBy(table.name())
                                        .select(table.name(), table.edition().max())
                                // 子查询不需要 .execute() 结尾
                        )
                )
                .select(table)
                .execute();
    }

    // 隐式子查询
    private void implicitSubQuery(JSqlClient sqlClient) {
        BookTable table = BookTable.$;
        // table.authors()
        // table.asTableEx().authors(author -> author.firstName().eq("Alex"));
        List<Book> books1 = sqlClient.createQuery(table)
                //.where(table.bookStore().id().eq(1))
                // 可以通过 asTableEx() 转换类型，但是不推荐，因为可能导致一些分页安全的问题
                //.where(table.asTableEx().authors().firstName().eq("Alex"))
                // 隐式子查询：Lambda 实现
                .where(table.authors(author -> author.firstName().eq("Alex")))
                .select(table)
                .execute();
        logger.info("implicitSubQuery: {}", books1);

        //另一种普通写法：非隐式查询
        AuthorTableEx authorTableEx = AuthorTableEx.$;
        sqlClient.createQuery(table)
                .where(
                        table.id().in(
                                sqlClient.createSubQuery(authorTableEx)
                                        .where(authorTableEx.firstName().eq("Alex"))
                                        .select(authorTableEx.books().id())
                        )
                ).select(table)
                .execute();
    }

    // 对象抓取器
    private void complexFetcher(JSqlClient sqlClient) {
        logger.info("默认情况下，关联对象只包含 id 属性");
        BookFetcher fetcher1 = BookFetcher.$.allScalarFields()
                .bookStore()
                .authors();
        Book bookBySimpleFetcher = sqlClient.findById(fetcher1, 1);
        logger.info("bookBySimpleFetcher: {}",bookBySimpleFetcher);

        logger.info("给关联对象指定对象抓取器，才可以抓取额外属性");
        BookFetcher fetcher2 = BookFetcher.$.allScalarFields()
                .bookStore(BookStoreFetcher.$.name())
                .authors(AuthorFetcher.$.firstName().lastName());
        Book bookByComplexFetcher = sqlClient.findById(fetcher2, 1);
        logger.info("bookByComplexFetcher :{}", bookByComplexFetcher);

        // 对象抓取器在多表情况下的工作方式：
        // 1. 先查询聚合根属性
        // 2. 再根据实体中的关联属性定义，去 LOAD 关联对象

        logger.info("对象抓取器是结构化的查询，与传统 SQL 平铺化查询处理方式不一样");
        BookTable bookTable = BookTable.$;
        // tuple ，更符合 SQL 的方式
        logger.info("多表查询，使用 tuple 返回平铺数据");
        var booksByTuple = sqlClient.createQuery(bookTable)
                .where(bookTable.authors(author -> author.firstName().eq("Alex")))
                .select(
                        bookTable.id(),
                        bookTable.name(),
                        bookTable.edition(),
                        bookTable.price(),
                        bookTable.bookStore().id(),
                        bookTable.bookStore().name(),
                        bookTable.asTableEx().authors().id(),
                        bookTable.asTableEx().authors().firstName(),
                        bookTable.asTableEx().authors().lastName()
                ).execute();
        logger.info("booksByTuple: {}", booksByTuple);

        // Jimmer 中，所有的查询条件都是作用于聚合根查询的，而关联对象只会通过实体中定义的关联属性去加载
        // 加载时使用的条件只有 ID：引用关联使用聚合根中的外键 ID,集合关联使用聚合根 ID 通过中间表关联查询
        // 所以可能出现这种情况：隐式子查询里，明明写了集合关联的条件，但是查询结果中出现了其他的数据
        List<Book> booksByFetcher = sqlClient.createQuery(bookTable)
                .where(bookTable.authors(author -> author.firstName().eq("Alex")))
                .select(bookTable.fetch(fetcher2))
                .execute();
        logger.info("booksByFetcher: {}", booksByFetcher);
        // 解决方式：使用属性过滤器
        List<Book> booksByFieldFilter = sqlClient.createQuery(bookTable)
                .where(bookTable.authors(author -> author.firstName().eq("Alex")))
                .select(bookTable.fetch(
                        BookFetcher.$.allScalarFields()
                                .bookStore(BookStoreFetcher.$.name())
                                .authors(AuthorFetcher.$.firstName().lastName(),
                                        it -> it.filter(
                                                args -> args.where(args.getTable().firstName().eq("Alex"))
                                        )
                                )
                )).execute();
        logger.info("booksByFieldFilter: {}", booksByFieldFilter);



    }

    private void complexDelete(JSqlClient sqlClient) {
        try {
            // NONE, 没有指定脱钩模式时默认为 NONE, 如果没有做全局配置的话，等同于 CHECK
            // 此时等同于该简写形式
            //sqlClient.getEntities().deleteCommand(BookStore.class, 1).execute()；
            // CHECK: 当前聚合根对象如果存在任何关联数据，则抛出异常阻止删除
            sqlClient.getEntities().deleteCommand(BookStore.class, 1)
                    .setDissociateAction(BookProps.BOOK_STORE, DissociateAction.CHECK)
                    .execute();
        }catch (SaveException.CannotDissociateTarget e) {
            logger.error("SaveException.CannotDissociateTarget: {}",e.getMessage(), e);
        }
        // LAX: Jimmer 自身什么都不做，由数据库控制是否级联删除。仅对假外键生效，其他场景等同于 CHECK
        sqlClient.getEntities().deleteCommand(Book.class,1)
                .setDissociateAction(BookProps.BOOK_STORE,DissociateAction.LAX)
                .execute();
        // SET_NULL: 将关联对象的外键设置为 NULL, 然后删除聚合根对象。需要注意，这个引用关联属性必须是可空的，
        // 在 Java 下意味着必须添加 @Nullable 或 @Null 注解
        sqlClient.getEntities().deleteCommand(Book.class,1)
                .setDissociateAction(BookProps.BOOK_STORE,DissociateAction.SET_NULL)
                .execute();
        // DELETE
        sqlClient.getEntities().deleteCommand(Book.class,1)
                .setDissociateAction(BookProps.BOOK_STORE,DissociateAction.DELETE)
                .execute();
    }

}
