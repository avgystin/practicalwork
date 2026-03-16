

## Для работы приложения требуются следующие сервисы:

- **PostgreSQL** — база данных
- **ZooKeeper** — координация Kafka
- **Kafka** — брокер сообщений

## Сборка и запуск
```
.\mvnw clean package -X
java -jar target/Bell-*.jar
```

## 🚀 Основные возможности

*   **Управление сессиями:** Создание, валидация и удаление пользовательских сессий с автоматической очисткой устаревших.
*   **Обработка заказов:** CRUD-операции для заказов, привязанных к сессиям и товарам.
*   **Интеграция с Kafka:**
    *   Автоматическое сохранение полученных сообщений из Kafka в базу данных.
    *   Удаление заказов через отдельный топик `practicalwork`.
*   **Динамические задержки ответов:** Сервис `DelayService` позволяет программно добавлять задержки к ответам различных эндпоинтов, конфигурируя их через `application.yml` или на лету через специальный эндпоинт. Это полезно для симуляции нагрузки и тестирования.
*   **Наблюдаемость (Observability):**
    *   **Метрики:** Интеграция с Micrometer для сбора метрик (HTTP запросы, Kafka, JVM, пул соединений и др.). Метрики экспортируются в формате Prometheus.
    *   **Трассировка:** Интеграция с Micrometer Tracing и Zipkin для распределенной трассировки запросов.
    *   **Мониторинг:** Встроенная поддержка эндпоинтов Spring Boot Actuator (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`).
*   **Использование Virtual Threads:** Проект настроен на использование виртуальных потоков (Project Loom) для улучшенной производительности при обработке большого количества конкурентных запросов.


## 📚 API Эндпоинты

### Основные операции

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/session/create` | Создание сессии |
| `GET` | `/order/getProducts` | Список товаров |
| `POST` | `/order/create` | Создание заказа |
| `GET` | `/order/getOrder` | Получение заказа |
| `DELETE` | `/session/delete` | Удаление сессии |
| `GET` | `/order/Check` | Проверка сессии |

### Примеры запросов

**1. Создание сессии**
```
curl -X GET http://localhost:12222/session/create
```
Ответ:
```
{"session_id": "123e4567-e89b-12d3-a456-426614174000"}
```
**2. Получение списка товаров**
```
curl -X GET http://localhost:12222/order/getProducts -H "Session-ID: 123e4567-e89b-12d3-a456-426614174000"
```
Ответ:
```
{"MacBook":1,"iPhone":89,"Samsung Galaxy":45,"Sony WH":24,"Apple Watch":41}
```
**3. Создание заказа**
```
curl -X POST http://localhost:12222/order/create -H "Session-ID: 123e4567-e89b-12d3-a456-426614174000" -H "Content-Type: application/json" -d "{\"product_name\":\"MacBook\",\"quantity\":2}"
```
Ответ:
```
{"order_id":1}
```
**4. Получение заказа**
```
curl -X GET "http://localhost:12222/order/getOrder?order_id=1&product_name=MacBook" -H "Session-ID: 123e4567-e89b-12d3-a456-426614174000"
```
Ответ:
```
{"order_id":1,"product_name":"MacBook","quantity":2,"total_price":2}
```
**5. Удаление сессии**
```
curl -X DELETE http://localhost:12222/session/delete -H "Session-ID: 123e4567-e89b-12d3-a456-426614174000"
```
Ответ:
```
Session deleted successfully
```
**6. Проверка сессии**
```
curl -X GET "http://localhost:12222/order/Check?session_id=123e4567-e89b-12d3-a456-426614174000"

```
Ответ:
```
{"session_id":"123e4567-e89b-12d3-a456-426614174000","is_valid":true,"message":"Session is active"}

```

### Управление задержками

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `GET` | `/delay` | Получить текущую конфигурацию задержек |
| `POST` | `/delay` | Обновить конфигурацию задержек |
| `POST` | `/delay/reset` | Сбросить к значениям из application.yml |

### Пример тела запроса для `/delay` (POST)

```json
{
  "session": {
    "create": 1000,
    "delete": 500,
    "check": 100
  },
  "order": {
    "create": 2000,
    "getProducts": 1000,
    "getOrder": 1000
  }
}
```

## 📊 Мониторинг и Наблюдаемость

### Spring Boot Actuator

| Эндпоинт | Описание |
|----------|----------|
| `http://localhost:12222/actuator/metrics` | Все метрики приложения |
| `http://localhost:12222/actuator/health` | Проверка здоровья |
| `http://localhost:12222/actuator/prometheus` | Метрики в формате Prometheus |
| `http://localhost:12222/actuator/metrics/users.concurrent` | Количество активных сессий |



