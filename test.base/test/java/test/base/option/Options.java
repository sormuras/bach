package test.base.option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Options(
    @Help("""
        --verbose
          Print messages about what is going on.
        """)
        boolean verbose,
    @Help("""
        --version
          Print version information and exit.
        """)
        boolean version,
    @Help("""
        --name NAME
          Set the name.
        """) Optional<String> name,
    @Help("""
        --state STATE
          This option is repeatable.
        """)
        List<Thread.State> states)
    implements Cli {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Help {
    String value();
  }

  public static String generateHelpMessage() {
    return Stream.of(canonicalComponents)
        .sorted(Comparator.comparing(RecordComponent::getName))
        .map(Options::generateHelpMessage)
        .collect(Collectors.joining(System.lineSeparator()));
  }

  private static String generateHelpMessage(RecordComponent component) {
    var help = component.getAnnotation(Help.class);
    if (help == null) return "--" + component.getName() + " <undocumented>";
    return help.value().strip();
  }

  public static Options empty() {
    return new Options(false, false, Optional.empty(), List.of());
  }

  public static Options parse(String... args) {
    var options = Options.empty();
    var deque = new ArrayDeque<>(List.of(args));
    while (!deque.isEmpty()) {
      var argument = deque.pop();
      options =
          switch (argument) {
            case "--verbose" -> options.with("verbose");
            case "--version" -> options.with("version");
            case "--name" -> options.with("name", Optional.of(deque.pop().strip()));
            case "--state" -> options.with(
                Thread.State.valueOf(deque.pop().strip().toUpperCase(Locale.ROOT)));
            default -> throw new IllegalArgumentException(argument);
          };
    }
    return options;
  }

  public static Options compose(Options... options) {
    var composite = Options.empty();
    try {
      component:
      for (var component : canonicalComponents) {
        for (var layer : options) {
          var value = component.getAccessor().invoke(layer);
          if (value instanceof Boolean flag && flag
              || value instanceof Optional<?> optional && optional.isPresent()
              || value instanceof List<?> list && !list.isEmpty()) {
            composite = composite.with(component.getName(), value);
            continue component;
          }
        }
      }
    } catch (ReflectiveOperationException exception) {
      throw new Error(exception);
    }
    return composite;
  }

  public List<String> lines() {
    var lines = new ArrayList<String>();
    if (verbose) lines.add("--verbose");
    if (version) lines.add("--version");
    name.ifPresent(
        name -> {
          lines.add("--name");
          lines.add("  " + name);
        });
    for (var state : states) {
      lines.add("--state");
      lines.add("  " + state.name().toLowerCase(Locale.ROOT));
    }
    return List.copyOf(lines);
  }

  public Options with(String flag) {
    return with(flag, true);
  }

  private static final RecordComponent[] canonicalComponents = Options.class.getRecordComponents();
  private static final Constructor<Options> canonicalConstructor = canonicalConstructor();

  @SuppressWarnings("JavaReflectionMemberAccess")
  private static Constructor<Options> canonicalConstructor() {
    var types = Stream.of(canonicalComponents).map(RecordComponent::getType);
    try {
      return Options.class.getDeclaredConstructor(types.toArray(Class<?>[]::new));
    } catch (NoSuchMethodException exception) {
      throw new Error(exception);
    }
  }

  public Options with(String option, Object newValue) {
    var values = new ArrayList<>();
    try {
      for (var component : canonicalComponents) {
        var reset = component.getName().equals(option);
        var oldValue = component.getAccessor().invoke(this);
        values.add(reset ? newValue : oldValue);
      }
      return canonicalConstructor.newInstance(values.toArray(Object[]::new));
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Reflection failed: " + e, e);
    }
  }

  public Options with(Function<Cli, ?> function, Object newValue) {
    var empty = Options.empty();
    var option = new AtomicReference<>("?");
    var instance =
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] {Cli.class},
            (proxy, method, args) -> {
              if (method.getDeclaringClass() == Object.class) return method.invoke(args);
              option.set(method.getName());
              return method.invoke(empty, args);
            });
    function.apply((Cli) instance);
    return with(option.get(), newValue);
  }

  public Options with(Thread.State state, Thread.State... more) {
    var states = new ArrayList<>(this.states);
    states.add(state);
    if (more.length > 0) states.addAll(List.of(more));
    return with("states", List.copyOf(states));
  }
}
