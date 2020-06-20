package com.github.saiprasadkrishnamurthy.datawatcher.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.datawatcher.broadcast.ResultsBroadcaster
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyDefinition
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyEvaluationResult
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKey
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKeyType
import com.github.saiprasadkrishnamurthy.datawatcher.repository.LongPollersRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.NatsChannelsRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyDefinitionRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyKeyRepository
import io.swagger.annotations.Api
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.async.DeferredResult
import java.security.KeyPairGenerator
import java.util.*


/**
 * @author Sai.
 */
@Api(tags = ["DATA REQUESTOR"])
@RequestMapping("/api/v1/")
@RestController
class DataRequestorResource(val policyKeyRepository: PolicyKeyRepository,
                            val longPollersRepository: LongPollersRepository,
                            val natsChannelsRepository: NatsChannelsRepository,
                            val resultsBroadcaster: ResultsBroadcaster,
                            val policyDefinitionRepository: PolicyDefinitionRepository,
                            @Value("\${longPollersTimeoutSeconds}")
                            val longPollersTimeoutSeconds: Long) {

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

    @PutMapping("policy/_create", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun saveOrUpdatePolicy(@RequestBody policyDefinition: PolicyDefinition): ResponseEntity<*> {
        policyDefinitionRepository.deleteById(policyDefinition.id)
        policyKeyRepository.deleteByPolicyId(policyDefinition.id)

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        val pub = Base64.getEncoder().encodeToString(kp.public.encoded)
        val pvt = Base64.getEncoder().encodeToString(kp.private.encoded)
        val digest = DigestUtils.sha256Hex(jacksonObjectMapper().writeValueAsString(policyDefinition))
        val policyDefinition = policyDefinition.copy(publicKey = pub)
        val policyKey = PolicyKey(id = UUID.randomUUID().toString(), privateKey = pvt, digest = digest,
                policyId = policyDefinition.id,
                externalPublicKey = "",
                selfPublicKey = policyDefinition.publicKey,
                policyKeyType = PolicyKeyType.DataRequestor)
        policyDefinitionRepository.save(policyDefinition)
        policyKeyRepository.save(policyKey)
        return ResponseEntity(mapOf("result" to "OK"), HttpStatus.OK)
    }

    @PutMapping("policy/{policyId}/external-public-key", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun updateExternalPublicKey(@PathVariable("policyId") policyId: String, @RequestBody key: String): ResponseEntity<*> {
        val policyKey = policyKeyRepository.findByPolicyIdAndPolicyKeyType(policyId.trim(), PolicyKeyType.DataRequestor)
                ?: return ResponseEntity(mapOf("error" to "Policy Not found with id: $policyId"), HttpStatus.BAD_REQUEST)
        val pk = policyKey.copy(externalPublicKey = key)
        policyKeyRepository.save(pk)
        return ResponseEntity(mapOf("result" to "OK"), HttpStatus.OK)
    }


}