package com.example.syncmanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.syncmanager.mapper")
public class SyncManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyncManagerApplication.class, args);
    }
}
