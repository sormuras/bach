package com.github.sormuras.bach.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.spi.ToolProvider;

public record GrabToolProvider() implements ToolProvider {

  @Override
  public String name() {
    return "grab";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      out.println(
          """
          Usage: grab ASSET ASSET...

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
          grab(out, Asset.of(arg));
          continue;
        }
        var properties = PathSupport.properties(Path.of(arg));
        for (var name : properties.stringPropertyNames()) {
          var target = Path.of(name);
          var value = properties.getProperty(name);
          grab(out, Asset.of(target, value));
        }
      }
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 2;
    }
    return 0;
  }

  void grab(PrintWriter out, Asset asset) throws Exception {
    if (asset.isPresent()) return;
    var target = asset.target();
    var parent = target.getParent();
    if (parent != null) Files.createDirectories(parent);
    if (asset instanceof StringAsset casted) {
      var string = casted.source();
      out.println("<< " + string.length() + " unicode units");
      Files.writeString(target, string);
    }
    if (asset instanceof HttpsAsset casted) {
      var uri = casted.source();
      out.println("<< " + uri);
      try (var stream = uri.toURL().openStream()) {
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    if (asset.isPresent()) {
      out.println(">> " + target);
      return;
    }
    throw new RuntimeException("Checksum mismatch? " + asset);
  }

  sealed interface Asset permits StringAsset, HttpsAsset {

    Object source();

    Path target();

    default boolean isPresent() {
      return Files.exists(target());
    }

    static Asset of(String string) {
      var property = StringSupport.parseProperty(string);
      return Asset.of(Path.of(property.key()), property.value());
    }

    static Asset of(Path target, String value) {
      if (value.startsWith("string:")) return new StringAsset(target, value.substring(7));
      if (value.startsWith("https:")) return new HttpsAsset(target, URI.create(value));
      throw new IllegalArgumentException(value);
    }
  }

  record StringAsset(Path target, String source) implements Asset {
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

  record HttpsAsset(Path target, URI source) implements Asset {
    @Override
    public boolean isPresent() {
      if (Files.notExists(target)) return false;
      var fragment = source.getFragment();
      if (fragment == null) return true;
      for (var element : fragment.split("&")) {
        var property = StringSupport.parseProperty(element);
        var algorithm = property.key();
        var expected = property.value();
        var computed = PathSupport.computeChecksum(target, algorithm);
        if (!expected.equalsIgnoreCase(computed)) {
          return false;
        }
      }
      return true;
    }
  }
}
