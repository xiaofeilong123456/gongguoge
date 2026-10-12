package com.gongguoge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gongguoge.mapper")
public class GongguogeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GongguogeApplication.class, args);
    }
}
