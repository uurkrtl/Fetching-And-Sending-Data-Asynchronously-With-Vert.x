package de.ugurkartal.asynchronous_data_exchange;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiFetchVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(ApiFetchVerticle.class);
  public static final String API_DATA = "api.data";
  public static final String API_URL = "jsonplaceholder.typicode.com";
  public static final String QUERY_PARAMETER = "/posts/1";
  private HttpClient client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    client = vertx.createHttpClient();
    vertx.setPeriodic(5000, id -> fetchDataAndSend());
    startPromise.complete();
  }

  private void fetchDataAndSend() {
    client.request(HttpMethod.GET, API_URL, QUERY_PARAMETER)
      .compose(req -> req.send().compose(resp -> resp.body()))
      .onSuccess(buffer -> {
        JsonObject json = buffer.toJsonObject();
        vertx.eventBus().send(API_DATA, json);
      })
      .onFailure(Throwable::printStackTrace);
  }
}
