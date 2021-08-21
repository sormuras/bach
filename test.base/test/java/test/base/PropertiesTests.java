package test.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PropertiesTests {

  @Test
  void loadMultiLineValuesFromTextBlock() throws Exception {
    var expected = """
        1
        2""";

    var string =
        """
        --a 1\\n2
        --b 1\\n\
        2
        --c \
        1\\n\
        2
        """;
    var properties = new Properties();
    properties.load(new StringReader(string));

    assertEquals(expected, properties.getProperty("--a"));
    assertEquals(expected, properties.getProperty("--b"));
    assertEquals(expected, properties.getProperty("--c"));
  }
}
