# joker-migrator

Демо к мастер-классу на Joker 2026: миграция Java-сервиса со Spring Boot 2.7 / Java 11 на Spring Boot 4.0 / Java 25 с помощью OpenRewrite и скилла для coding-агента.

Идея: **рецепты OpenRewrite делают основную работу детерминированно, LLM доводит только хвост, а правоту определяют компилятор и тесты.** Скилл задаёт порядок работы и правила: не ослаблять тесты, не выбирать версии на глаз, читать выжимки из логов, а не логи целиком.

## Что внутри

```
legacy-shop/      сервис «до миграции»: Spring Boot 2.7.18, Java 11, Gradle 6.8.3 (wrapper),
                  API first (openapi-generator 6.6.0), javax.*, Lombok 1.18.24, corp-commons 1.0.0;
                  71 класс, 60 тестов
corp-commons/     вымышленная «внутренняя» библиотека: v1, v2 с ломающими изменениями
                  и recipes — рецепт миграции на 2.0, который выпускает команда библиотеки
skill/
  openrewrite-migrate/         скилл миграции: SKILL.md, справочники, init-скрипт для Gradle
  openrewrite-recipe-author/   бонус: скилл для команд библиотек — пишет и публикует рецепты
```

Ветка `step-0` — состояние до миграции.

## Как запустить

Нужны **JDK 11 и JDK 25** (Gradle 6.8.3 запускается только на JDK ≤ 15, мигрированный проект собирается на 25) и Git. Maven и Gradle ставить не нужно — в проектах есть wrapper.

1. Опубликовать corp-commons и её рецепт в локальный «корпоративный» репозиторий `corp-repo/`:

   ```bash
   cd corp-commons
   JAVA_HOME=<jdk-25> ./gradlew publish
   ```

2. Проверить исходную сборку:

   ```bash
   cd ../legacy-shop
   JAVA_HOME=<jdk-11> ./gradlew build
   ```

3. Подключить скилл к coding-агенту. Для Claude Code — скопировать `skill/openrewrite-migrate` в `.claude/skills/` проекта или в `~/.claude/skills/`; другие агенты с поддержкой формата SKILL.md подключают папку так же. И дать задачу:

   > Обнови legacy-shop: Spring Boot 4, Java 25 и наша corp-commons 2.0.0 (она лежит в ../corp-repo, описание изменений — ../corp-commons/MIGRATION.md). Сборка и тесты должны остаться зелёными.

Скрипты скилла можно запускать и без агента (нужен JDK 17+; JDK для самой сборки они выбирают сами по версии Gradle):

```bash
S=skill/openrewrite-migrate/scripts
# рецепты Spring Boot, Java и внутренний рецепт corp-commons — один прогон и коммит
java $S/RunRecipes.java --project legacy-shop \
  --recipes org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0,org.openrewrite.java.migrate.UpgradeToJava25,com.corp.commons.MigrateToCorpCommons2 \
  --artifacts org.openrewrite.recipe:rewrite-spring:6.37.1,org.openrewrite.recipe:rewrite-migrate-java:3.42.1,com.corp:corp-commons-recipes:2.0.0 \
  --commit "recipe: Spring Boot 2.7 -> 4.0, Java 11 -> 25, corp-commons 2.0"
# сборка: вместо лога на сотни строк — JSON с ошибками, сгруппированными по причине
java $S/BuildReport.java --project legacy-shop --task compile
# pre-commit-хук, который не даёт закоммитить удалённые тесты, @Disabled и убранные проверки
java $S/CheckDiff.java --install-hook
```

После этого остаётся хвост: настройка генератора OpenAPI, Jackson 3, изменения API Spring 7, таймауты corp-commons (`long` → `Duration`) и URL со слешем в конце, который ловит только тест.

## Что показали замеры

Та же задача, один агент со скиллом и один без (Claude Opus 5.5), чистые клоны, метрики сняты отдельным скриптом после чистой пересборки:

| | Со скиллом | Без скилла |
| --- | --- | --- |
| Итог | 60/60 тестов | 60/60 тестов |
| Строк кода, написанных моделью | **90** | **456** |
| Токены | 120 тыс. | 126 тыс. |
| Время | 15 мин | 12 мин |

Обе миграции доходят до зелёной сборки и не ослабляют тесты — сильная модель справляется и без скилла. Разница в том, сколько кода нужно проверить глазами: со скиллом основную часть изменений делают рецепты, детерминированно и с журналом, а коммиты размечены `recipe` / `llm`.

## Ссылки

- Telegram: https://t.me/jvmbrain
- YouTube: https://www.youtube.com/@jvmbrain

## Лицензия

Apache License 2.0 — см. [LICENSE](LICENSE). Рецепты `rewrite-spring` и `rewrite-migrate-java`, которые скачиваются при запуске, распространяются под Moderne Source Available License: их можно бесплатно применять к своему коду, но не продавать как сервис.
