package run.bach.internal;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import run.bach.Bach;
import run.bach.Browser;
import run.bach.ToolCall;

public record LoadValidator(Bach bach) implements Browser.Validator {
  @Override
  public Path validate(URI source, Path target) {
    var calls = new ArrayList<ToolCall>();
    var fragment = source.getFragment();
    var elements = fragment == null ? new String[0] : fragment.split("&");
    for (var element : elements) {
      var property = StringSupport.parseProperty(element);
      var algorithm = property.key();
      var expected = property.value();
      calls.add(
          ToolCall.of("hash")
              .with("--algorithm", algorithm)
              .with("--expected", expected)
              .with(target));
    }
    if (calls.isEmpty() && bach.cli().online()) {
      calls.add(ToolCall.of("sign").with("--verify").with("--file", target));
    }
    if (PathSupport.isJarFile(target)) {
      calls.add(ToolCall.of("jar").with("--validate").with("--file", target));
    }
    if (calls.isEmpty()) {
      bach.log(System.Logger.Level.WARNING, "Do you trust? " + target.toUri());
      return target;
    }
    try {
      calls.stream().parallel().forEach(bach::run);
    } catch (RuntimeException exception) {
      try {
        Files.deleteIfExists(target);
      } catch (Exception ignore) {
      }
      throw exception;
    }
    return target;
  }
}
