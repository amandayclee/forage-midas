package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final IncentiveService incentiveService;

    public TransactionService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            IncentiveService incentiveService
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.incentiveService = incentiveService;
    }

    @Transactional
    public void processTransaction(Transaction transaction) {
        try {
            UserRecord sender = userRepository.findById(transaction.getSenderId());
            UserRecord recipient = userRepository.findById(transaction.getRecipientId());

            // Log initial balances if wilbur involved
            if (sender.getName().equals("wilbur") || recipient.getName().equals("wilbur")) {
                logger.info("Before transaction - Wilbur's balance: {}",
                        userRepository.findByName("wilbur").getBalance());
            }

            // 驗證交易
            validateTransaction(sender, transaction);

            // 獲取獎勵金額
            float incentiveAmount = incentiveService.getIncentiveAmount(transaction);

            // 更新餘額
            // 發送方扣除交易金額
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            // 接收方增加交易金額和獎勵金額
            recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

            // 儲存用戶狀態
            userRepository.save(sender);
            userRepository.save(recipient);

            // 儲存交易記錄
            TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
            transactionRepository.save(record);

            // Log final balances if wilbur involved
            if (sender.getName().equals("wilbur") || recipient.getName().equals("wilbur")) {
                logger.info("After transaction - Wilbur's balance: {}",
                        userRepository.findByName("wilbur").getBalance());
            }

        } catch (IllegalStateException e) {
            // 記錄錯誤但不中斷流程
            logger.warn("Transaction failed: {} -> {}", transaction, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing transaction: {} -> {}", transaction, e.getMessage());
            throw e;
        }
    }

    private void validateTransaction(UserRecord sender, Transaction transaction) {
        if (sender.getBalance() < transaction.getAmount()) {
            throw new IllegalStateException("Insufficient funds");
        }
    }
}