package com.github.saiprasadkrishnamurthy.datawatcher.broadcast

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.datawatcher.config.WebSocketConfig
import com.github.saiprasadkrishnamurthy.datawatcher.model.*
import com.github.saiprasadkrishnamurthy.datawatcher.repository.LongPollersRepository
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyKeyRepository
import io.nats.client.Connection
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*


@Service
class ResultsBroadcaster(private val webSocketConfig: WebSocketConfig,
                         private val longPollersRepository: LongPollersRepository,
                         private val natsConnection: Connection,
                         private val policyKeyRepository: PolicyKeyRepository) {

    private val OBJECT_MAPPER = jacksonObjectMapper()

    fun broadcastResults(policyId: String, policyEvaluationResult: PolicyEvaluationResult) {
        broadcastViaKafka(policyId, policyEvaluationResult)
        broadcastViaNats(policyId, policyEvaluationResult)
        broadcastViaWebHook(policyId, policyEvaluationResult)
        // may be more...
    }

    fun resultsReceived(policyId: String, policyEvaluationResult: PolicyEvaluationResult) {
        try {
            verifySignature(policyEvaluationResult)
        } catch (pte: PolicyTamperedException) {
            broadcastErrorLongPolling(policyId, pte)
            return
        } catch (sie: SignatureInvalidException) {
            broadcastErrorLongPolling(policyId, sie)
            return
        } catch (ex: Exception) {
            broadcastErrorLongPolling(policyId, ex)
        }
        broadcastViaLongPolling(policyId, policyEvaluationResult)
        broadcastViaWebsocket(policyId, policyEvaluationResult)
    }

    private fun verifySignature(policyEvaluationResult: PolicyEvaluationResult) {
        val policyKey = policyKeyRepository.findByPolicyIdAndPolicyKeyType(policyId = policyEvaluationResult.policyId, policyKeyType = PolicyKeyType.DataRequestor)
        if (policyKey != null) {
            val digest = policyKey.digest
            val externalPublicKey = policyKey.externalPublicKey

            // check digest.
            val kf = KeyFactory.getInstance("RSA")
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(externalPublicKey))
            val publicKey = kf.generatePublic(keySpec) as RSAPublicKey
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            val messageBytes = OBJECT_MAPPER.writeValueAsBytes(policyEvaluationResult.copy(signature = "")) // exclude the signature.
            signature.update(messageBytes)
            val policyTampered = policyEvaluationResult.policyDigest != digest
            val isInvalidSignature = !signature.verify(Base64.getDecoder().decode(policyEvaluationResult.signature))
            if (policyTampered) throw PolicyTamperedException(policyKey.id)
            if (isInvalidSignature) throw SignatureInvalidException(policyKey.id)
        } else {
            throw PolicyNotFoundException(policyEvaluationResult.policyId)
        }
    }


    private fun broadcastViaNats(policyId: String, policyEvaluationResult: PolicyEvaluationResult) {
        natsConnection.publish(policyId, jacksonObjectMapper().writeValueAsBytes(policyEvaluationResult));
    }

    private fun broadcastViaLongPolling(policyId: String, policyEvaluationResult: PolicyEvaluationResult) {
        longPollersRepository.get(policyId)
                .filter {
                    !it.isSetOrExpired
                }
                .forEach {
                    it.setResult(policyEvaluationResult)
                }
    }

    private fun broadcastErrorLongPolling(policyId: String, error: Exception) {
        longPollersRepository.get(policyId)
                .filter {
                    !it.isSetOrExpired
                }
                .forEach {
                    it.setErrorResult(error)
                }
    }

    private fun broadcastViaWebsocket(policyId: String, policyEvaluationResult: PolicyEvaluationResult) {
        webSocketConfig.websocketTemplate().convertAndSend("/$policyId", policyEvaluationResult)
    }

    private fun broadcastViaWebHook(policyId: String, policyEvaluationResult: PolicyEvaluationResult) {
        // TODO
    }

    private fun broadcastViaKafka(policyId: String, policyEvaluationResult: PolicyEvaluationResult) {
        // TODO
    }
}