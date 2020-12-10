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
      if (level == 0) lines.add(record.getClass().getSimpleName());
      var shift = indent.repeat(level);
      for (var component : record.getClass().getRecordComponents()) {
        var name = component.getName();
        try {
          var value = component.getAccessor().invoke(record);
          if (value instanceof Record) {
            lines.add(format("%s%s%s: %s", shift, indent, name, value.getClass().getSimpleName()));
            lines.addAll(toLines(level + 2, (Record) value));
            continue;
          }
          if (value instanceof Map) {
            lines.add(format("%s%s%s", shift, indent, name));
            for (var entry : ((Map<?, ?>) value).entrySet()) {
              var entryKey = entry.getKey();
              var entryValue = entry.getValue();
              if (entryValue instanceof Record) {
                lines.add(
                    format(
                        "%s%s(%s) -> %s",
                        shift, indent, entryKey, entryValue.getClass().getSimpleName()));
                lines.addAll(toLines(level + 2, (Record) entryValue));
              } else lines.add(format("%s%s  %s -> %s", shift, indent, entryKey, entryValue));
            }
            continue;
          }
          lines.add(format("%s%s%s: %s", shift, indent, name, value));
        } catch (ReflectiveOperationException e) {
          lines.add("// Reflection over " + component + " failed: " + e);
        }
      }
      return lines;
    }
  }

  /** Hidden default constructor. */
  private Records() {}
}
