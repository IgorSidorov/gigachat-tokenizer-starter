package io.github.igorsidorov.gigachat.tokenizer.service

import io.github.igorsidorov.gigachat.tokenizer.config.GigaChatTokenizerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader

/**
 * [GigaChatTokenEstimatorService]
 * @author SidorovIV
 * @created 03.04.2026
 */
class GigaChatTokenEstimatorServiceTest {

    private lateinit var service: GigaChatTokenEstimatorService
    private val properties = GigaChatTokenizerProperties()
    private val resourceLoader = DefaultResourceLoader()

    @BeforeEach
    fun setup() {
        service = GigaChatTokenEstimatorService(properties, resourceLoader)
        service.init()
    }

    @AfterEach
    fun tearDown() {
        service.shutdown()
    }

    @Test
    fun `countTokens should return correct number of tokens`() {
        val text = "Привет, Гигачат!"
        val count = service.countTokens(text)

        assertThat(count).isGreaterThan(0)
        assertThat(count).isEqualTo(8)
    }

    @Test
    fun `countChatTokens should account for roles and special tokens`() {
        val messages = listOf(
            ChatMessage(role = "user", content = "Привет")
        )

        val total = service.countChatTokens(messages)

        assertThat(total).isGreaterThan(service.countTokens("Привет"))
    }

    @Test
    fun `clampToTokens should truncate text nicely at punctuation`() {
        val longText = "Это первое предложение. Это второе предложение. Это третье."
        val limit = 5

        val result = service.clampToTokens(longText, limit)

        assertThat(result).isEqualTo("Это первое предложение.")
        assertThat(service.countTokens(result)).isLessThanOrEqualTo(limit)
    }

    @Test
    fun `clampToTokens should do hard trim if punctuation is too far`() {
        val textWithoutPunctuation = "Один два три четыре пять шесть семь восемь девять десять"
        val limit = 3

        val result = service.clampToTokens(textWithoutPunctuation, limit)

        assertThat(service.countTokens(result)).isEqualTo(limit)
    }

    @Test
    fun `fitsInLimit should return true if text is small`() {
        assertThat(service.fitsInLimit("Short text", 100)).isTrue()
        assertThat(service.fitsInLimit("Very long text", 1)).isFalse()
    }

    @Test
    fun `getTokens should return non-empty array`() {
        val ids = service.getTokens("Test")
        assertThat(ids).isNotEmpty
        assertThat(ids[0]).isInstanceOf(Integer::class.java)
    }
}