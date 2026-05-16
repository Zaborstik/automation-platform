# 📋 Описание YAML файлов в проекте

> **Документ:** назначение YAML-конфигов в корне репозитория и GitHub Actions.  
> **К чему относится:** инфраструктура CI/CD и качества кода (не бизнес-логика приложения).

Этот документ объясняет назначение и использование всех YAML файлов в проекте automation-platform.

---

## 📁 Список YAML файлов

1. **`qodana.yaml`** - Конфигурация статического анализа кода
2. **`.github/workflows/ci.yml`** - CI/CD pipeline для GitHub Actions
3. **`docker-compose.dev-db.yml`** — только PostgreSQL для локальной разработки (корень репозитория); полные стеки см. [`docker/server/`](../docker/server/) и [`docker/local/`](../docker/local/).
4. **`.pre-commit-config.yaml`** - Конфигурация pre-commit hooks

---

## 1. 🔍 `qodana.yaml`

### Что это такое?

**Qodana** - это инструмент статического анализа кода от JetBrains (создатели IntelliJ IDEA). Он проверяет ваш код на:
- Потенциальные баги
- Нарушения best practices
- Проблемы с производительностью
- Уязвимости безопасности
- Плохой стиль кода

### Зачем нужен?

- **Автоматическая проверка качества кода** - находит проблемы до того, как они попадут в production
- **Единые стандарты** - все разработчики следуют одним правилам
- **Интеграция с CI/CD** - автоматически проверяет код при каждом коммите
- **Бесплатный** - community версия доступна бесплатно

### Основные настройки:

```yaml
profile:
  name: qodana.recommended  # Профиль проверок (starter/recommended)
  
projectJDK: "21"  # Версия Java для анализа

failureConditions:  # Пороги для провала CI/CD
  severityThresholds:
    any: 50        # Максимум 50 проблем
    critical: 5    # Максимум 5 критических
```

### Как использовать?

1. **Локально** (в IntelliJ IDEA):
   - Установите плагин Qodana
   - Запустите анализ через меню: `Code → Inspect Code`

2. **В CI/CD**:
   - Автоматически запускается через GitHub Actions (см. `.github/workflows/ci.yml`)
   - Результаты доступны в артефактах workflow

3. **Вручную через Docker**:
   ```bash
   docker run --rm -v "$PWD:/data/project" \
     jetbrains/qodana-jvm-community:2025.2
   ```

### Документация:
- [Официальная документация Qodana](https://www.jetbrains.com/help/qodana/qodana-yaml.html)

---

## 2. 🚀 `.github/workflows/ci.yml`

### Что это такое?

**GitHub Actions** - это встроенная в GitHub система CI/CD (Continuous Integration / Continuous Deployment). Этот файл описывает автоматические действия, которые выполняются при каждом push или pull request.

### Зачем нужен?

- **Автоматическая проверка** - код проверяется автоматически, не нужно запускать тесты вручную
- **Раннее обнаружение проблем** - баги находят до merge в основную ветку
- **Единая среда** - все тесты запускаются в одинаковых условиях
- **История** - видно, когда и какие тесты провалились

### Что делает этот pipeline?

1. **Build and Test Job**:
   - Проверяет код из репозитория
   - Устанавливает Java 21
   - Компилирует проект (`mvn clean compile`)
   - Запускает тесты (`mvn test`)
   - Собирает JAR файлы (`mvn package`)
   - Сохраняет артефакты

2. **Code Quality Job**:
   - Запускает Qodana анализ
   - Сохраняет отчет о качестве кода

3. **Security Scan Job**:
   - Проверяет зависимости на уязвимости (OWASP Dependency Check)
   - Находит известные security issues в библиотеках

### Когда запускается?

- При push в ветки `main` или `develop`
- При создании pull request в эти ветки
- При создании тега версии (например, `v1.0.0`)

### Как посмотреть результаты?

1. Перейдите на вкладку **Actions** в GitHub репозитории
2. Выберите нужный workflow run
3. Посмотрите результаты каждого job
4. Скачайте артефакты (JAR файлы, отчеты)

### Документация:
- [GitHub Actions документация](https://docs.github.com/en/actions)

---

## 3. 🐳 `docker-compose.dev-db.yml`

### Что это такое?

**Docker Compose** описывает один сервис **PostgreSQL** для локальной разработки Java-сервисов. Полные стеки «сервер» и «локальный рантайм» находятся в [`docker/server/`](../docker/server/) и [`docker/local/`](../docker/local/).

### Зачем нужен?

- **Быстрый старт** - не нужно вручную устанавливать и настраивать PostgreSQL
- **Изоляция** - база данных работает в отдельном контейнере, не засоряет систему
- **Воспроизводимость** - у всех разработчиков одинаковая среда
- **Простота** - одна команда запускает всё необходимое

### Что запускается?

**PostgreSQL 16** - база данных для разработки:
- Порт: `5432`
- База данных: `platformdb`
- Пользователь: `platform_user`
- Пароль: `platform_pass`

### Как использовать?

1. **Установите Docker Desktop**:
   - [Скачать Docker Desktop](https://www.docker.com/products/docker-desktop)

2. **Запустите контейнеры**:
   ```bash
   docker compose -f docker-compose.dev-db.yml up -d
   ```
   Флаг `-d` запускает в фоновом режиме (detached)

3. **Проверьте статус**:
   ```bash
   docker compose -f docker-compose.dev-db.yml ps
   ```

4. **Посмотрите логи**:
   ```bash
   docker compose -f docker-compose.dev-db.yml logs -f postgres
   ```

5. **Остановите контейнеры**:
   ```bash
   docker compose -f docker-compose.dev-db.yml down
   ```

6. **Остановите и удалите данные**:
   ```bash
   docker compose -f docker-compose.dev-db.yml down -v
   ```

### Подключение к БД:

После запуска, Spring Boot приложение может подключиться к БД используя настройки:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/platformdb
spring.datasource.username=platform_user
spring.datasource.password=platform_pass
```

### Документация:
- [Docker Compose документация](https://docs.docker.com/compose/)

---

## 4. 🪝 `.pre-commit-config.yaml`

### Что это такое?

**Pre-commit hooks** - это скрипты, которые автоматически выполняются перед каждым git commit. Они проверяют и форматируют код до того, как он попадет в репозиторий.

### Зачем нужен?

- **Качество кода** - автоматически исправляет мелкие проблемы (trailing spaces, end of file)
- **Единый стиль** - все коммиты следуют одним правилам
- **Раннее обнаружение** - находит проблемы до push в репозиторий
- **Экономия времени** - не нужно запускать проверки вручную

### Что проверяется?

1. **end-of-file-fixer** - файлы должны заканчиваться новой строкой
2. **trailing-whitespace** - удаляет пробелы в конце строк
3. **check-yaml** - проверяет синтаксис YAML файлов
4. **check-json** - проверяет синтаксис JSON файлов
5. **check-merge-conflict** - проверяет незакрытые merge конфликты
6. **check-added-large-files** - предупреждает о больших файлах (>500KB)
7. **detect-secrets** - ищет потенциальные секреты (API ключи, пароли)
8. **maven-validate** - проверяет корректность pom.xml

### Как установить?

1. **Установите pre-commit**:
   ```bash
   pip install pre-commit
   ```
   Или через Homebrew (macOS):
   ```bash
   brew install pre-commit
   ```

2. **Установите hooks**:
   ```bash
   pre-commit install
   ```

3. **Готово!** Теперь hooks будут запускаться автоматически при каждом `git commit`

### Как использовать вручную?

- **Проверить все файлы**:
  ```bash
  pre-commit run --all-files
  ```

- **Проверить только staged файлы**:
  ```bash
  pre-commit run
  ```

- **Пропустить hooks** (не рекомендуется):
  ```bash
  git commit --no-verify
  ```

### Что делать, если hook провалился?

1. Pre-commit автоматически исправит то, что может (например, trailing spaces)
2. Если есть ошибки, которые нужно исправить вручную - исправьте их
3. Добавьте исправленные файлы: `git add .`
4. Попробуйте закоммитить снова: `git commit`

### Документация:
- [Pre-commit документация](https://pre-commit.com/)

---

## 🎯 Резюме

| Файл | Назначение | Когда используется |
|------|-----------|-------------------|
| `qodana.yaml` | Статический анализ кода | В CI/CD и локально |
| `.github/workflows/ci.yml` | Автоматические проверки | При каждом push/PR |
| `docker-compose.dev-db.yml` | Локально только Postgres | При запуске БД для разработки |
| `.pre-commit-config.yaml` | Проверки перед коммитом | При каждом `git commit` |

---

## 📚 Полезные ссылки

- [Qodana документация](https://www.jetbrains.com/help/qodana/)
- [GitHub Actions документация](https://docs.github.com/en/actions)
- [Docker Compose документация](https://docs.docker.com/compose/)
- [Pre-commit документация](https://pre-commit.com/)

---

**Создано:** 2025-01-27  
**Версия:** 1.0
