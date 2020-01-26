package com.tf.yana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication

@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = "com.tf.yana")
public class SpringBootJdbcApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootJdbcApplication.class, args);
	}

//	@Bean
//	public MultipartResolver multipartResolver() {
//	    return new CommonsMultipartResolver();
//	}

//	@Bean
//	public CommonsMultipartResolver multipartResolver() {
//		CommonsMultipartResolver multipart = new CommonsMultipartResolver();
//		multipart.setMaxUploadSize(10 * 1024 * 1024);
//		return multipart;
//
//	}
//
//	@Bean
//	@Order(0)
//	public MultipartFilter multipartFilter() {
//		MultipartFilter multipartFilter = new MultipartFilter();
//		multipartFilter.setMultipartResolverBeanName("multipartReso‌​lver");
//		return multipartFilter;
//	}
}
