package com.github.saiprasadkrishnamurthy.datawatcher.rest

import com.github.saiprasadkrishnamurthy.datawatcher.broadcast.ResultsBroadcaster
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyEvaluationResult
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKeyType
import com.github.saiprasadkrishnamurthy.datawatcher.repository.LongPollersRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.NatsChannelsRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyKeyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult


/**
 * @author Sai.
 */
@RequestMapping("/api/v1/")
@RestController
class PolicyEvaluationResultListenerResource(val policyKeyRepository: PolicyKeyRepository,
                                             val longPollersRepository: LongPollersRepository,
                                             val natsChannelsRepository: NatsChannelsRepository,
                                             val resultsBroadcaster: ResultsBroadcaster,
                                             @Value("\${longPollersTimeoutSeconds}") val longPollersTimeoutSeconds: Long) {

    @GetMapping("/watch/{policyId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun watch(@PathVariable("policyId") policyId: String, @RequestParam("policyDigest") policyDigest: String): Any {
        val policyKey = policyKeyRepository.findByPolicyIdAndPolicyKeyType(policyId.trim(), PolicyKeyType.DataRequestor)!!
        val digest = policyKey.digest
        if (digest != policyDigest) {
            return ResponseEntity(mapOf("error" to "Invalid Policy"), HttpStatus.BAD_REQUEST)
        }
        val output = DeferredResult<PolicyEvaluationResult>(longPollersTimeoutSeconds)
        longPollersRepository.save(policyId.trim(), output)
        natsChannelsRepository.save(policyId.trim()) { result -> resultsBroadcaster.resultsReceived(policyId, result) }
        return output
    }

    @GetMapping("/watch-websocket/{policyId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun watchViaWebsocket(@PathVariable("policyId") policyId: String, @RequestParam("policyDigest") policyDigest: String): Any {
        val policyKey = policyKeyRepository.findByPolicyIdAndPolicyKeyType(policyId.trim(), PolicyKeyType.DataRequestor)!!
        val digest = policyKey.digest
        if (digest != policyDigest) {
            return ResponseEntity(mapOf("error" to "Invalid Policy"), HttpStatus.BAD_REQUEST)
        }
        natsChannelsRepository.save(policyId.trim()) { result -> resultsBroadcaster.resultsReceived(policyId, result) }
        return ResponseEntity(mapOf("status" to "Websocket ready on $policyId"), HttpStatus.OK)
    }
}