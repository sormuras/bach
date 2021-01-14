package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ModuleLookup;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.util.Formattable;
import java.util.Formatter;
import java.util.Optional;

/**
 * A module lookup for an external module.
 *
 * @param module the name of the external module
 * @param uri the uri of the external module
 */
public record ExternalModuleLookup(String module, String uri) implements ModuleLookup, Formattable {
  /**
   * @throws IllegalArgumentException if the given module is {@code null} or is not a legal name
   * @throws IllegalArgumentException if the given uri string violates RFC&nbsp;2396
   */
  public ExternalModuleLookup {
    ModuleDescriptor.newModule(module);
    URI.create(uri);
  }

  @Override
  public Optional<String> lookupModule(String module) {
    return this.module.equals(module) ? Optional.of(uri) : Optional.empty();
  }

  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    if (precision < 0) {
      formatter.format("%s -> %s", module, uri);
      return;
    }
    if (precision == 0) return;
    var builder = new StringBuilder(precision).append(module).append(" -> ");
    var available = precision - builder.length();
    if (available >= uri.length()) {
      builder.append(uri);
    } else {
      var head = available / 2;
      var tail = available % 2 == 0 ? head - 3 : head - 2;
      builder.append(uri, 0, head);
      builder.append("...");
      builder.append(uri, uri.length() - tail, uri.length());
    }
    formatter.format("%s", builder);
  }

  @Override
  public String toString() {
    return module + " -> " + uri;
  }
}
