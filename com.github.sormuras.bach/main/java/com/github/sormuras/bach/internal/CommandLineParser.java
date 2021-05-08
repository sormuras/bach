package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Tool;
import com.github.sormuras.bach.api.Action;
import com.github.sormuras.bach.api.ExternalLibraryVersion;
import com.github.sormuras.bach.api.ExternalModuleLocation;
import com.github.sormuras.bach.api.Tweak;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class CommandLineParser {

  private final Deque<String> deque;

  public CommandLineParser(List<String> arguments) {
    this.deque = new ArrayDeque<>();
    arguments.stream().flatMap(String::lines).map(String::strip).forEach(deque::add);
  }

  public Options ofCommandLineArguments(List<String> arguments) {
    if (arguments.isEmpty()) return Options.of();
    var cache = Records.of(Options.class).cache();
    var values = cache.values(Options.of());
    while (!deque.isEmpty()) {
      var index = cache.indexOf(deque.peek().startsWith("--") ? deque.removeFirst() : "--action");
      values[index] = read(cache.type().getRecordComponents()[index], values[index]);
    }
    return cache.newRecord(values);
  }

  private Object read(RecordComponent component, Object old) {
    return merge(old, wrap(component, read(component)));
  }

  private Object read(RecordComponent component) {
    if (component.getType() == boolean.class) return true;

    switch (component.getName()) {
      case "actions" -> {
        return Action.ofCli(deque.removeFirst());
      }
      case "chroot" -> {
        return Path.of(deque.removeFirst());
      }
      case "externalLibraryVersions" -> {
        return ExternalLibraryVersion.ofCommandLine(deque::removeFirst);
      }
      case "externalModuleLocations" -> {
        return ExternalModuleLocation.ofCommandLine(deque::removeFirst);
      }
      case "mainJavaRelease" -> {
        return Integer.parseInt(deque.removeFirst());
      }
      case "projectVersion" -> {
        return ModuleDescriptor.Version.parse(deque.removeFirst());
      }
      case "tool" -> {
        var name = deque.removeFirst();
        var args = List.copyOf(deque);
        deque.clear();
        return new Tool(name, args);
      }
      case "tweaks" -> {
        return Tweak.ofCommandLine(deque::removeFirst);
      }
    }

    return deque.removeFirst();
  }

  private static Object wrap(RecordComponent component, Object newValue) {
    if (component.getType() == Optional.class) {
      return newValue instanceof Optional ? newValue : Optional.ofNullable(newValue);
    }
    if (component.getType() == List.class) {
      return newValue instanceof List ? newValue : List.of(newValue);
    }
    return newValue;
  }

  private static Object merge(Object oldValue, Object newValue) {
    if (oldValue instanceof List<?> oldList && newValue instanceof List<?> newList) {
      return Stream.concat(oldList.stream(), newList.stream()).toList();
    }
    return newValue;
  }
}
