package test.base;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/** Swallow and capture texts printed to system streams. */
@Target(METHOD)
@Retention(RUNTIME)
@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
@ExtendWith(SwallowSystem.Extension.class)
public @interface SwallowSystem {

  class Extension implements BeforeEachCallback, ParameterResolver {

    public Extension() {}

    @Override
    public void beforeEach(ExtensionContext context) {
      getOrComputeStreamsIfAbsent(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
      var isTestMethod = context.getTestMethod().isPresent();
      var isStreamsParameter = parameterContext.getParameter().getType() == Streams.class;
      return isTestMethod && isStreamsParameter;
    }

    @Override
    public Streams resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
      return getOrComputeStreamsIfAbsent(context);
    }

    private Streams getOrComputeStreamsIfAbsent(ExtensionContext context) {
      var store = context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
      return store.getOrComputeIfAbsent(Streams.class);
    }
  }

  class Streams implements ExtensionContext.Store.CloseableResource {

    private final PrintStream standardOut, standardErr;
    private final ByteArrayOutputStream out, err;
    private final List<Runnable> shutdownHooks;

    Streams() {
      this.standardOut = System.out;
      this.standardErr = System.err;
      this.out = new ByteArrayOutputStream();
      this.err = new ByteArrayOutputStream();
      this.shutdownHooks = new ArrayList<>();
      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(err));
    }

    public void addShutdownHook(Runnable runnable) {
      shutdownHooks.add(runnable);
    }

    @Override
    public void close() {
      System.setOut(standardOut);
      System.setErr(standardErr);
      shutdownHooks.forEach(Runnable::run);
    }

    public Stream<String> lines() {
      return out.toString().lines();
    }

    public Stream<String> errors() {
      return err.toString().lines();
    }

    @Override
    public String toString() {
      return "out=```" + out.toString() + "```, err=```" + err.toString() + "```";
    }
  }
}
