package com.atguigu.tingshu.model;

import lombok.Data;

import javax.persistence.Column;

@Data
public class CDCEntity {
    // 注意Column 注解必须是persistence包下的
    @Column(name = "id")
    private Long id;
}