package de.ugurkartal.asynchronous_data_exchange;

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
