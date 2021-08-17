package com.github.sormuras.bach.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.spi.ToolProvider;

public record RestoreToolProvider() implements ToolProvider {

  @Override
  public String name() {
    return "restore";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      out.println(
          """
          Usage: restore ASSET ASSET...
    
          An ASSET is either of from TARGET=SCHEME:SOURCE form or denotes a Java `.properties` file
          containing key-value pairs of the former form. Supported schemes are `string` and `https`.
    
          Examples:
            lines.txt=string:First line\\nMiddle line\\nLast line
            lib/foo.jar=https://.../foo.jar#SIZE=789
          or
            .bach/external.properties
          """);
      return 1;
    }
    try {
      for (var arg : args) {
        if (arg.contains("=")) {
          restore(out, Asset.of(arg));
          continue;
        }
        var properties = PathSupport.properties(Path.of(arg));
        for (var name : properties.stringPropertyNames()) {
          var target = Path.of(name);
          var value = properties.getProperty(name);
          restore(out, Asset.of(value, target));
        }
      }
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 2;
    }
    return 0;
  }

  void restore(PrintWriter out, Asset asset) throws Exception {
    if (asset.isPresent()) return;
    var target = asset.target();
    var parent = target.getParent();
    if (parent != null) Files.createDirectories(parent);
    if (asset instanceof StringAsset a) {
      var string = a.source();
      out.println("<< " + string.length() + " unicode units");
      Files.writeString(target, string);
    }
    if (asset instanceof UriAsset a) {
      var uri = a.source();
      out.println("<< " + uri);
      try (var stream = uri.toURL().openStream()) {
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    out.println(">> " + target);
  }

  sealed interface Asset permits StringAsset, UriAsset {

    Object source();

    Path target();

    default boolean isPresent() {
      return Files.exists(target());
    }

    static Asset of(String property) {
      int separator = property.indexOf('=');
      if (separator <= 0) throw new AssertionError("`TARGET=SCHEME:SOURCE` expected: " + property);
      var target = property.substring(0, separator);
      var source = property.substring(separator + 1);
      return Asset.of(source.strip(), Path.of(target.strip()));
    }

    static Asset of(String value, Path target) {
      if (value.startsWith("string:")) return new StringAsset(value.substring(7), target);
      if (value.startsWith("https:")) return new UriAsset(URI.create(value), target);
      throw new IllegalArgumentException(value);
    }
  }

  record StringAsset(String source, Path target) implements Asset {
    @Override
    public boolean isPresent() {
      if (Files.notExists(target)) return false;
      try {
        return source.equals(Files.readString(target));
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
  }

  record UriAsset(URI source, Path target) implements Asset {
    @Override
    public boolean isPresent() {
      if (Files.notExists(target)) return false;
      var fragment = source.getFragment();
      if (fragment == null) return true;
      for (var pair : fragment.split("&")) {
        int separator = pair.indexOf('=');
        if (separator <= 0) throw new AssertionError("`algorithm=value` pattern expected: " + pair);
        var algorithm = pair.substring(0, separator);
        var expected = pair.substring(separator + 1);
        var computed = PathSupport.computeChecksum(target, algorithm);
        if (!expected.equalsIgnoreCase(computed)) {
          return false;
        }
      }
      return true;
    }
  }
}
