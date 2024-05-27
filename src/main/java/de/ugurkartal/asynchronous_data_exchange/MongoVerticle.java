package de.ugurkartal.asynchronous_data_exchange;

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
    mongoClient.find("customer", new JsonObject(), res -> {
      if (res.succeeded()) {
        res.result().forEach(doc -> vertx.eventBus().send(MONGO_DATA, doc));
      } else {
        res.cause().printStackTrace();
      }
    });
  }
}
