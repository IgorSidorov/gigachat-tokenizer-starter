package io.github.igorsidorov.gigachat.tokenizer.config

import io.github.igorsidorov.gigachat.tokenizer.service.GigaChatTokenEstimatorService
import io.github.igorsidorov.gigachat.tokenizer.service.TokenEstimatorService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader

@AutoConfiguration
@EnableConfigurationProperties(GigaChatTokenizerProperties::class)
class GigaChatTokenizerAutoConfiguration {
    private val log = logger()

    init {
        log.info("GigaChat Tokenizer AutoConfiguration loaded successfully")
    }

    @Bean
    @ConditionalOnMissingBean
    fun tokenEstimatorService(
        props: GigaChatTokenizerProperties,
        resourceLoader: ResourceLoader
    ): TokenEstimatorService = GigaChatTokenEstimatorService(props, resourceLoader)
}

@ConfigurationProperties(prefix = "gigachat.tokenizer")
class GigaChatTokenizerProperties {
    var model: String = "ultra"
    var customPath: String? = null
    var majorityThreshold: Double = 0.7
}

inline fun <reified R : Any> R.logger(): Logger =
    LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion"))

