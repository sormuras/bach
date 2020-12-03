package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Functions;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.project.ModuleDirectory;
import com.github.sormuras.bach.project.ModuleLookup;
import com.github.sormuras.bach.project.ProjectInfo;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Supplier;

/** Java Shell Builder. */
public final class Bach {

  /** Path to directory with external modules. */
  public static final Path LIBRARIES = Path.of(ProjectInfo.EXTERNAL_MODULES);

  /** Path to directory that collects all generated assets. */
  public static final Path WORKSPACE = Path.of(ProjectInfo.WORKSPACE);

  /**
   * Returns a shell builder initialized with default components.
   *
   * @return a new instance of {@code Bach} initialized with default components
   */
  public static Bach ofSystem() {
    return new Bach(Logbook.ofSystem(), Functions.memoize(Bach::newHttpClient));
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
    return module.getDescriptor().version().map(Object::toString).orElse("16-ea");
  }

  /**
   * Returns a new HttpClient following {@linkplain HttpClient.Redirect#NORMAL normal} redirects.
   *
   * @return a new http client instance
   */
  public static HttpClient newHttpClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  /** A logbook. */
  private final Logbook logbook;

  /** A supplier of a http client. */
  private final Supplier<HttpClient> httpClientSupplier;

  /**
   * Initialize default constructor.
   *
   * @param logbook the logbook to record messages
   * @param httpClientSupplier the supplier of an http client
   */
  public Bach(Logbook logbook, Supplier<HttpClient> httpClientSupplier) {
    this.logbook = logbook;
    this.httpClientSupplier = httpClientSupplier;
  }

  /**
   * Returns the http client for downloading files.
   *
   * @return the http client for downloading files
   */
  public HttpClient httpClient() {
    return httpClientSupplier.get();
  }

  /**
   * Returns the logbook.
   *
   * @return the logbook
   */
  public Logbook logbook() {
    return logbook;
  }

  /**
   * Copy all content and attributes from a uri to a target file.
   *
   * @param uri the request URI of the remote source file
   * @param file the path to the local target file
   * @return the local target file
   */
  public Path httpCopy(URI uri, Path file) {
    return httpCopy(uri, file, StandardCopyOption.COPY_ATTRIBUTES);
  }

  /**
   * Copy all content from a uri to a target file.
   *
   * @param uri the request URI of the remote source file
   * @param file the path to the local target file
   * @param options options specifying how the copy should be done
   * @return the local target file
   */
  public Path httpCopy(URI uri, Path file, CopyOption... options) {
    var request = HttpRequest.newBuilder(uri).GET();
    if (Files.exists(file) && Paths.isViewSupported(file, "user"))
      try {
        var etagBytes = (byte[]) Files.getAttribute(file, "user:etag");
        var etag = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(etagBytes)).toString();
        request.setHeader("If-None-Match", etag);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    var directory = file.getParent();
    if (directory != null) Paths.createDirectories(directory);
    var handler = HttpResponse.BodyHandlers.ofFile(file);
    var response = httpSend(request.build(), handler);
    if (response.statusCode() == 200 /* Ok */) {
      logbook.accept(file + " << " + uri);
      if (Set.of(options).contains(StandardCopyOption.COPY_ATTRIBUTES))
        try {
          var etagHeader = response.headers().firstValue("etag");
          if (etagHeader.isPresent() && Paths.isViewSupported(file, "user")) {
            var etag = StandardCharsets.UTF_8.encode(etagHeader.get());
            Files.setAttribute(file, "user:etag", etag);
          }
          var lastModifiedHeader = response.headers().firstValue("last-modified");
          if (lastModifiedHeader.isPresent()) {
            var text = lastModifiedHeader.get(); // force " GMT" suffix
            if (!text.endsWith(" GMT")) text = text.substring(0, text.lastIndexOf(' ')) + " GMT";
            var time = ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME);
            Files.setLastModifiedTime(file, FileTime.from(Instant.from(time)));
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      return file;
    }
    if (response.statusCode() == 304 /*Not Modified*/) return file;
    Paths.deleteIfExists(file);
    throw new IllegalStateException("Copy " + uri + " failed: response=" + response);
  }

  /**
   * Read all content from a uri into a string.
   *
   * @param uri the uri to read
   * @return a UTF-8 encoded string of the requested URI
   */
  public String httpRead(URI uri) {
    var request = HttpRequest.newBuilder(uri).GET();
    return httpSend(request.build(), HttpResponse.BodyHandlers.ofString()).body();
  }

  <T> HttpResponse<T> httpSend(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
    try {
      return httpClient().send(request, handler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load a module.
   *
   * @param directory the module finder to query for already loaded modules
   * @param searcher the function that maps a module name to its uri
   * @param module the name of the module to load
   */
  public void loadModule(ModuleDirectory directory, ModuleLookup searcher, String module) {
    if (directory.finder().find(module).isPresent()) return;
    httpCopy(directory.lookup(module, searcher), directory.jar(module));
  }

  /**
   * Load all missing modules of the given module directory.
   *
   * @param directory the module finder to query for already loaded modules
   * @param searcher the searcher to query for linked modules
   */
  public void loadMissingModules(ModuleDirectory directory, ModuleLookup searcher) {
    while (true) {
      var missing = directory.missing();
      if (missing.isEmpty()) return;
      for (var module : missing)
        httpCopy(directory.lookup(module, searcher), directory.jar(module));
    }
  }
}
