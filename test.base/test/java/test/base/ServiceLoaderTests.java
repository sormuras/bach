package test.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class ServiceLoaderTests {

  static final ServiceLoader<ToolProvider> loader =
      ServiceLoader.load(ToolProvider.class, Thread.currentThread().getContextClassLoader());

  @Test
  void findToolProviderTypesNotAnnotatedWithName() {
    var names =
        loader.stream()
            .filter(provider -> !provider.type().isAnnotationPresent(Name.class))
            .map(ServiceLoader.Provider::type)
            .map(Class::getSimpleName)
            .toList();

    assertTrue(names.contains("TestProvider1"));
    assertFalse(names.contains("TestProvider2"));
  }

  @Test
  void findToolProviderTypesAnnotatedWithSpecificName() {
    var names =
        loader.stream()
            .filter(provider -> new Name.Support(provider.type()).test("test"))
            .map(ServiceLoader.Provider::type)
            .map(Class::getSimpleName)
            .toList();

    assertLinesMatch(List.of("TestProvider2"), names);
  }
}
