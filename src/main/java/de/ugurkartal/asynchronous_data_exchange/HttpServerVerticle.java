package de.ugurkartal.asynchronous_data_exchange;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  public static final int PORT = 8080;
  private JsonObject lastReceivedData = new JsonObject();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);

    // HTTP endpoint to get the latest data
    router.get("/data").handler(ctx -> ctx.response().end(lastReceivedData.encodePrettily()));

    // Set up the HTTP server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(PORT, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          LOGGER.info("HTTP server started on port {}", PORT);
        } else {
          startPromise.fail(http.cause());
        }
      });

    // Listen to the EventBus for data
    vertx.eventBus().<JsonObject>consumer(EventBusVerticle.PROCESSED_DATA, message -> {
      lastReceivedData = message.body();
      LOGGER.info("Received data on EventBus: {}", lastReceivedData.encodePrettily());
    });
  }
}
