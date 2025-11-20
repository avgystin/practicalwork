# Chapter 5: Kafka Messaging

Welcome back to the `practicalwork` project tutorial! In the [previous chapter](04_configurable_delays_.md), we learned about **Configurable Delays** and how we can use them to simulate real-world waiting times in our application. Now, let's explore a new and powerful way for different parts of our system to communicate with each other efficiently and reliably: **Kafka Messaging**.

### Why Do We Need Kafka Messaging?

Imagine our `practicalwork` application is like a busy headquarters. When important events happen – for instance, a new order is created, or a user logs in – multiple departments (other parts of our application or even other separate applications) might need to know about it.

The traditional way is for one department to directly call another: "Hey Order Department, tell the Shipping Department *right now* that there's a new order!" This works, but what if the Shipping Department is busy, or its phone line is down? The Order Department would have to wait, or the whole process might fail.

The problem **Kafka Messaging** solves is providing a **reliable and decoupled way for different parts of an application to communicate**. Instead of direct calls, Kafka acts like a high-speed, organized **postal service for messages**.

When an event occurs (like a new message being posted), our application simply writes a message to a specific "mailbox" (called a **Kafka topic**). Other parts of the application (or other apps) can then subscribe to these mailboxes. They will constantly check for new messages and process them whenever they are ready, without interrupting the sender. This ensures that information flows efficiently, even if some parts of the system are temporarily unavailable or busy.

**Central Use Case:** In `practicalwork`, when a client sends a message to our `/post-message` endpoint, we don't want to hold up the client waiting for that message to be processed and stored. Instead, we want to quickly confirm receipt to the client, and then let Kafka handle the reliable delivery and asynchronous storage of that message into a database.

### Key Concepts of Kafka Messaging

Let's break down the essential ideas behind Kafka:

1.  **Kafka (The System)**: This is the central messaging platform itself. It's designed to handle a huge number of messages very quickly and reliably.
2.  **Producers**: These are like the "senders" or "writers" of messages. In our application, when an event happens, a `KafkaProducer` creates a message and sends it to Kafka.
3.  **Consumers**: These are the "receivers" or "readers" of messages. A `KafkaConsumer` is constantly listening to Kafka, waiting for new messages that interest it.
4.  **Topics**: These are like specific "mailboxes" or "categories" within Kafka. Producers send messages to a particular topic, and consumers subscribe to topics to receive messages from them. For example, we might have a `postedmessages` topic for general system events, and a `neworders` topic for all new order events.
5.  **Asynchronous Processing**: This is the core benefit. The producer sends a message and doesn't wait for it to be processed by a consumer. It just "drops the letter in the mailbox" and moves on. The consumer will pick up and process the message *later*, at its own pace. This makes our application faster and more resilient.

### How to Use: Sending a Message to Kafka (The Producer Side)

Let's look at how our `practicalwork` application sends messages to Kafka. We have a specific [REST API Endpoint](01_rest_api_endpoints_.md) for this: `/post-message`.

When a client sends a `POST` request to this endpoint, our application's job is simply to take the incoming message, add some metadata (like a timestamp and the request path), and then publish it to Kafka using our `KafkaProducer`.

**Endpoint:** `POST /post-message`

**What it does:** Receives a message from the client, adds some details, and sends it to a Kafka topic. It then immediately responds to the client, without waiting for the message to be stored in a database.

**Example Code (from `src/main/java/BellSpring/controller/SpringController.java`):**

```java
// File: src/main/java/BellSpring/controller/SpringController.java

// ... inside SpringController class ...

    private final KafkaProducer kafkaProducer; // Our Kafka sender

    @PostMapping("/post-message")
    public ResponseEntity<String> calculateSquare(@RequestBody Map<String, String> request,
                                                  HttpServletRequest httpRequest) {
        // Extract basic info from the request
        String msg_id = request.get("msg_id");
        long unixtimestampMs = System.currentTimeMillis();
        String unixtimestamp = String.valueOf(unixtimestampMs / 1000);
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();

        // Send this information to Kafka using our KafkaProducer
        return kafkaProducer.sendToKafka(msg_id, unixtimestamp, method, path);
    }
```
*   `private final KafkaProducer kafkaProducer;`: Our `SpringController` relies on a specialized helper, `KafkaProducer`, to handle the actual sending of messages to Kafka.
*   `@PostMapping("/post-message")`: This annotation defines our endpoint for receiving messages.
*   `kafkaProducer.sendToKafka(...)`: This is the key line! Instead of directly saving to a database here, the controller delegates the task to the `kafkaProducer`, which sends the message to Kafka. The `ResponseEntity.ok("OK")` is sent back to the client almost immediately.

**Example Client Request:**

```http
POST /post-message HTTP/1.1
Host: your-app-domain.com
Content-Type: application/json

{
    "msg_id": "unique-message-123",
    "content": "Hello Kafka!"
}
```

**Example Application Response:**

```http
HTTP/1.1 200 OK

OK
```
The client quickly gets an "OK" response, even though the actual saving of the message to the database will happen later, handled by the Kafka consumer.

### Internal Implementation: The KafkaProducer (Sending)

Let's look at how the `KafkaProducer` actually sends messages to Kafka.

#### High-Level Flow for Sending a Message

1.  **Client Request**: A client sends a `POST` request to `/post-message`.
2.  **`SpringController` Prepares Data**: The `SpringController` extracts relevant information (`msg_id`, timestamp, HTTP method, URI) from the request.
3.  **`SpringController` Delegates**: The controller calls `kafkaProducer.sendToKafka()` with this prepared data.
4.  **`KafkaProducer` Formats Message**: The `KafkaProducer` takes the individual pieces of data and combines them into a single message (in our case, a `Map` that Spring Kafka will convert to JSON).
5.  **`KafkaProducer` Sends to Kafka**: The `KafkaProducer` uses a special tool called `KafkaTemplate` to send this message to a designated **Kafka topic** (e.g., `postedmessages`).
6.  **Quick Response**: The `kafkaProducer` quickly returns "OK" to the `SpringController`, which then sends it back to the client.

Here's a simple diagram illustrating this flow:

```mermaid
sequenceDiagram
    participant Client
    participant SpringController
    participant KafkaProducer
    participant KafkaTopic["Kafka (Topic: postedmessages)"]

    Client->>SpringController: POST /post-message (message data)
    Note over SpringController: Prepares message payload
    SpringController->>KafkaProducer: sendToKafka(msg_id, timestamp, method, path)
    KafkaProducer->>KafkaTopic: Sends message to "postedmessages"
    KafkaTopic-->>KafkaProducer: (Acknowledges receipt)
    KafkaProducer-->>SpringController: Returns "OK"
    SpringController-->>Client: HTTP 200 OK
```

#### Diving into `KafkaProducer.java`

Here's the simplified code for our `KafkaProducer`:

```java
// File: src/main/java/BellSpring/service/KafkaProducer.java

// ... imports and class definition ...

@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate; // Tool to send messages

    public ResponseEntity<String> sendToKafka(String msg_id, String unixtimestamp,
                                 String method, String path) {

        // Create a map to hold all the message details
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("msg_id", msg_id);
        hashMap.put("timestamp", unixtimestamp);
        hashMap.put("method", method);
        hashMap.put("uri", path);

        try {
            // Send the message (the HashMap) to the "postedmessages" topic
            kafkaTemplate.send("postedmessages", hashMap);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            // If there's an error sending to Kafka, return an error response
            return ResponseEntity.status(500).body("Error");
        }
    }
}
```
*   `@Service`: Marks this class as a Spring service.
*   `private final KafkaTemplate<String, Object> kafkaTemplate;`: This is a special Spring Kafka component that handles all the low-level details of connecting to Kafka and sending messages. We simply use its `send()` method.
*   `HashMap<String, String> hashMap`: We gather all the information into a `Map`. Spring Kafka knows how to convert this `Map` into a JSON string when sending it to the topic, making it easy for consumers to read.
*   `kafkaTemplate.send("postedmessages", hashMap);`: This line sends our `hashMap` message to the Kafka topic named `postedmessages`.

### Internal Implementation: The KafkaConsumer (Receiving)

On the other side, we have a `KafkaConsumer` that is constantly listening for messages from the `postedmessages` topic. When it receives a message, it processes it – in our case, by saving it into a database.

#### High-Level Flow for Receiving a Message

1.  **`KafkaConsumer` Listens**: The `KafkaConsumer` is continuously connected to Kafka and waiting for new messages on the topics it's interested in (e.g., `postedmessages`).
2.  **Message Arrives**: A `KafkaProducer` sends a message to the `postedmessages` topic.
3.  **`KafkaConsumer` Receives**: The `KafkaConsumer` automatically detects and receives this new message.
4.  **`KafkaConsumer` Processes**: Our consumer's `listen` method is triggered. It takes the message content and creates a `MessageEntity` object.
5.  **`KafkaConsumer` Saves to DB**: The consumer then uses a `MessageRepository` to save this `MessageEntity` into our PostgreSQL database.

Here's a simple diagram illustrating this asynchronous flow:

```mermaid
sequenceDiagram
    participant KafkaTopic["Kafka (Topic: postedmessages)"]
    participant KafkaConsumer
    participant MessageRepository
    participant Database["PostgreSQL Database"]

    KafkaTopic->>KafkaConsumer: New message available!
    Note over KafkaConsumer: Automatically picks up message
    KafkaConsumer->>KafkaConsumer: Processes message content
    KafkaConsumer->>MessageRepository: save(newMessageEntity)
    MessageRepository->>Database: Saves message
    Database-->>MessageRepository: (Confirmation of save)
    MessageRepository-->>KafkaConsumer: (Operation complete)
```

#### Diving into `KafkaConsumer.java`

Here's the simplified code for our `KafkaConsumer`:

```java
// File: src/main/java/BellSpring/service/KafkaConsumer.java

// ... imports and class definition ...

@Component
@RequiredArgsConstructor
@Slf4j // For logging messages
public class KafkaConsumer {

    private final MessageRepository messageRepository; // To save messages to the database

    @KafkaListener(topics = "postedmessages", groupId = "my-group")
    public void listen(String message) {
        System.out.println("Received message: " + message);

        // Convert the received string message into an entity to save
        MessageEntity entity = new MessageEntity();
        entity.setContent(message); // Assuming the message content is directly the string

        // Save the message into our PostgreSQL database
        messageRepository.save(entity);

        log.info("Message saved to database with id: {}", entity.getId());
    }

    // Another consumer example for a different topic, "practicalwork"
    // This one deletes an order from the database
    @KafkaListener(topics = "practicalwork", groupId = "del-messege")
    public void delmessege(String message) {
        // Here, 'message' is expected to be an order ID
        Long id = Long.parseLong(message);
        // orderRepository.deleteById(id); // (OrderRepository would be injected here)
        log.info("Delete message: {}", message);
    }
}
```
*   `@Component`: Marks this class as a Spring component.
*   `private final MessageRepository messageRepository;`: Our consumer uses a `MessageRepository` (which connects to our database) to store the received messages. (We'll learn more about repositories in [Persistence Layer (JPA/Hibernate)](06_persistence_layer__jpa_hibernate__.md)).
*   `@KafkaListener(topics = "postedmessages", groupId = "my-group")`: This is the magical annotation! It tells Spring Kafka:
    *   "Listen to messages from the Kafka topic named `postedmessages`."
    *   "Belong to the `my-group` consumer group." (A group allows multiple consumers to share the workload.)
*   `public void listen(String message)`: This method is automatically called by Spring Kafka every time a new message arrives in the `postedmessages` topic. The `message` parameter will contain the actual message content (the JSON string that our producer sent).
*   `MessageEntity entity = new MessageEntity(); entity.setContent(message);`: We create a simple data object to hold the message content.
*   `messageRepository.save(entity);`: This line saves the `MessageEntity` into our PostgreSQL database, ensuring the message is durably stored.
*   The `delmessege` method shows that you can have multiple `@KafkaListener` methods in the same class, each listening to different topics or having different processing logic.

### Kafka Configuration in `application.yml`

To make all of this work, our application needs to know where the Kafka server is and how to connect to it. These settings are typically found in `src/main/resources/application.yml`:

```yaml
# File: src/main/resources/application.yml

# ... other settings ...

spring:
  # ... application name ...
  kafka:
    bootstrap-servers: localhost:9092 # Address of our Kafka server
    producer:
      acks: all                       # Producer waits for all replicas to confirm message receipt
      retries: 3                      # Retries sending message if it fails
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer # Converts objects to JSON
      properties:
        enable.idempotence: true      # Ensures messages are only written once, preventing duplicates
    consumer:
      group-id: my-group              # ID of the consumer group
      auto-offset-reset: earliest     # Start reading messages from the beginning of the topic if no offset is stored
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer # Converts message content to String

  # ... datasource and JPA settings ...
```
*   `bootstrap-servers`: This is the most important setting; it tells our application where to find the Kafka cluster (in this case, on our local machine at `localhost:9092`).
*   `producer`: These settings configure how our `KafkaProducer` behaves (e.g., how reliably it sends messages, how it converts Java objects into bytes for Kafka). `JsonSerializer` is key here, allowing us to send `Map` objects that become JSON.
*   `consumer`: These settings configure how our `KafkaConsumer` behaves (e.g., its `group-id`, how it restarts reading messages, how it converts bytes from Kafka back into Java objects). `StringDeserializer` means our consumer expects the Kafka message content to be a simple string.

### Conclusion

In this chapter, we've introduced **Kafka Messaging** as a powerful postal service for internal messages within our `practicalwork` application. We've learned about the roles of `KafkaProducer` (sending messages to topics), `KafkaConsumer` (listening and processing messages asynchronously), and **Topics** (the mailboxes). We saw how this decoupling allows our application to quickly acknowledge requests while reliably processing tasks like saving messages to a database in the background.

This asynchronous processing is a cornerstone of modern, scalable applications, ensuring that one slow operation doesn't hold up the entire system. Our next step is to understand how we durably store all this valuable information—the orders, the messages—in a long-term storage solution. We'll dive into the [Persistence Layer (JPA/Hibernate)](06_persistence_layer__jpa_hibernate__.md).

---

<sub><sup>Generated by [AI Codebase Knowledge Builder](https://github.com/The-Pocket/Tutorial-Codebase-Knowledge).</sup></sub> <sub><sup>**References**: [[1]](https://github.com/avgystin/practicalwork/blob/71096d4adfc15ec4fc4942c8c3cefe26364d3a19/src/main/java/BellSpring/controller/SpringController.java), [[2]](https://github.com/avgystin/practicalwork/blob/71096d4adfc15ec4fc4942c8c3cefe26364d3a19/src/main/java/BellSpring/model/MessageEntity.java), [[3]](https://github.com/avgystin/practicalwork/blob/71096d4adfc15ec4fc4942c8c3cefe26364d3a19/src/main/java/BellSpring/repository/MessageRepository.java), [[4]](https://github.com/avgystin/practicalwork/blob/71096d4adfc15ec4fc4942c8c3cefe26364d3a19/src/main/java/BellSpring/service/KafkaConsumer.java), [[5]](https://github.com/avgystin/practicalwork/blob/71096d4adfc15ec4fc4942c8c3cefe26364d3a19/src/main/java/BellSpring/service/KafkaProducer.java), [[6]](https://github.com/avgystin/practicalwork/blob/71096d4adfc15ec4fc4942c8c3cefe26364d3a19/src/main/resources/application.yml)</sup></sub>