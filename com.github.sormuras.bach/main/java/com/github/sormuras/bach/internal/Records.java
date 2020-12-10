package com.github.sormuras.bach.internal;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Internal {@link Record}-related utilities. */
public class Records {
  /** @return a multi-line string representation of the given object */
  public static String toLines(Record record) {
    var lines = new LinesBuilder("  ").toLines(record);
    return String.join(System.lineSeparator(), lines);
  }

  /** An extensible lines generator converting a record to a list of strings. */
  public static class LinesBuilder {

    private final String indent;

    public LinesBuilder(String indent) {
      this.indent = indent;
    }

    public List<String> toLines(Record record) {
      return List.copyOf(toLines(0, record));
    }

    /** Returns a multi-line string representation of the given object. */
    List<String> toLines(int level, Record record) {
      var lines = new ArrayList<String>();
      if (level == 0) lines.add(simpleName(record));
      var shift = indent.repeat(level);
      for (var component : record.getClass().getRecordComponents()) {
        var name = component.getName();
        try {
          var object = component.getAccessor().invoke(record);
          if (object instanceof Record) {
            lines.add(format("%s%s%s: %s", shift, indent, name, simpleName(object)));
            lines.addAll(toLines(level + 2, (Record) object));
            continue;
          }
          if (object instanceof Map) {
            lines.add(format("%s%s%s", shift, indent, name));
            for (var entry : ((Map<?, ?>) object).entrySet()) {
              var key = entry.getKey();
              var value = entry.getValue();
              if (value instanceof Record) {
                lines.add(format("%s%s(%s) -> %s", shift, indent, key, simpleName(value)));
                lines.addAll(toLines(level + 2, (Record) value));
              } else lines.add(format("%s%s(%s) -> %s", shift, indent, key, value));
            }
            continue;
          }
          lines.add(format("%s%s%s: %s", shift, indent, name, object));
        } catch (ReflectiveOperationException e) {
          lines.add("// Reflection over " + component + " failed: " + e);
        }
      }
      return lines;
    }
  }

  private static String simpleName(Object object) {
    return object.getClass().getSimpleName();
  }

  /** Hidden default constructor. */
  private Records() {}
}
