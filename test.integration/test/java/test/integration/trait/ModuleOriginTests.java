package test.integration.trait;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.ModuleOrigin;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleOriginTests {
  @ParameterizedTest
  @ValueSource(classes = Object.class)
  void system(Class<?> type) {
    assertSame(ModuleOrigin.SYSTEM, ModuleOrigin.of(type.getModule()));
  }

  @ParameterizedTest
  @ValueSource(classes = {Test.class, ModuleOriginTests.class, Bach.class})
  void external(Class<?> type) {
    assertSame(ModuleOrigin.EXTERNAL, ModuleOrigin.of(type.getModule()));
  }

  @Test
  void unknown() {
    class NullLocationModuleReference extends ModuleReference {

      protected NullLocationModuleReference() {
        super( ModuleDescriptor.newModule("unknown").build(), null);
      }

      @Override
      public ModuleReader open() {
        throw new UnsupportedOperationException("open");
      }
    }

    assertSame(ModuleOrigin.UNKNOWN, ModuleOrigin.of(new NullLocationModuleReference()));
  }
}
