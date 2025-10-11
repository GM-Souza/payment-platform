package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.Infra.Kafka.TransactionNotificationDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionKafkaService {

    private final KafkaTemplate<String, TransactionNotificationDTO> kafkaTemplate;

    public TransactionKafkaService(KafkaTemplate<String, TransactionNotificationDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    //Metodo que envia a notificacao para o topico do Kafka
    public void sendTransactionNotification(TransactionNotificationDTO dto) {
        kafkaTemplate.send("payment_order_processed", dto);
    }
    public void sendWelcomeEmailNotification(TransactionNotificationDTO dto) {
        kafkaTemplate.send("welcome_email", dto);
    }
}
