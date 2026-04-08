package io.github.igorsidorov.gigachat.tokenizer.service

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import io.github.igorsidorov.gigachat.tokenizer.config.GigaChatTokenizerProperties
import io.github.igorsidorov.gigachat.tokenizer.config.logger
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.core.io.ResourceLoader
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private const val DOT = "."
private const val EXCLAMATION = "!"
private const val QUESTION = "?"
private const val TEMP_FILE_PREFIX = "gigachat_tokenizer"
private const val TEMP_FILE_SUFFIX = ".json"
private const val END_NOT_FOUND = -1
private const val MAX_LENGTH = "maxLength"
private const val TRUNCATION = "truncation"
private const val FALSE = "false"

/**
 * Основной сервис для оценки объема текста в токенах.
 * Позволяет адаптировать запросы под лимиты контекстного окна GigaChat.
 */
interface TokenEstimatorService {

    /**
     * Считает количество токенов в произвольной строке текста.
     *
     * @param text Исходный текст.
     * @return Количество токенов (Int).
     */
    fun countTokens(text: String): Int

    /**
     * Точный подсчет токенов для списка сообщений чата с учетом разметки GigaChat (v3/Pro).
     * Включает системные токены ролей, переносы строк и финальный токен ответа.
     *
     * @param messages Список объектов [ChatMessage] (role, content).
     * @return Суммарное количество токенов для всего диалога.
     */
    fun countChatTokens(messages: List<ChatMessage>): Int

    /**
     * Проверяет, умещается ли текст в заданный лимит.
     *
     * @param text Текст для проверки.
     * @param limit Максимально допустимое количество токенов.
     * @return true, если текст проходит по лимиту.
     */
    fun fitsInLimit(text: String, limit: Int): Boolean = countTokens(text) <= limit

    /**
     * "Красиво" обрезает текст до указанного лимита токенов.
     * Пытается завершить строку на знаке препинания (., !, ?), если это не удаляет
     * более 30% от целевого объема (порог 0.7).
     *
     * @param text Исходный текст.
     * @param maxTokens Целевой лимит токенов.
     * @return Обрезанная строка.
     */
    fun clampToTokens(text: String, maxTokens: Int): String

    /**
     * Возвращает массив идентификаторов токенов.
     * Используется для низкоуровневой работы с моделью или визуализации токенизации.
     *
     * @param text Исходный текст.
     * @return Массив ID токенов (IntArray).
     */
    fun getTokens(text: String): IntArray
}


class GigaChatTokenEstimatorService(
    private val properties: GigaChatTokenizerProperties,
    private val resourceLoader: ResourceLoader
) : TokenEstimatorService {

    private lateinit var tokenizer: HuggingFaceTokenizer
    private var tempFilePath: Path? = null
    private val log = logger()

    @PostConstruct
    fun init() {
        val resourcePath = if (properties.model == "custom") {
            properties.customPath ?: throw IllegalStateException("customPath must be set for model='custom'")
        } else {
            "classpath:tokenizers/${properties.model}.json"
        }

        try {
            val resource = resourceLoader.getResource(resourcePath)
            val tempFile = Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
            tempFilePath = tempFile

            resource.inputStream.use { input ->
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)
            }

            val options = mapOf(TRUNCATION to FALSE, MAX_LENGTH to Int.MAX_VALUE.toString())
            tokenizer = HuggingFaceTokenizer.newInstance(tempFile, options)

            log.debug("Successfully loaded GigaChat tokenizer from {}", resourcePath)
        } catch (ex: Exception) {
            log.error("Failed to initialize GigaChat tokenizer from path: $resourcePath", ex)
            tempFilePath?.let { Files.deleteIfExists(it) }
            throw ex
        }
    }


    override fun countTokens(text: String): Int = tokenizer.encode(text).ids.size

    override fun getTokens(text: String): IntArray {
        return tokenizer.encode(text).ids.map { it.toInt() }.toIntArray()
    }

    override fun countChatTokens(messages: List<ChatMessage>): Int {
        if (messages.isEmpty()) return 0

        var total = 1

        for (msg in messages) {
            val formattedMsg = """
                <|start_header_id|>${msg.role}<|end_header_id|>
                ${msg.content}<|eot_id|>
            """

            total += countTokens(formattedMsg)
        }

        total += countTokens("<|start_header_id|>assistant<|end_header_id|>")

        return total
    }

    override fun clampToTokens(text: String, maxTokens: Int): String {
        val encoding = tokenizer.encode(text)
        val ids = encoding.ids
        if (ids.size <= maxTokens) return text
        val spans = encoding.charTokenSpans
        if (spans.isEmpty()) return ""
        val targetIdx = maxTokens - 1
        val lastCharIndex = if (targetIdx in spans.indices) {
            spans[targetIdx]?.end ?: 0
        } else {
            spans.lastOrNull()?.end ?: 0
        }
        return if (lastCharIndex <= 0) ""
        else finalizeTruncation(text.substring(0, lastCharIndex))
    }


    private fun finalizeTruncation(rawTruncated: String): String {
        val lastSentenceEnd = rawTruncated.lastIndexOfAny(listOf(DOT, EXCLAMATION, QUESTION))
        val threshold = rawTruncated.length * properties.majorityThreshold

        return if (lastSentenceEnd != END_NOT_FOUND && lastSentenceEnd > threshold) {
            rawTruncated.substring(0, lastSentenceEnd + 1).trim()
        } else {
            rawTruncated.trim()
        }
    }

    @PreDestroy
    fun shutdown() {
        if (::tokenizer.isInitialized) tokenizer.close()
        tempFilePath?.let { Files.deleteIfExists(it) }
    }
}


data class ChatMessage(val role: String, val content: String, val name: String? = null) : Serializable
