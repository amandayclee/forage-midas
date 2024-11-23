package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class KafkaConsumer {
    private final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final List<Float> firstFourTransactions = new ArrayList<>();
    private UserRecord waldorf = null;  // 用來追蹤 waldorf 的引用

    @Autowired
    private DatabaseConduit databaseConduit;

    @Autowired
    private TransactionRepository transactionRepository;

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-group")
    @Transactional
    public void receive(Transaction transaction) {
//        if (firstFourTransactions.size() < 4) {
//            float amount = transaction.getAmount();
//            firstFourTransactions.add(amount);
//            logger.info("Received transaction #{}: amount = {}",
//                    firstFourTransactions.size(), amount);
//        }
//
//        if (firstFourTransactions.size() == 4) {
//            logger.info("First four transaction amounts: {}", firstFourTransactions);
//        }

        UserRecord sender = databaseConduit.findUserById(transaction.getSenderId());
        UserRecord recipient = databaseConduit.findUserById(transaction.getRecipientId());

        if (sender == null || recipient == null) {
            logger.warn("Invalid transaction: sender or recipient not found");
            return;
        }

        if (waldorf == null && (sender.getName().equals("waldorf") || recipient.getName().equals("waldorf"))) {
            waldorf = sender.getName().equals("waldorf") ? sender : recipient;
            logger.info("Found waldorf! Initial balance: {}", waldorf.getBalance());
        }

        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Invalid transaction: insufficient funds");
            return;
        }

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount());

        // 記錄 waldorf 的餘額變化
        if (sender.getName().equals("waldorf") || recipient.getName().equals("waldorf")) {
            logger.info("Waldorf's balance updated to: {}",
                    sender.getName().equals("waldorf") ? sender.getBalance() : recipient.getBalance());
        }

        // 儲存更新後的資料
        databaseConduit.save(sender);
        databaseConduit.save(recipient);

        // 儲存交易紀錄
        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount());
        transactionRepository.save(record);

        logger.info("Transaction processed successfully: {}", transaction);
    }
}