package com.github.saiprasadkrishnamurthy.datawatcher.repository

import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKey
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyKeyType
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PolicyKeyRepository : CrudRepository<PolicyKey, String> {
    fun findByPolicyIdAndPolicyKeyType(policyId: String, policyKeyType: PolicyKeyType): PolicyKey?
    fun deleteByPolicyId(policyId: String)
}