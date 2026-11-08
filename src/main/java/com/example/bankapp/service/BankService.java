package com.example.bankapp.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllCustomers();
        }

        return customerRepository.searchByName(keyword);
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total == null ? BigDecimal.ZERO : total;
    }

    public long getSuccessfulCount() {
        return transactionRepository.countByStatus(TransactionStatus.SUCCESS);
    }

    public long getFailedCount() {
        return transactionRepository.countByStatus(TransactionStatus.FAILED);
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountNumber));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {

        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number already exists");
        }

        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email already exists");
        }

        if (customer.getBalance() == null ||
                customer.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BankException("Invalid initial balance");
        }

        return customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure) throws Exception {

        if (fromAccount.equals(toAccount)) {
            throw new BankException("Accounts must be different accounts");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than zero");
        }

        Customer sender = customerRepository.getByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Sender account not found"));

        customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Receiver account not found"));

        int debited = customerRepository.debit(fromAccount, amount);

        if (debited == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        if (simulateFailure) {
            throw new BankException(
                    "All changes were rolled back - simulated system failure");
        }

        int credited = customerRepository.credit(toAccount, amount);

        if (credited == 0) {
            throw new BankException("Could not credit receiver account");
        }

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Bank transfer completed successfully");

        return transactionRepository.save(transaction);
    }

    @Transactional
    public BankTransaction deposit(
            String accountNumber,
            BigDecimal amount) {

        validateAmount(amount);
        getCustomer(accountNumber);

        int updated = customerRepository.credit(accountNumber, amount);

        if (updated == 0) {
            throw new BankException("Deposit failed");
        }

        return saveTransaction(
                null, accountNumber, amount,
                "Deposit completed successfully");
    }

    @Transactional
    public BankTransaction withdraw(
            String accountNumber,
            BigDecimal amount) {

        validateAmount(amount);
        getCustomer(accountNumber);

        int updated = customerRepository.debit(accountNumber, amount);

        if (updated == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance");
        }

        return saveTransaction(
                accountNumber, null, amount,
                "Withdrawal completed successfully");
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transferWithFee(
            String fromAccount,
            String toAccount,
            BigDecimal amount) {

        validateAmount(amount);

        if (fromAccount.equals(toAccount)) {
            throw new BankException("Accounts must be different accounts");
        }

        getCustomer(fromAccount);
        getCustomer(toAccount);
        getCustomer("999999");

        BigDecimal fee = amount.multiply(new BigDecimal("0.01"));

        BigDecimal totalDebit = amount.add(fee);

        if (customerRepository.debit(fromAccount, totalDebit) == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance including transfer fee");
        }

        if (customerRepository.credit(toAccount, amount) == 0) {
            throw new BankException("Receiver credit failed");
        }

        if (customerRepository.credit("999999", fee) == 0) {
            throw new BankException("Bank fee credit failed");
        }

        return saveTransaction(
                fromAccount,
                toAccount,
                amount,
                "Transfer completed with 1% fee: " + fee);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankException("Amount must be greater than zero");
        }
    }

    private BankTransaction saveTransaction(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String description) {

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);

        return transactionRepository.save(transaction);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal minAmount) {
        return transactionRepository.findLargeTransfers(minAmount);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        return transactionRepository.getTotalSent(
                accountNumber, TransactionStatus.SUCCESS);
    }
}