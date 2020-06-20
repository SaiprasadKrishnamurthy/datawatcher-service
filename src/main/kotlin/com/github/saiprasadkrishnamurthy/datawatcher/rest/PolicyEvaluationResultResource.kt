package com.github.saiprasadkrishnamurthy.datawatcher.rest

import com.fasterxml.jackson.databind.JsonNode
import com.github.saiprasadkrishnamurthy.datawatcher.broadcast.ResultsBroadcaster
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyEvaluationService
import com.github.saiprasadkrishnamurthy.datawatcher.repository.LongPollersRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyDefinitionRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture


/**
 * @author Sai.
 */
@RequestMapping("/api/v1/")
@RestController
class PolicyEvaluationResultResource(val policyEvaluationService: PolicyEvaluationService,
                                     val policyDefinitionRepository: PolicyDefinitionRepository,
                                     val longPollersRepository: LongPollersRepository,
                                     val resultsBroadcaster: ResultsBroadcaster) {

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

    @PostMapping("policy-source-data-type/{policySourceDataType}/_evaluate", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun evaluateForPolicyType(@PathVariable("policySourceDataType") policySourceDataType: String, @RequestBody jsonNode: JsonNode): ResponseEntity<*> {
        CompletableFuture.runAsync {
            policyDefinitionRepository.findBySourceDataType(policySourceDataType.trim()).forEach {
                resultsBroadcaster.broadcastResults(it.id, policyEvaluationService.evaluate(it.id, jsonNode))
            }
        }
        return ResponseEntity("OK", HttpStatus.OK)
    }
}