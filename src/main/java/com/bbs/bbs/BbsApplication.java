package com.bbs.bbs;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {MybatisAutoConfiguration.class})
@MapperScan("com.bbs.bbs.Mapper")
public class BbsApplication {
	public static void main(String[] args) {
		SpringApplication.run(BbsApplication.class, args);
	}
}
