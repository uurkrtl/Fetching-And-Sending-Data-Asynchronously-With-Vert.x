package de.ugurkartal.asynchronous_data_exchange;

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
