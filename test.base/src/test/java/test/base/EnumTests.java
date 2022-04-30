package test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class EnumTests {

  @ParameterizedTest
  @CsvSource("""
              NEW,NEW
              TERMINATED,TERMINATED
              """)
  void enumTransformedToString(String expected, Thread.State state) {
    assertEquals(expected, state.toString());
  }

  @ParameterizedTest
  @MethodSource
  @Anno(expected = "NEW", state = Thread.State.NEW)
  @Anno(expected = "TERMINATED", state = Thread.State.TERMINATED)
  void enumTransformedToStringWithAnnotation(Anno anno) {
    assertEquals(anno.expected(), anno.state().toString());
  }

  private static Stream<Arguments> enumTransformedToStringWithAnnotation() throws Exception {
    return Stream.of(
            EnumTests.class
                .getDeclaredMethod("enumTransformedToStringWithAnnotation", Anno.class)
                .getAnnotationsByType(Anno.class))
        .map(Arguments::of);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Repeatable(Annos.class)
  @interface Anno {
    String expected();

    Thread.State state();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Annos {
    Anno[] value();
  }
}
