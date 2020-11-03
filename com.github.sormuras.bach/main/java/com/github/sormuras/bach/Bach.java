package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Functions;
import com.github.sormuras.bach.internal.Paths;
import java.io.PrintStream;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.function.Supplier;

/** Java Shell Builder. */
public final class Bach implements Http, Load, Print, Tool {

  /**
   * Returns a shell builder initialized with default components.
   *
   * @return a new instance of {@code Bach} initialized with default components
   */
  public static Bach ofSystem() {
    return new Bach(System.out, Functions.memoize(Http::newClient));
  }

  /**
   * Returns the version of Bach.
   *
   * @return the version as a string
   * @throws IllegalStateException if not running on the module path
   */
  public static String version() {
    var module = Bach.class.getModule();
    if (!module.isNamed()) throw new IllegalStateException("Bach's module is unnamed?!");
    return module
        .getDescriptor()
        .version()
        .map(Object::toString)
        .orElseGet(() -> Paths.readString(Path.of("VERSION")).orElseThrow());
  }

  /** A print stream for normal messages. */
  private final PrintStream printer;

  /** A supplier of a http client. */
  private final Supplier<HttpClient> httpClientSupplier;

  /**
   * Initialize default constructor.
   *
   * @param printer the print stream for normal messages
   * @param httpClientSupplier the supplier of an http client
   */
  public Bach(PrintStream printer, Supplier<HttpClient> httpClientSupplier) {
    this.printer = printer;
    this.httpClientSupplier = httpClientSupplier;
  }

  @Override
  public HttpClient httpClient() {
    return httpClientSupplier.get();
  }

  @Override
  public PrintStream printStream() {
    return printer;
  }
}
