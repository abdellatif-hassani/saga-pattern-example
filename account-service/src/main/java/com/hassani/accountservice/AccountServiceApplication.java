package com.hassani.accountservice;

import com.hassani.accountservice.entities.Account;
import com.hassani.accountservice.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner init(AccountRepository accountRepository) {
        return args -> {
            accountRepository.save(new Account(null ,"1001", 500.00));
            accountRepository.save(new Account(null ,"7812", 0.00));
        };
    }
}
