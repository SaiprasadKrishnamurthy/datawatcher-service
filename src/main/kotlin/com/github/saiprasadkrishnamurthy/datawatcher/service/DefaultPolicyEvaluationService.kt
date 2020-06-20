package com.github.saiprasadkrishnamurthy.datawatcher.service

import com.api.jsonata4java.expressions.Expressions
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.datawatcher.model.*
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyDefinitionRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyKeyRepository
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*


/**
 * @author Sai.
 */
@Service
class DefaultPolicyEvaluationService(val policyDefinitionRepository: PolicyDefinitionRepository,
                                     val policyKeyRepository: PolicyKeyRepository) : PolicyEvaluationService {
    override fun evaluate(policyId: String, document: JsonNode): PolicyEvaluationResult {
        val policy = policyDefinitionRepository.findById(policyId.trim())
        return policy.map { policyDefinition ->
            val policyKey = policyKeyRepository.findByPolicyIdAndPolicyKeyType(policyDefinition.id, PolicyKeyType.DataOwner)!!
            val score = policyDefinition.rules.map {
                val expr = Expressions.parse(it.expression)
                val result = expr.evaluate(document)
                if (result != null && result.isBoolean && result.booleanValue()) it.score else 0.0
            }.reduce { a, b -> a + b }
            if (score >= policyDefinition.threshold.lowerBounds && score < policyDefinition.threshold.upperBounds) {
                val transformation = Expressions.parse(policyDefinition.outputDefinition)
                val result = PolicyEvaluationResult(policyId, transformation.evaluate(document), score, policyDefinition.threshold, "", policyKey.digest)
                signature(result, policyKey)
            } else {
                val result = PolicyEvaluationResult(policyId, jacksonObjectMapper().readTree("{}"), score, policyDefinition.threshold, "", policyKey.digest)
                signature(result, policyKey)
            }
        }.orElseThrow {
            PolicyNotFoundException(policyId)
        }
    }

    // Sign with the local private key.
    private fun signature(result: PolicyEvaluationResult, policyKey: PolicyKey): PolicyEvaluationResult {
        val pk = policyKey.privateKey
        val kf = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(pk))
        val privateKey = kf.generatePrivate(keySpec) as RSAPrivateKey
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(jacksonObjectMapper().writeValueAsBytes(result))
        val sign = signature.sign()
        return result.copy(signature = Base64.getEncoder().encodeToString(sign))
    }
}