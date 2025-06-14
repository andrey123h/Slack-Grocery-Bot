package com.andreycorp.slack_grocery_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@SpringBootApplication
public class SlackGroceryBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SlackGroceryBotApplication.class, args);
	}

}
