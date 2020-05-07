package test.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class RecordTests {

  static class Records {

    /**
     * An informative annotation type used to indicate that a class type declaration is intended to
     * be transmuted into a {@code record} as defined by JEP 359, soon.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RecordLike {}

    /** Returns a new instance based on a given template record object and a value override map. */
    public static <R extends Cloneable> R cloneModify(R template, Map<String, Object> overrides) {
      var recordLikeClass = template.getClass();
      if (!recordLikeClass.isAnnotationPresent(RecordLike.class)) throw new AssertionError();
      try {
        @SuppressWarnings("unchecked")
        R clone = (R) recordLikeClass.getDeclaredMethod("clone").invoke(template);
        for (var override : overrides.entrySet()) {
          var componentLikeField = recordLikeClass.getDeclaredField(override.getKey());
          componentLikeField.setAccessible(true);
          componentLikeField.set(clone, override.getValue());
        }
        return clone;
      } catch (ReflectiveOperationException e) {
        throw new AssertionError("Reflection over " + recordLikeClass + " failed: " + e, e);
      }
    }

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

  @Records.RecordLike
  public static final class Legacy implements Cloneable {
    private final String name;

    public Legacy(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Legacy legacy = (Legacy) o;
      return name.equals(legacy.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  @Test
  void copySampleRecord() {
    var template = new Sample(true, "one", List.of(Path.of("one")));
    var expected = new Sample(true, "one", List.of());

    assertEquals(expected, Records.copy(template, Map.of("paths", List.of())));
  }

  @Test
  void copyLegacyClass() {
    var template = new Legacy("two");
    var expected = new Legacy("2");

    assertEquals(expected, Records.cloneModify(template, Map.of("name", "2")));
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
