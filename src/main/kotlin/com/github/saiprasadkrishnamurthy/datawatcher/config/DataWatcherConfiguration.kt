package com.github.saiprasadkrishnamurthy.datawatcher.config

import com.google.common.base.Predicates
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import java.util.concurrent.ForkJoinPool

@Configuration
class DataWatcherConfiguration {

    @Bean("workerPool")
    fun workerPool(): ForkJoinPool = ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true)

    @Bean
    fun configApi(environment: Environment): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .groupName("data-watcher-service")
                .apiInfo(apiInfo(environment))
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(Predicates.not(PathSelectors.regex("/error"))) // Exclude Spring error controllers
                .build()
    }

    private fun apiInfo(environment: Environment): ApiInfo {
        return ApiInfoBuilder()
                .title("Data Watcher Service | REST APIs")
                .contact(Contact("Saiprasad Krishnamurthy", "https://github.com/SaiprasadKrishnamurthy", "saiprasad.krishnamurthy@gmail.com"))
                .version("Build: " + environment.getProperty("build.version")!!)
                .build()
    }
}