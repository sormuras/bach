package de.sormuras.bach;

import java.net.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;

/** Shared heavyweight singletons. */
public /*static*/ class Atomics {

  private final AtomicReference<HttpClient> atomicHttpClient = new AtomicReference<>();

  /** Return shared {@link HttpClient} instance. */
  public HttpClient getHttpClient() {
    var atomic = atomicHttpClient.get();
    if (atomic != null) return atomic;
    var logger = System.getLogger(Atomics.class.getName());
    logger.log(System.Logger.Level.DEBUG, "Creating new HttpClient instance...");
    var object = newHttpClient();
    logger.log(System.Logger.Level.DEBUG, "Created new HttpClient instance");
    return atomicHttpClient.compareAndSet(null, object) ? object : atomicHttpClient.get();
  }

  protected HttpClient newHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }
}
