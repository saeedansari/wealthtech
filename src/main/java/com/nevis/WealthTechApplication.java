package com.nevis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WealthTechApplication {

  public static void main(String[] args) {
    SpringApplication.run(WealthTechApplication.class, args);
  }

}
