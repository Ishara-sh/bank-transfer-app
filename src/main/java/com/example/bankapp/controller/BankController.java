package com.example.bankapp.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bankapp.dto.TransferRequest;

import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {

        model.addAttribute("customers",
                bankService.searchCustomers(keyword));

        model.addAttribute("transactions",
                bankService.getLatestTransactions());

        model.addAttribute("totalBalance",
                bankService.getTotalBalance());

        model.addAttribute("successCount",
                bankService.getSuccessfulCount());

        model.addAttribute("failedCount",
                bankService.getFailedCount());

        model.addAttribute("keyword", keyword);

        model.addAttribute("transferRequest", new TransferRequest());

        model.addAttribute("customer", new Customer());

        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(
            @org.springframework.web.bind.annotation.ModelAttribute("customer")
            Customer customer,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.createCustomer(customer);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Customer added successfully");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(
            @Valid TransferRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());

            return "redirect:/";
        }

        try {

            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure());

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Transfer completed successfully");

        } catch (Exception e) {

            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage());

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(
            @PathVariable String accountNumber,
            Model model) {

        model.addAttribute(
                "customer", bankService.getCustomer(accountNumber));

        model.addAttribute(
                "transactions",
                bankService.getAccountHistory(accountNumber));

        model.addAttribute(
                "totalSent",
                bankService.getTotalSent(accountNumber));

        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {

        model.addAttribute(
                "transactions",
                bankService.getLargeTransfers(min));

        model.addAttribute("min", min);

        return "large-transfers";
    }

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.deposit(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Deposit completed successfully");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.withdraw(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Withdrawal completed successfully");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/transfer-with-fee")
    public String transferWithFee(
            @RequestParam String fromAccount,
            @RequestParam String toAccount,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.transferWithFee(
                    fromAccount, toAccount, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Transfer with fee completed successfully");

        } catch (Exception e) {

            auditService.recordFailure(
                    fromAccount, toAccount, amount, e.getMessage());

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }
}