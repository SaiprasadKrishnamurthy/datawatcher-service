package com.github.saiprasadkrishnamurthy.datawatcher.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyEvaluationResult
import io.nats.client.Connection
import io.nats.client.Dispatcher
import org.springframework.stereotype.Repository
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap


@Repository
class NatsChannelsRepository(private val natsConnection: Connection) {

    private val NATS_CHANNELS: MutableMap<String, Dispatcher> = ConcurrentHashMap()
    private val OBJECT_MAPPER = jacksonObjectMapper()

    fun save(policyId: String, fn: (PolicyEvaluationResult) -> Unit) {
        val d = natsConnection.createDispatcher { msg ->
            val str = String(msg.data, StandardCharsets.UTF_8)
            val result = OBJECT_MAPPER.readValue(str, PolicyEvaluationResult::class.java)
            fn(result)
        }
        NATS_CHANNELS.putIfAbsent(policyId, d.subscribe(policyId, policyId))
    }
}