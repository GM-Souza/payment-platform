//package com.grupo5.payment_platform.Services;
//
//import com.grupo5.payment_platform.Enums.EmailSubject;
//import com.grupo5.payment_platform.Infra.Kafka.TransactionNotificationDTO;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//public class TransactionKafkaService {
//
//    private final KafkaTemplate<String, TransactionNotificationDTO> kafkaTemplate;
//
//    public TransactionKafkaService(KafkaTemplate<String, TransactionNotificationDTO> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    // envia duas notificações usando os DTOs fornecidos (sender e receiver)
//    public void sendTransactionNotificationForBoth(TransactionNotificationDTO senderDto, TransactionNotificationDTO receiverDto) {
//        if (senderDto != null) {
//            kafkaTemplate.send("payment_order_processed.sender", senderDto);
//        }
//        if (receiverDto != null) {
//            kafkaTemplate.send("payment_order_processed.receiver", receiverDto);
//        }
//    }
//
//    // mantém método para envio direto de um DTO para welcome_email
//    public void sendWelcomeEmailNotification(TransactionNotificationDTO dto) {
//        if (dto != null) {
//            kafkaTemplate.send("welcome_email", dto);
//        }
//    }
//
//}
