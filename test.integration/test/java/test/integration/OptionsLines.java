package test.integration;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.api.CodeSpace;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.Tweak;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

record OptionsLines(Options options) {

  public Stream<String> lines() {
    return lines(__ -> true);
  }

  public Stream<String> lines(Predicate<RecordComponent> include) {
    return Stream.of(Options.class.getRecordComponents())
        .filter(include)
        .sorted(Comparator.comparing(RecordComponent::getName))
        .flatMap(this::lines);
  }

  private Stream<String> lines(RecordComponent component) {
    var value = access(options, component);
    var cli = toCli(component.getName());
    if (value instanceof Boolean flag) {
      return flag ? Stream.of(cli) : Stream.empty();
    }
    if (value instanceof Optional<?> optional) {
      return optional.isPresent() ? lines(cli, optional.get()).stream() : Stream.empty();
    }
    if (value instanceof List<?> list) {
      if (list.isEmpty()) return Stream.empty();
      var lines = new ArrayList<String>();
      for (var element : list) lines.addAll(lines(cli, element));
      return lines.stream();
    }
    throw new IllegalArgumentException("Unsupported component type: " + component);
  }

  private static List<String> lines(String cli, Object element) {
    var lines = new ArrayList<String>();
    lines.add(cli);
    if (element instanceof Command<?> command) {
      lines.add("  " + command.name());
      command.arguments().forEach(arg -> lines.add("  " + arg));
    } else if (element instanceof ExternalLibraryVersion elv) {
      lines.add("  " + elv.name().cli());
      lines.add("  " + elv.version());
    } else if (element instanceof ExternalModuleLocation eml) {
      lines.add("  " + eml.module());
      lines.add("  " + eml.uri());
    } else if (element instanceof Tweak tweak) {
      var namesOfSpaces = tweak.spaces().stream().map(CodeSpace::name).map(String::toLowerCase);
      lines.add("  " + namesOfSpaces.collect(Collectors.joining(",")));
      lines.add("  " + tweak.trigger());
      lines.add("  " + tweak.arguments().size());
      tweak.arguments().forEach(arg -> lines.add("  " + arg));
    } else {
      lines.add("  " + element);
    }
    return lines;
  }

  public static String toCli(String componentName) {
    return "--"
        + componentName
            .codePoints()
            .mapToObj(
                i ->
                    Character.isUpperCase(i)
                        ? "-" + new String(Character.toChars(Character.toLowerCase(i)))
                        : new String(Character.toChars(i)))
            .collect(Collectors.joining());
  }

  public static String toComponentName(String cli) {
    var codes = cli.substring(2).codePoints().toArray();
    var builder = new StringBuilder(codes.length * 2);
    for (int i = 0; i < codes.length; i++) {
      int point = codes[i];
      if (point == "-".codePointAt(0)) {
        builder.append(Character.toChars(Character.toUpperCase(codes[++i])));
        continue;
      }
      builder.append(Character.toChars(point));
    }
    return builder.toString();
  }

  private static Object access(Options options, RecordComponent component) {
    try {
      return component.getAccessor().invoke(options);
    } catch (ReflectiveOperationException exception) {
      throw new AssertionError("access failed for " + component + " on " + options, exception);
    }
  }
}
