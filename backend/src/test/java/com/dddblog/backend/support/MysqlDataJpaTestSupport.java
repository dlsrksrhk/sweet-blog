package com.dddblog.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class MysqlDataJpaTestSupport {

	private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
		DockerImageName.parse("mysql:8.0.36")
	)
		.withDatabaseName("ddd_blog_test")
		.withUsername("test")
		.withPassword("test");

	static {
		MYSQL.start();
	}

	@DynamicPropertySource
	static void mysql_속성을_등록한다(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
	}
}
