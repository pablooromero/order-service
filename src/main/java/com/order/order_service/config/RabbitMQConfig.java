package com.order.order_service.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "email-exchange";
    public static final String QUEUE_PDF = "pdf-queue";

    @Bean
    public TopicExchange mailExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue pdfQueue() {
        return new Queue(QUEUE_PDF, true);
    }

    @Bean
    public Binding pdfBinding(Queue pdfQueue, TopicExchange mailExchange) {
        return BindingBuilder.bind(pdfQueue).to(mailExchange).with("user.pdf");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}