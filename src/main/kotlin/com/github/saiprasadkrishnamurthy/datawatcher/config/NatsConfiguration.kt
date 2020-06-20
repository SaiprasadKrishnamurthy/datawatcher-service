package com.github.saiprasadkrishnamurthy.datawatcher.config

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class NatsConfiguration {

    companion object {
        val LOG = LoggerFactory.getLogger(NatsConfiguration::class.java)
    }

    @Bean
    fun natsConnection(@Value("\${nats.url}") natsUrl: String): Connection {
        val options = Options.Builder()
                .server(natsUrl)
                .build()
        return Nats.connect(options)
    }
}