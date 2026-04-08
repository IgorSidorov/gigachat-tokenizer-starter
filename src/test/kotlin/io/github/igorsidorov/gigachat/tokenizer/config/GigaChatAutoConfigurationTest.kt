package io.github.igorsidorov.gigachat.tokenizer.config

import io.github.igorsidorov.gigachat.tokenizer.service.GigaChatTokenEstimatorService
import io.github.igorsidorov.gigachat.tokenizer.service.TokenEstimatorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 *
 * @author SidorovIV
 * @created 03.04.2026
 */
class GigaChatAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(GigaChatTokenizerAutoConfiguration::class.java))

    @Test
    fun `should create TokenEstimatorService bean when configured`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(TokenEstimatorService::class.java)
            assertThat(context).hasSingleBean(GigaChatTokenEstimatorService::class.java)
        }
    }

    @Test
    fun `should allow overriding properties`() {
        contextRunner
            .withPropertyValues("gigachat.tokenizer.model=ultra")
            .run { context ->
             context.getBean(GigaChatTokenEstimatorService::class.java)
             assertThat(context).hasBean("tokenEstimatorService")
            }
    }
}