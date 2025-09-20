package org.lionhead.advancestarter;

import org.babyfish.jimmer.DraftObjects;
import org.babyfish.jimmer.sql.JSqlClient;
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
            queryFields(sqlClient);
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
        sqlClient.save(author1,SaveMode.UPSERT);

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
                .whereIf(website==null, booStoreTable.website().ne(website))
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

    private void fetcher(JSqlClient sqlClient){
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

        // 混用 tuple 元组和独享抓取器
        sqlClient.createQuery(table)
                .where(table.id().eq(1))
                .select(
                        table.id(),
                        table.fetch(
                        // allScalarFields 所有属性，包含关联属性
                        AuthorFetcher.$
                                .allTableFields()
                                .gender(false)
                )).fetchOne();
    }
}
