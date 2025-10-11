package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.Infra.Kafka.TransactionNotificationDTO;
import com.grupo5.payment_platform.Infra.Kafka.ValueTransactionDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionKafkaService {

    private final KafkaTemplate<String, TransactionNotificationDTO> kafkaTemplate;
    private final KafkaTemplate<String, ValueTransactionDTO> transactionKafkaTemplate;

    public TransactionKafkaService(KafkaTemplate<String, TransactionNotificationDTO> kafkaTemplate, KafkaTemplate<String, ValueTransactionDTO> transactionKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.transactionKafkaTemplate = transactionKafkaTemplate;
    }

    //Metodo que envia a notificacao para o topico do Kafka
    public void sendTransactionNotification(ValueTransactionDTO valueTransactionDTO) {
        transactionKafkaTemplate.send("payment_order_processed", valueTransactionDTO);
    }
    public void sendWelcomeNotification(TransactionNotificationDTO dto) {
        kafkaTemplate.send("welcome_email", dto);
    }
    public void sendPaymentSuccessNotification(ValueTransactionDTO valueTransactionDTO) {
        transactionKafkaTemplate.send("payment_success", valueTransactionDTO);
    }
}
