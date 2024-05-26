package de.ugurkartal.asynchronous_data_exchange;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(EventBusVerticle.class);
  public static final String ADDRESS = "fetch.data";

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.setPeriodic(5000, id -> {
      vertx.eventBus().request(ADDRESS, "", reply -> {
        if (reply.succeeded()) {
          JsonObject data = (JsonObject) reply.result().body();
          LOGGER.info("Received data: {}", data);
        } else {
          LOGGER.error("Failed to fetch data: {}", reply.cause().getMessage());
        }
      });
    });

    startPromise.complete();
  }
}
