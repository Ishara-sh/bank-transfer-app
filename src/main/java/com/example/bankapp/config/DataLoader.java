package com.example.bankapp.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {

        if (customerRepository.count() == 0) {

            Customer kamal = new Customer();
            kamal.setAccountNumber("100001");
            kamal.setFullName("Kamal");
            kamal.setEmail("kamal@example.com");
            kamal.setBalance(new BigDecimal("50000.00"));

            Customer nimali = new Customer();
            nimali.setAccountNumber("100002");
            nimali.setFullName("Nimali");
            nimali.setEmail("nimali@example.com");
            nimali.setBalance(new BigDecimal("25000.00"));

            Customer saman = new Customer();
            saman.setAccountNumber("100003");
            saman.setFullName("Saman");
            saman.setEmail("saman@example.com");
            saman.setBalance(new BigDecimal("10000.00"));

            Customer bank = new Customer();
            bank.setAccountNumber("999999");
            bank.setFullName("Bank Fee Account");
            bank.setEmail("bankfees@example.com");
            bank.setBalance(new BigDecimal("0.00"));

            customerRepository.save(kamal);
            customerRepository.save(nimali);
            customerRepository.save(saman);
            customerRepository.save(bank);
        }
    }
}