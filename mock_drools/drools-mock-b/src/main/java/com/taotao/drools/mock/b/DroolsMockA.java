package com.taotao.drools.mock.b;

import com.taotao.drools.mock.b.config.DroolsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DroolsMockA {
    public static void main(String[] args) {
        SpringApplication.run(DroolsConfig.class, args);
    }
}
