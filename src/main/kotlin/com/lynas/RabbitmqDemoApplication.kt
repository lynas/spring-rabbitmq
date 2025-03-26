package com.lynas

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class RabbitmqDemoApplication

fun main(args: Array<String>) {
    runApplication<RabbitmqDemoApplication>(*args)
}

const val QUEUE_NAME = "QUEUE_NAME1"
const val TOPIC_EXCHANGE_NAME = "TOPIC_EXCHANGE_NAME1"
const val ROUTE_KEY_NAME = "ROUTE_KEY_NAME1"

const val DLQ_NAME = "DLQ_QUEUE_NAME1"
const val DLQ_EXCHANGE_NAME = "DLQ_EXCHANGE_NAME1"
const val DLQ_ROUTE_KEY_NAME = "DLQ_ROUTE_KEY_NAME1"

@Configuration
class RabbitMQConfig {

    @Bean
    fun mainQueue(): Queue {
        val args = mapOf(
            "x-dead-letter-exchange" to DLQ_EXCHANGE_NAME,
            "x-dead-letter-routing-key" to DLQ_ROUTE_KEY_NAME
        )
        return Queue(QUEUE_NAME, true, false, false, args)
    }

    @Bean
    fun mainExchange(): TopicExchange = TopicExchange(TOPIC_EXCHANGE_NAME)

    @Bean
    fun mainBinding(mainQueue: Queue, mainExchange: TopicExchange): Binding =
        BindingBuilder.bind(mainQueue)
            .to(mainExchange)
            .with(ROUTE_KEY_NAME)

    @Bean
    fun deadLetterExchange(): TopicExchange = TopicExchange(DLQ_EXCHANGE_NAME)
    @Bean
    fun deadLetterQueue(): Queue = Queue(DLQ_NAME)

    @Bean
    fun deadLetterBinding(deadLetterQueue: Queue, deadLetterExchange: TopicExchange): Binding =
        BindingBuilder.bind(deadLetterQueue)
            .to(deadLetterExchange)
            .with(DLQ_ROUTE_KEY_NAME)

    @Bean
    fun template(connectionFactory: ConnectionFactory): RabbitTemplate = RabbitTemplate(connectionFactory)
        .apply {
            messageConverter = Jackson2JsonMessageConverter()
        }

    @Bean
    fun rabbitListenerContainerFactory(connectionFactory: ConnectionFactory): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory()
            .apply {
                setConnectionFactory(connectionFactory)
                setMessageConverter(Jackson2JsonMessageConverter())
            }

}

@Service
class Producer(
    val template: RabbitTemplate
) {

    fun sendMessage(message: UserInfo) {
        template.convertAndSend(TOPIC_EXCHANGE_NAME, ROUTE_KEY_NAME, message)
    }
}


@RestController
class DemoController(
    val producer: Producer
) {

    @PostMapping("/testPost")
    fun testPost(@RequestBody userInfo: UserInfo): UserInfo {
        producer.sendMessage(userInfo)
        return userInfo
    }
}

data class UserInfo(
    val name: String,
)

@Service
class Consumer {

    @RabbitListener(queues = [QUEUE_NAME], containerFactory = "rabbitListenerContainerFactory")
    fun consumer(message: UserInfo) {
        println("Consumer received: $message")
    }

    @RabbitListener(queues = [DLQ_NAME], containerFactory = "rabbitListenerContainerFactory")
    fun dlqConsumer(message: UserInfo) {
        println("DLQ received: $message")
    }

}
