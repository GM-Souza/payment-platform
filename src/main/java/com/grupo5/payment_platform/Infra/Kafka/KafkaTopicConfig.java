//package com.grupo5.payment_platform.Infra.Kafka;
//
//import org.apache.kafka.clients.admin.AdminClientConfig;
//import org.apache.kafka.clients.admin.NewTopic;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.KafkaAdmin;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class KafkaTopicConfig {
//
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapAddress;
//
//    //Neste metodo estamos configurando o KafkaAdmin para gerenciar topicos no Kafka
//    //E falamos qual o endereco do servidor Kafka,que recebe a variavel bootstrapAddress com o value que vem do properties
//    @Bean
//    public KafkaAdmin kafkaAdmin() {
//        Map<String, Object> configs = new HashMap<>();
//        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
//        return new KafkaAdmin(configs);
//    }
//
//    @Bean
//    public NewTopic topicOrderProcessed() {
//        return new NewTopic("payment_order_processed", 2, (short) 1);
//    }
//
//
//}