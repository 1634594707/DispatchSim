package com.dispatchsim.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dispatch-sim.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    private final AppProperties properties;

    @Bean
    DirectExchange eventExchange() {
        return new DirectExchange(properties.getRabbit().getEventExchange(), true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(properties.getRabbit().getDeadLetterExchange(), true, false);
    }

    @Bean
    Queue orderEventQueue() {
        return new Queue(properties.getRabbit().getOrderQueue(), true);
    }

    @Bean
    Queue vehicleEventQueue() {
        return new Queue(properties.getRabbit().getVehicleQueue(), true);
    }

    @Bean
    Queue deadLetterQueue() {
        return new Queue(properties.getRabbit().getDeadLetterQueue(), true);
    }

    @Bean
    Binding orderEventBinding() {
        return BindingBuilder.bind(orderEventQueue()).to(eventExchange()).with("order");
    }

    @Bean
    Binding vehicleEventBinding() {
        return BindingBuilder.bind(vehicleEventQueue()).to(eventExchange()).with("vehicle");
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dead-letter");
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("domainEventMessage", com.dispatchsim.infrastructure.messaging.DomainEventMessage.class);
        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages("com.dispatchsim.infrastructure.messaging");
        classMapper.setDefaultType(com.dispatchsim.infrastructure.messaging.DomainEventMessage.class);
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }
}
