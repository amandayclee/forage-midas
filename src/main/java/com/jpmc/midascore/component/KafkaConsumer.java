package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class KafkaConsumer {
    private final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final List<Float> firstFourTransactions = new ArrayList<>();

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-group")
    public void receive(Transaction transaction) {
        if (firstFourTransactions.size() < 4) {
            float amount = transaction.getAmount();
            firstFourTransactions.add(amount);
            logger.info("Received transaction #{}: amount = {}",
                    firstFourTransactions.size(), amount);
        }

        if (firstFourTransactions.size() == 4) {
            logger.info("First four transaction amounts: {}", firstFourTransactions);
        }
    }
}