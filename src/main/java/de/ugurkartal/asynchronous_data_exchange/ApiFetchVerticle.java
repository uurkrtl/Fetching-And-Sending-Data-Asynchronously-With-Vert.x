package de.ugurkartal.asynchronous_data_exchange;

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
