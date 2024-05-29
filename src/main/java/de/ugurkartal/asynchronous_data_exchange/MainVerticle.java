package de.ugurkartal.asynchronous_data_exchange;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  public static void main(String[] args) {
    Vertx.clusteredVertx(new VertxOptions())
      .onSuccess(vertx -> {
        vertx.deployVerticle(new MongoVerticle());
        vertx.deployVerticle(new ApiFetchVerticle());
        vertx.deployVerticle(new EventBusVerticle());
        vertx.deployVerticle(new HttpServerVerticle());
        LOGGER.info("Deployed {}", MainVerticle.class.getName());
      })
      .onFailure(failure -> LOGGER.error("Failed to start clustered Vert.x instance: ", failure));
  }
}
