package com.shopwavefusion.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
public class DataSourceConfig {

	@Value("${MYSQLHOST:${DB_HOST:}}")
	private String mysqlHost;

	@Value("${MYSQLPORT:${DB_PORT:3306}}")
	private String mysqlPort;

	@Value("${MYSQLDATABASE:${DB_NAME:shopwavefusion}}")
	private String mysqlDatabase;

	@Value("${MYSQLUSER:${DB_USERNAME:root}}")
	private String mysqlUser;

	@Value("${MYSQLPASSWORD:${DB_PASSWORD:root}}")
	private String mysqlPassword;

	@Bean
	@Primary
	public DataSource dataSource() {
		if (StringUtils.hasText(mysqlHost)) {
			String url = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase;
			return DataSourceBuilder.create()
					.url(url)
					.username(mysqlUser)
					.password(mysqlPassword)
					.driverClassName("com.mysql.cj.jdbc.Driver")
					.build();
		}

		return DataSourceBuilder.create()
				.url("jdbc:h2:mem:shopwavefusion;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
				.username("sa")
				.password("")
				.driverClassName("org.h2.Driver")
				.build();
	}
}