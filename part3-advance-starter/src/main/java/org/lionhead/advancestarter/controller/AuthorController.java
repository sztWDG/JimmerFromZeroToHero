package org.lionhead.advancestarter.controller;

import jakarta.annotation.Resource;
import org.babyfish.jimmer.sql.JSqlClient;
import org.lionhead.advancestarter.entity.Author;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/author")
public class AuthorController {

    @Resource
    private JSqlClient sqlClient;

    @PostMapping
    public Author create(@RequestBody Author author){
        return sqlClient.insert(author).getModifiedEntity();
    }
}
