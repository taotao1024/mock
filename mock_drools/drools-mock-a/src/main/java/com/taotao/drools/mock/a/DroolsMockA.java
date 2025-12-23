package com.taotao.drools.mock.a;

import com.taotao.drools.mock.a.config.DroolsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DroolsMockA {
    public static void main(String[] args) {
        SpringApplication.run(DroolsConfig.class, args);
    }
}
