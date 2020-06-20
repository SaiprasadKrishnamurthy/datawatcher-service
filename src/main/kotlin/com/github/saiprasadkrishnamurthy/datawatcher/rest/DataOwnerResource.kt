package com.github.saiprasadkrishnamurthy.datawatcher.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.datawatcher.broadcast.ResultsBroadcaster
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyDefinition
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyEvaluationService
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKey
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKeyType
import com.github.saiprasadkrishnamurthy.datawatcher.repository.LongPollersRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyDefinitionRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyKeyRepository
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.KeyPairGenerator
import java.util.*
import java.util.concurrent.CompletableFuture


/**
 * @author Sai.
 */
@RequestMapping("/api/v1/")
@RestController
class DataOwnerResource(val policyEvaluationService: PolicyEvaluationService,
                        val policyDefinitionRepository: PolicyDefinitionRepository,
                        val longPollersRepository: LongPollersRepository,
                        val resultsBroadcaster: ResultsBroadcaster,
                        val policyKeyRepository: PolicyKeyRepository) {

    @PostMapping("policy-source-data-type/{policySourceDataType}/_evaluate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluateForPolicyType(@PathVariable("policySourceDataType") policySourceDataType: String, @RequestBody jsonNode: JsonNode): ResponseEntity<*> {
        CompletableFuture.runAsync {
            policyDefinitionRepository.findBySourceDataType(policySourceDataType.trim()).forEach {
                resultsBroadcaster.broadcastResults(it.id, policyEvaluationService.evaluate(it.id, jsonNode))
            }
        }
        return ResponseEntity("OK", HttpStatus.OK)
    }

    @PostMapping("policy/{policyId}/_evaluate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluateForPolicyId(@PathVariable("policyId") policyId: String, @RequestBody jsonNode: JsonNode): ResponseEntity<*> {
        CompletableFuture.runAsync {
            longPollersRepository.get(policyId)
                    .filter {
                        !it.isSetOrExpired
                    }
                    .forEach {
                        it.setResult(policyEvaluationService.evaluate(policyId, jsonNode))
                    }
        }
        return ResponseEntity("OK", HttpStatus.OK)
    }

    @PutMapping("policy/_deploy", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deployPolicy(@RequestBody policyDefinition: PolicyDefinition): ResponseEntity<*> {
        policyDefinitionRepository.deleteById(policyDefinition.id)
        policyDefinitionRepository.save(policyDefinition)
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.genKeyPair()
        val pub = Base64.getEncoder().encodeToString(kp.public.encoded)
        val pvt = Base64.getEncoder().encodeToString(kp.private.encoded)
        val digest = DigestUtils.sha256Hex(jacksonObjectMapper().writeValueAsString(policyDefinition))
        val policyDefinition = policyDefinition.copy(publicKey = pub)
        val policyKey = PolicyKey(id = UUID.randomUUID().toString(),
                privateKey = pvt, digest = digest,
                externalPublicKey = policyDefinition.publicKey,
                selfPublicKey = pub,
                policyId = policyDefinition.id,
                policyKeyType = PolicyKeyType.DataOwner)
        policyDefinitionRepository.save(policyDefinition)
        policyKeyRepository.save(policyKey)
        return ResponseEntity(mapOf("result" to "OK"), HttpStatus.OK)
    }
}