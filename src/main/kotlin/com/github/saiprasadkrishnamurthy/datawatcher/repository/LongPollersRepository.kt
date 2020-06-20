package com.github.saiprasadkrishnamurthy.datawatcher.repository

import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyEvaluationResult
import net.jodah.expiringmap.ExpiringMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import org.springframework.web.context.request.async.DeferredResult
import java.util.concurrent.TimeUnit


@Repository
class LongPollersRepository(
        @Value("\${longPollersPersistenceRetentionSeconds}") val longPollersPersistenceRetentionSeconds: Long,
        @Value("\${longPollersStorageSize}") val longPollersStorageSize: Int) {

    val LONG_POLLERS: MutableMap<String, MutableList<DeferredResult<PolicyEvaluationResult>>> = ExpiringMap.builder()
            .maxSize(longPollersStorageSize)
            .expiration(longPollersPersistenceRetentionSeconds, TimeUnit.SECONDS)
            .build()

    fun save(policyId: String, deferredResult: DeferredResult<PolicyEvaluationResult>) {
        LONG_POLLERS.compute(policyId) { _, v ->
            if (v == null) {
                mutableListOf(deferredResult)
            } else {
                v.add(deferredResult)
                v
            }
        }
    }

    fun get(policyId: String): MutableList<DeferredResult<PolicyEvaluationResult>> {
        return LONG_POLLERS.getOrDefault(policyId, mutableListOf())
    }

    @Scheduled(fixedDelayString = "\${longPollersCleanupSchedulerIntervalMillis}")
    fun cleanupDeadConnections() {
        LONG_POLLERS.forEach { (_, u) ->
            u.removeIf { dr ->
                dr.isSetOrExpired
            }
        }
    }
}