package com.andreycorp.slack_grocery_bot;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.sql.DataSource;
import java.sql.Connection;


@EnableAsync
@SpringBootApplication
public class SlackGroceryBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlackGroceryBotApplication.class, args);
	}

	// ─── Sanity‐check bean ────────────────────────────────────────────────────────
	@Bean
	public CommandLineRunner testDb(DataSource ds) {
		return args -> {
			try (Connection c = ds.getConnection()) {
				System.out.println("Connected to " + c.getMetaData().getURL());
			}
		};
	}

}
