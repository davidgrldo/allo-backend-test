package com.allobank.splitbill.controller;

import com.allobank.splitbill.model.enums.ExpenseCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @GetMapping
    public List<String> list() {
        return java.util.Arrays.stream(ExpenseCategory.values())
                .map(Enum::name)
                .toList();
    }
}