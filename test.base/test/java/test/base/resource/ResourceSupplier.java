package test.base.resource;

import java.util.function.Supplier;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

public interface ResourceSupplier<R> extends AutoCloseable, CloseableResource, Supplier<R> {

  default Object as(Class<?> parameterType) {
    // TODO find unique converter, like String Object#toString() or File Path#toFile()?
    throw new UnsupportedOperationException("Can't convert to " + parameterType);
  }

  @Override
  default void close() {
    R instance = get();
    if (instance instanceof AutoCloseable) {
      try {
        ((AutoCloseable) instance).close();
      } catch (Exception e) {
        // TODO better exception handling by reporting or re-throwing?
        throw new RuntimeException("Close failed: " + instance, e);
      }
    }
  }
}