# GigaChat Tokenizer Spring Boot Starter

Это kotlin/java библиотека для точного подсчета токенов и управления контекстным окном в проектах, использующих модели GigaChat 
(разработанная компанией Сбер).
Основана на оригинальных конфигурациях токенизаторов GigaChat Ultra (модели v3, v3.1)
движок HuggingFace Tokenizers.

✨ **Возможности**

* **Точный подсчет:** Полное соответствие логике токенизации GigaChat.
  
    💡 Примечание: Расчет включает технический токен начала строки \<s> (BOS). Это необходимо для точного соблюдения лимитов модели GigaChat, поэтому значение может быть на 1 больше, чем при подсчете слов через web API GigaChat.
* **Chat Markup:** Автоматический расчет токенов для списка сообщений (List<ChatMessage>) с учетом системных промптов и
  ролей.
* **Умная обрезка (Smart Clamping):** Обрезает текст до лимита токенов, стараясь сохранить целостность предложений (
  завершение на знаках препинания).
* Spring Boot Auto-configuration: Подключается одной зависимостью, настройки через application.yaml.

🚀 **Быстрый старт**

**1. Добавьте зависимость**
В ваш build.gradle.kts:

``` 
    dependencies {
      implementation("io.github.igorsidorov:gigachat-tokenizer-starter:0.1.0")
    }
   ```
Или в pom.xml:
```
    <dependency>
        <groupId>io.github.igorsidorov</groupId>
        <artifactId>gigachat-tokenizer-starter</artifactId>
        <version>0.1.0</version>
    </dependency>
```

**2. Использование в сервисе**
```
    @Service
    class MyChatService(private val tokenService: TokenEstimatorService) {

        fun processLongText(text: String) {
            val count = tokenService.countTokens(text)
            println("Количество токенов: $count")

            // Обрезаем текст до 1000 токенов "красиво"
            val safeText = tokenService.clampToTokens(text, 1000)
        }

        fun checkChatLimit(messages: List<ChatMessage>) {
            val total = tokenService.countChatTokens(messages)
            if (total > 8192) {
                // Логика сокращения истории
            }
        }
    }
```

**3. ⚙️ Конфигурация**
Все настройки по умолчанию уже заданы. Если нужно изменить модель или путь:
```   
    gigachat:
        tokenizer:
            model: "ultra"                # Доступно: ultra (v3/V3.1), custom
            customPath: "path/to/model"   # Путь к модели (для custom)
            majority-threshold: 0.7       # Порог для сохранения смысла при обрезке (0.0 - 1.0)
```

**🛠 Технические детали**

* Движок: ai.djl.huggingface:tokenizers
* Формат логов: Используется стандартный SLF4J.
* Ресурсы: Токенизаторы поставляются внутри JAR и распаковываются во временную директорию при старте,(автоматическая
  очистка при выключении).

## Лицензия
Распространяется под лицензией Apache 2.0. Подробности в файле [LICENSE](LICENSE).