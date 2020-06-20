package com.github.saiprasadkrishnamurthy.datawatcher.repository

import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyDefinition
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PolicyDefinitionRepository : CrudRepository<PolicyDefinition, String> {
    fun findBySourceDataType(sourceDataType: String): List<PolicyDefinition>
}