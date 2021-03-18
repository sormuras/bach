package com.github.sormuras.bach.util;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** {@link Record}-related utilities. */
public final class Records {

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
          if (object instanceof Record nested) {
            var text = nested.toString();
            if (text.length() <= 99) {
              lines.add(format("%s%s%s: %s", shift, indent, name, text));
              continue;
            }
            lines.add(format("%s%s%s: %s", shift, indent, name, simpleName(nested)));
            lines.addAll(toLines(level + 2, nested));
            continue;
          }
          if (object instanceof Collection<?> collection) {
            lines.addAll(toLines(level, name, collection));
            continue;
          }
          if (object instanceof Map<?, ?> map) {
            lines.addAll(toLines(level, name, map));
            continue;
          }
          lines.add(format("%s%s%s: %s", shift, indent, name, object));
        } catch (ReflectiveOperationException e) {
          lines.add("// Reflection over " + component + " failed: " + e);
        }
      }
      return lines;
    }

    List<String> toLines(int level, String name, Collection<?> collection) {
      var shift = indent.repeat(level);
      if (collection.isEmpty()) return List.of(format("%s%s%s: []", shift, indent, name));
      if (collection instanceof Set<?> set) return toLines(level, name, set);
      if (collection instanceof List<?> list) return toLines(level, name, list);
      return List.of(format("%s%s%s: %s", shift, indent, name, collection));
    }

    List<String> toLines(int level, String name, Set<?> set) {
      var lines = new ArrayList<String>();
      var shift = indent.repeat(level + 1);
      lines.add(format("%s%s: Set(size=%d)", shift, name, set.size()));
      for (var value : set) {
        if (value instanceof Record nested) {
          var text = nested.toString();
          if (text.length() <= 99) {
            lines.add(format("%s%s %s", shift, indent, text));
            continue;
          }
          lines.add(format("%s%s %s", shift, indent, simpleName(nested)));
          lines.addAll(toLines(level + 2, nested));
        } else lines.add(format("%s%s - %s", shift, indent, value));
      }
      return lines;
    }

    List<String> toLines(int level, String name, List<?> list) {
      var lines = new ArrayList<String>();
      var shift = indent.repeat(level + 1);
      lines.add(format("%s%s: List(size=%d)", shift, name, list.size()));
      var iterator = list.listIterator();
      while (iterator.hasNext()) {
        var key = iterator.nextIndex();
        var value = iterator.next();
        if (value instanceof Record nested) {
          var text = nested.toString();
          if (text.length() <= 99) {
            lines.add(format("%s%s%s: %s", shift, indent, key, text));
            continue;
          }
          lines.add(format("%s%s#%s: %s", shift, indent, key, simpleName(nested)));
          lines.addAll(toLines(level + 2, nested));
        } else lines.add(format("%s%s#%s: %s", shift, indent, key, value));
      }
      return lines;
    }

    List<String> toLines(int level, String name, Map<?, ?> map) {
      var lines = new ArrayList<String>();
      var shift = indent.repeat(level + 1);
      lines.add(format("%s%s: Map(size=%d)", shift, name, map.size()));
      for (var entry : map.entrySet()) {
        var key = entry.getKey();
        var value = entry.getValue();
        if (value instanceof Record nested) {
          var text = nested.toString();
          if (text.length() <= 99) {
            lines.add(format("%s%s%s: %s", shift, indent, key, text));
            continue;
          }
          lines.add(format("%s%s(%s) -> %s", shift, indent, key, simpleName(nested)));
          lines.addAll(toLines(level + 2, nested));
        } else lines.add(format("%s%s(%s) -> %s", shift, indent, key, value));
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
