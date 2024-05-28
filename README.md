# Fetching data asynchronously from MongoDB and JSON Placeholder API and sending it via the EventBus with Vert.x

![asynchronous-data-exchange](https://github.com/uurkrtl/Fetching-And-Sending-Data-Asynchronously-With-Vert.x/assets/52300746/415d4394-c999-459c-9101-3330d10d90a0)

This repository demonstrates a clustered Vert.x application with asynchronous data exchange using EventBus, HTTP server setup, JSON Placeholder API and MongoDB integration.

To create an example Vert.x application that fetches data asynchronously from MongoDB, JSON Placeholder API and sends it via the EventBus, follow these steps:

## 1. Create a Vert.x Project
```css
vertx-mongodb-eventbus
├── pom.xml
└── src
    └── main
        └── java
            └── com
                └── example
                    └── MainVerticle.java
                    └── MongoVerticle.java
                    └── ApiFetchVerticle.java
                    └── EventBusVerticle.java
                    └── HttpServerVerticle.java

```

## 2. Add Dependencies to pom.xml
Include the necessary Vert.x, MongoDB, and Hazelcast dependencies in your pom.xml:
```xml
<dependencies>
    <!-- Vert.x Core -->
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-core</artifactId>
    </dependency>
    <!-- Vert.x MongoDB Client -->
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-mongo-client</artifactId>
    </dependency>
    <!-- Vert.x Cluster Manager for Hazelcast -->
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-hazelcast</artifactId>
    </dependency>
    <!-- Vert.x Web -->
    <dependency>
      <groupId>io.vertx</groupId>
      <artifactId>vertx-web</artifactId>
    </dependency>
    <!-- slf4j Logger -->
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.4.14</version>
    </dependency>
</dependencies>
```

## 3. Implement the Verticles

### MainVerticle
The MainVerticle deploys MongoVerticle, HttpServerVerticle, and EventBusVerticle.
```java
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  public static void main(String[] args) {
    HazelcastClusterManager clusterManager = new HazelcastClusterManager();
    VertxOptions options = new VertxOptions().setClusterManager(clusterManager);

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
        vertx.deployVerticle(new MongoVerticle());
        vertx.deployVerticle(new ApiFetchVerticle());
        vertx.deployVerticle(new EventBusVerticle());
        vertx.deployVerticle(new HttpServerVerticle());
        LOGGER.info("Deployed {}", MainVerticle.class.getName());
      } else {
        LOGGER.error("Failed to start clustered Vert.x instance");
        res.cause().printStackTrace();
      }
    });
  }

  @Override
  public void start() {
    LOGGER.info("MainVerticle started");
  }
}
```
### MongoVerticle
Fetches data from MongoDB and sends it via the EventBus.
```java
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MongoVerticle extends AbstractVerticle {
  public static final String MONGO_DATA = "mongo.data";
  private MongoClient mongoClient;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    JsonObject config = new JsonObject()
      .put("connection_string", System.getenv("MONGODB_URI"))
      .put("db_name", System.getenv("MONGODB_NAME"));

    mongoClient = MongoClient.createShared(vertx, config);

    // Fetch data from MongoDB and send it via EventBus
    vertx.setPeriodic(5000, id -> fetchDataAndSend());

    startPromise.complete();
  }

  private void fetchDataAndSend() {
    mongoClient.find("customer", new JsonObject())
      .onSuccess(res -> res.forEach(doc -> vertx.eventBus().send(MONGO_DATA, doc)))
      .onFailure(Throwable::printStackTrace);
  }
}
```

### ApiFetchVerticle
Fetches data from API and sends it via the EventBus.
```java
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

public class ApiFetchVerticle extends AbstractVerticle {
  public static final String API_DATA = "api.data";
  private HttpClient client;
  String apiUrl;
  String pathVariable;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    client = vertx.createHttpClient();
    apiUrl = config().getString("apiUrl", "jsonplaceholder.typicode.com");
    pathVariable = config().getString("pathVariable", "/posts/1");
    vertx.setPeriodic(5000, id -> fetchDataAndSend());
    startPromise.complete();
  }

  private void fetchDataAndSend() {
    client.request(HttpMethod.GET, apiUrl, pathVariable)
      .compose(req -> req.send().compose(HttpClientResponse::body))
      .onSuccess(buffer -> vertx.eventBus().send(API_DATA, buffer.toJsonObject()))
      .onFailure(Throwable::printStackTrace);
  }
}
```

### EventBusVerticle
Listens for data sent from MongoVerticle to EventBus, processes it, and republishes it.
```java
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(EventBusVerticle.class);
  public static final String PROCESSED_DB_DATA = "processed.db.data";
  public static final String PROCESSED_API_DATA = "processed.api.data";
  private JsonObject data = new JsonObject();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.eventBus().consumer(MongoVerticle.MONGO_DATA, this::handleDbMessage);
    vertx.eventBus().consumer(ApiFetchVerticle.API_DATA, this::handleApiMessage);
    startPromise.complete();
  }

  private void handleApiMessage(Message<Object> message) {
    data = (JsonObject) message.body();
    data.put("processed", true);
    LOGGER.info("Proccessing Api data: {}", data);
    vertx.eventBus().publish(PROCESSED_API_DATA, data);
  }

  private void handleDbMessage(Message<Object> message) {
    data = (JsonObject) message.body();
    data.put("processed", true);
    LOGGER.info("Proccessing DB data: {}", data.encodePrettily());
    vertx.eventBus().publish(PROCESSED_DB_DATA, data);
  }
}
```

### HttpServerVerticle
Sets up an HTTP server that responds with data received from the EventBus.
```java
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  private JsonObject dataFromDb = new JsonObject();
  private JsonObject dataFromApi = new JsonObject();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    int port = 8080;
    Router router = Router.router(vertx);

    // HTTP endpoint to get the latest data
    router.get("/from-db").handler(this::getDataFromDb);
    router.get("/from-api").handler(this::getDataFromApi);

    // Set up the HTTP server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)
      .onSuccess(httpServer -> {
        LOGGER.info("HTTP server started on port {}", port);
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }

  // Listen to the EventBus for data
  private void getDataFromApi(RoutingContext context) {
    vertx.eventBus().<JsonObject>consumer(EventBusVerticle.PROCESSED_API_DATA, message ->
      dataFromApi = message.body());
    context.response()
      .putHeader("Content-Type", "application/json")
      .setStatusCode(200)
      .end(dataFromApi.encodePrettily());
    LOGGER.info("Received data from API on EventBus: {}", dataFromDb.encodePrettily());
  }

  private void getDataFromDb(RoutingContext context) {
    vertx.eventBus().<JsonObject>consumer(EventBusVerticle.PROCESSED_DB_DATA, message ->
      dataFromDb = message.body());
    context.response()
      .putHeader("Content-Type", "application/json")
      .setStatusCode(200)
      .end(dataFromDb.encodePrettily());
    LOGGER.info("Received data from DB on EventBus: {}", dataFromDb.encodePrettily());
  }
}
```

## Running the Application
To see clustering in action, follow these steps:

1- Start multiple instances of your application. You can do this by running the MainVerticle multiple times or deploying it in different JVMs.

2- Each instance will join the cluster and communicate via the clustered EventBus.
