package com.educore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
@ConfigurationPropertiesScan   // يجمع كل @ConfigurationProperties beans تلقائياً (e.g. FawaterekProperties)
public class EduCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduCoreApplication.class, args);
    }

}
