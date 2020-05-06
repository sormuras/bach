package test.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class RecordTests {

  static class Records {
    static <R extends Record> R copy(R template, Map<String, Object> overrides) {
      try {
        var types = new ArrayList<Class<?>>();
        var values = new ArrayList<>();
        for (var component : template.getClass().getRecordComponents()) {
          types.add(component.getType());
          var name = component.getName();
          var overridden = overrides.containsKey(name);
          values.add(overridden ? overrides.get(name) : component.getAccessor().invoke(template));
        }
        var canonical = template.getClass().getDeclaredConstructor(types.toArray(Class[]::new));
        @SuppressWarnings("unchecked")
        var result = (R) canonical.newInstance(values.toArray(Object[]::new));
        return result;
      } catch (ReflectiveOperationException e) {
        throw new AssertionError("Reflection failed: " + e, e);
      }
    }

    /** Returns a multi-line string representation of the given object. */
    public static String toMultiLineString(Record record) {
      return toMultiLineString(0, record, "\t");
    }

    /** Returns a multi-line string representation of the given object. */
    private static String toMultiLineString(int level, Record record, String indent) {
      var lines = new ArrayList<String>();
      if (level == 0) lines.add(record.getClass().getSimpleName());
      var components = record.getClass().getRecordComponents();
      for (var component : components) {
        var name = component.getName();
        var shift = indent.repeat(level);
        try {
          var value = component.getAccessor().invoke(record);
          var nested = value.getClass();
          if (nested.isRecord()) {
            lines.add(String.format("%s%s%s -> %s", shift, indent, name, nested.getSimpleName()));
            lines.add(toMultiLineString(level + 2, (Record) value, indent));
            continue;
          }
          lines.add(String.format("%s%s%s = %s", shift, indent, name, value));
        } catch (ReflectiveOperationException e) {
          lines.add("// Reflection over " + component + " failed: " + e);
        }
      }
      return String.join(System.lineSeparator(), lines);
    }
  }

  public record Sample(boolean flag, String name, List<Path> paths) {}

  @Test
  void copySampleRecord() {
    var template = new Sample(true, "one", List.of(Path.of("one")));
    var expected = new Sample(true, "one", List.of());

    assertEquals(expected, Records.copy(template, Map.of("paths", List.of())));
  }

  @Test
  void printSampleRecordAsTextBlock() {
    var sample = new Sample(true, "one", List.of(Path.of("one")));

    assertEquals("Sample[flag=true, name=one, paths=[one]]", sample.toString());
    assertLinesMatch(
        List.of("Sample", "\tflag = true", "\tname = one", "\tpaths = [one]"),
        Records.toMultiLineString(sample).lines().collect(Collectors.toList()));
  }
}
