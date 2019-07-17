package com.wp.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement			//启动事务支持
@MapperScan(value = "com.wp.demo.mapper")
@SpringBootApplication
public class GruaduationProject1Application extends SpringBootServletInitializer {


	/*@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(GruaduationProject1Application.class);
	}*/

	public static void main(String[] args) {
		SpringApplication.run(GruaduationProject1Application.class, args);
	}

}

