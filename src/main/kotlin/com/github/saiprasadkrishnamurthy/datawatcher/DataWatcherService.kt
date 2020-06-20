package com.github.saiprasadkrishnamurthy.datawatcher

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableCaching
@EnableSwagger2
@EnableScheduling
@EnableEncryptableProperties
class DataWatcherService

fun main(args: Array<String>) {
    runApplication<DataWatcherService>(*args)
}
