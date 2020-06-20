package com.github.saiprasadkrishnamurthy.datawatcher.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyDefinition
import com.github.saiprasadkrishnamurthy.datawatcher.repository.PolicyDefinitionRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths
import java.util.concurrent.ForkJoinPool

@Configuration
class DataWatcherConfiguration {

    @Bean("workerPool")
    fun workerPool(): ForkJoinPool = ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true)

   /* @Bean
    fun command(policyDefinitionRepository: PolicyDefinitionRepository) = CommandLineRunner {
        val mapper = jacksonObjectMapper()
        val p = mapper.readValue(Paths.get("policy.json").toFile(), PolicyDefinition::class.java)
        policyDefinitionRepository.save(p)
    }*/
}