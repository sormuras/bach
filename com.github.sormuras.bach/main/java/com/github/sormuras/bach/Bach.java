package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Functions;
import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.module.ModuleSearcher;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolResponse;
import com.github.sormuras.bach.tool.ToolRunner;
import java.io.PrintStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;

/** Java Shell Builder. */
public final class Bach {

  /**
   * Returns a shell builder initialized with default components.
   *
   * @return a new instance of {@code Bach} initialized with default components
   */
  public static Bach ofSystem() {
    return new Bach(System.out, Functions.memoize(Bach::newHttpClient));
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

  /** A print stream for normal messages. */
  private final PrintStream printStream;

  /** A supplier of a http client. */
  private final Supplier<HttpClient> httpClientSupplier;

  /**
   * Initialize default constructor.
   *
   * @param printStream the print stream for normal messages
   * @param httpClientSupplier the supplier of an http client
   */
  public Bach(PrintStream printStream, Supplier<HttpClient> httpClientSupplier) {
    this.printStream = printStream;
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
   * Returns the print stream for printing messages.
   *
   * @return the print stream for printing messages
   */
  public PrintStream printStream() {
    return printStream;
  }

  /**
   * Print a listing of all files matching the given glob pattern.
   *
   * @param glob the glob pattern
   */
  public void printFind(String glob) {
    Paths.find(Path.of(""), glob, path -> printStream().println(Paths.slashed(path)));
  }

  /**
   * Print a sorted list of all modules locatable by the given module finder.
   *
   * @param finder the module finder to query for modules
   */
  public void printModules(ModuleFinder finder) {
    finder.findAll().stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::toNameAndVersion)
        .sorted()
        .forEach(printStream()::println);
  }

  /**
   * Print a description of the given module locatable by the given module finder.
   *
   * @param finder the module finder to query for modules
   * @param module the name of the module to describe
   */
  public void printModuleDescription(ModuleFinder finder, String module) {
    finder
        .find(module)
        .ifPresentOrElse(
            reference -> Modules.describeModule(printStream(), reference),
            () -> printStream().println("No such module found: " + module));
  }

  /**
   * Print a sorted list of all provided tools locatable by the given module finder.
   *
   * @param finder the module finder to query for tool providers
   */
  public void printToolProviders(ModuleFinder finder) {
    ServiceLoader.load(Modules.layer(finder), ToolProvider.class).stream()
        .map(ServiceLoader.Provider::get)
        .map(Bach::printDescribe)
        .sorted()
        .forEach(printStream()::println);
  }

  static String printDescribe(ToolProvider tool) {
    var name = tool.name();
    var module = tool.getClass().getModule();
    var by =
        Optional.ofNullable(module.getDescriptor())
            .map(ModuleDescriptor::toNameAndVersion)
            .orElse(module.toString());
    var info =
        switch (name) {
          case "jar" -> "Create an archive for classes and resources, and update or restore resources";
          case "javac" -> "Read Java class and interface definitions and compile them into class files";
          case "javadoc" -> "Generate HTML pages of API documentation from Java source files";
          case "javap" -> "Disassemble one or more class files";
          case "jdeps" -> "Launch the Java class dependency analyzer";
          case "jlink" -> "Assemble and optimize a set of modules into a custom runtime image";
          case "jmod" -> "Create JMOD files and list the content of existing JMOD files";
          case "jpackage" -> "Package a self-contained Java application";
          case "junit" -> "Launch the JUnit Platform";
          default -> tool.toString();
        };
    return "%s (provided by module %s)\n%s".formatted(name, by, info.indent(2));
  }

  /**
   * Request head-only from the specified uri.
   *
   * @param uri the request URI
   * @param timeout the timeout for this request in seconds
   * @return a response that discarded the response body
   */
  public HttpResponse<Void> httpHead(URI uri, int timeout) {
    var nobody = HttpRequest.BodyPublishers.noBody();
    var duration = Duration.ofSeconds(timeout);
    var request = HttpRequest.newBuilder(uri).method("HEAD", nobody).timeout(duration).build();
    return httpSend(request, HttpResponse.BodyHandlers.discarding());
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
      printStream().println(file + " << " + uri);
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
  public void loadModule(ModuleDirectory directory, ModuleSearcher searcher, String module) {
    if (directory.finder().find(module).isPresent()) return;
    httpCopy(directory.lookup(module, searcher), directory.jar(module));
  }

  /**
   * Load all missing modules of the given module directory.
   *
   * @param directory the module finder to query for already loaded modules
   * @param searcher the searcher to query for linked module-
   */
  public void loadMissingModules(ModuleDirectory directory, ModuleSearcher searcher) {
    while (true) {
      var missing = directory.missing();
      if (missing.isEmpty()) return;
      for (var module : missing)
        httpCopy(directory.lookup(module, searcher), directory.jar(module));
    }
  }

  /**
   * Run the given call using the directory to find its tool provider.
   *
   * @param directory the module finder to query for already loaded modules
   * @param call the name and arguments of the tool to run
   * @return a responce object describing the result of the tool run
   */
  public ToolResponse toolCall(ModuleDirectory directory, ToolCall call) {
    return new ToolRunner(directory.finder()).run(call);
  }

  /**
   * Run the tool using the directory to find it and passing the given arguments.
   *
   * @param directory the module finder to query for already loaded modules
   * @param name the name of the tool to run
   * @param args the array of args to be passed to the tool as strings
   */
  public void toolRun(ModuleDirectory directory, String name, Object... args) {
    var response = toolCall(directory, Command.of(name, args));
    if (!response.out().isEmpty()) printStream().println(response.out());
    printStream().println(response.err());
  }
}
