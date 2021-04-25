package test.integration.core;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.api.UnsupportedOptionException;
import com.github.sormuras.bach.core.ModuleRealm;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleRealmTests {
  @ParameterizedTest
  @ValueSource(classes = Object.class)
  void system(Class<?> type) {
    assertSame(ModuleRealm.SYSTEM, ModuleRealm.of(type.getModule()));
  }

  @ParameterizedTest
  @ValueSource(classes = {Test.class, ModuleRealmTests.class, Bach.class})
  void external(Class<?> type) {
    assertSame(ModuleRealm.EXTERNAL, ModuleRealm.of(type.getModule()));
  }

  @Test
  void unknown() {
    class NullLocationModuleReference extends ModuleReference {

      protected NullLocationModuleReference() {
        super( ModuleDescriptor.newModule("unknown").build(), null);
      }

      @Override
      public ModuleReader open() {
        throw new UnsupportedOptionException("open");
      }
    }

    assertSame(ModuleRealm.UNKNOWN, ModuleRealm.of(new NullLocationModuleReference()));
  }
}
