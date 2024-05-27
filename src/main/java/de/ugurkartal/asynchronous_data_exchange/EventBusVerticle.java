package de.ugurkartal.asynchronous_data_exchange;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(EventBusVerticle.class);
  public static final String PROCESSED_DATA = "processed.data";

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.eventBus().consumer(MongoVerticle.MONGO_DATA, this::handleMessage);
    startPromise.complete();
  }

  private void handleMessage(Message<Object> message) {
    JsonObject data = (JsonObject) message.body();
    data.put("processed", true);
    LOGGER.info("Proccessing data: {}", data.encodePrettily());
    vertx.eventBus().publish(PROCESSED_DATA,data);
  }
}
