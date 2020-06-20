package com.github.saiprasadkrishnamurthy.datawatcher.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyDefinition
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKey
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKeyType
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyDefinitionRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyKeyRepository
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.KeyPairGenerator
import java.util.*


/**
 * @author Sai.
 */
@RequestMapping("/api/v1/")
@RestController
class PolicyResource(val policyDefinitionRepository: PolicyDefinitionRepository,
                     val policyKeyRepository: PolicyKeyRepository) {

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