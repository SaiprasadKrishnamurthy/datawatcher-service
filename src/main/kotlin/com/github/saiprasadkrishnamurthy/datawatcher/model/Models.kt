package com.github.saiprasadkrishnamurthy.datawatcher.model

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

enum class PolicyKeyType {
    PolicyOwner, DataOwner
}

@Document
data class PolicyDefinition(@Id val id: String = UUID.randomUUID().toString(),
                            val name: String, val description: String,
                            val version: Int, val originator: String,
                            val department: String, val sourceDataType: String,
                            val disabled: Boolean,
                            val timestamp: Long = System.currentTimeMillis(),
                            val rules: List<Rule> = listOf(),
                            val threshold: Threshold,
                            val webHook: WebHook,
                            val outputDefinition: String,
                            val publicKey: String = "",
                            val encryptMessage: Boolean = false)

@Document
data class PolicyKey(@Id val id: String = UUID.randomUUID().toString(), val policyId: String, val privateKey: String, val digest: String, val externalPublicKey: String, val selfPublicKey: String, val policyKeyType: PolicyKeyType = PolicyKeyType.PolicyOwner)

data class Rule(val name: String, val description: String, val expression: String, val score: Double)

data class Threshold(val lowerBounds: Double, val upperBounds: Double)

data class WebHook(val url: String, val apiToken: String)

data class PolicyEvaluationResult(val policyId: String, val result: JsonNode, val score: Double, val threshold: Threshold, val signature: String = "", val policyDigest: String)

class PolicyNotFoundException(private val policyId: String) : RuntimeException(policyId)
class PolicyTamperedException(private val policyId: String) : RuntimeException(policyId)
class SignatureInvalidException(private val policyId: String) : RuntimeException(policyId)

interface PolicyEvaluationService {
    fun evaluate(policyId: String, document: JsonNode): PolicyEvaluationResult
}

