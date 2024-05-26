package de.ugurkartal.asynchronous_data_exchange;

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
