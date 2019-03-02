import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

@Target({METHOD, TYPE})
@Retention(RUNTIME)
@ExtendWith(BachExtension.Extension.class)
public @interface BachExtension {

  class Extension implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
      var type = parameterContext.getParameter().getType();
      return type.equals(BachSupplier.class) || type.equals(Bach.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
      var supplier = new BachSupplier();
      var type = parameterContext.getParameter().getType();
      if (type.equals(BachSupplier.class)) {
        return supplier;
      }
      if (type.equals(Bach.class)) {
        return supplier.bach;
      }
      throw new ParameterResolutionException("Can't resolve parameter of type: " + type);
    }
  }

  class BachSupplier implements Supplier<Bach> {
    private final Bach bach;
    private final ByteArrayOutputStream out, err;

    BachSupplier() {
      this.bach = new Bach();
      this.out = new ByteArrayOutputStream();
      this.err = new ByteArrayOutputStream();

      bach.level = System.Logger.Level.ALL;
      bach.out = new PrintStream(out);
      bach.err = new PrintStream(err);
    }

    @Override
    public Bach get() {
      return bach;
    }

    List<String> outLines() {
      return out.toString().lines().collect(Collectors.toList());
    }

    List<String> errLines() {
      return err.toString().lines().collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "out=```" + out.toString() + "```, err=```" + err.toString() + "```";
    }

    void printStreams() {
      outLines().forEach(System.out::println);
      System.out.flush();
      errLines().forEach(System.err::println);
      System.err.flush();
    }
  }
}
