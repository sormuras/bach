package run.duke;

import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CommandLineInterface<R extends Record> {
  public static <R extends Record> CommandLineInterface<R> of(Lookup lookup, Class<R> schema) {
    return new CommandLineInterface<>(lookup, schema);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  public @interface Name {
    String[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  public @interface Help {
    String[] value();
  }

  record Option(Type type, Set<String> names, String help, Class<? extends Record> nestedSchema) {
    public enum Type {
      /** An optional flag, like {@code --verbose}. */
      FLAG(false),
      /** An optional key-value pair, like {@code --version 47.11}. */
      KEY_VALUE(Optional.empty()),
      /** An optional and repeatable key, like {@code --with alpha --with omega} */
      REPEATABLE(List.of()),
      /** A required positional option */
      REQUIRED(""),
      /** A collection of all unhandled arguments. */
      VARARGS(new String[0]);

      private final Object defaultValue;

      Type(Object defaultValue) {
        this.defaultValue = defaultValue;
      }

      Object defaultValue() {
        return defaultValue;
      }

      static Type valueOf(Class<?> type) {
        if (type == Boolean.class || type == boolean.class) return FLAG;
        if (type == Optional.class) return KEY_VALUE;
        if (type == List.class) return REPEATABLE;
        if (type == String.class) return REQUIRED;
        if (type == String[].class) return VARARGS;
        throw new IllegalArgumentException("Unsupported value type: " + type);
      }
    }

    public Option {
      requireNonNull(type, "type is null");
      requireNonNull(names, "named is null");
      requireNonNull(help, "help is null");
      names = Collections.unmodifiableSet(new LinkedHashSet<>(names));
      if (names.isEmpty()) throw new IllegalArgumentException("no name defined");
    }

    public static List<Option> scan(Class<? extends Record> schema) {
      requireNonNull(schema, "schema is null");
      var recordComponents = schema.getRecordComponents();
      if (recordComponents == null) {
        throw new IllegalArgumentException("the schema is not a record");
      }
      return Arrays.stream(recordComponents).map(Option::of).toList();
    }

    public static Option of(RecordComponent component) {
      requireNonNull(component, "component is null");
      var nameAnno = component.getAnnotation(Name.class);
      var helpAnno = component.getAnnotation(Help.class);
      var names =
          nameAnno != null
              ? new LinkedHashSet<>(Arrays.asList(nameAnno.value()))
              : Set.of(component.getName().replace('_', '-'));
      var type = Type.valueOf(component.getType());
      var help = helpAnno != null ? String.join("\n", helpAnno.value()) : "";
      var nestedSchema =
          (component.getGenericType() instanceof ParameterizedType paramType
                  && paramType.getActualTypeArguments()[0] instanceof Class<?> nestedType
                  && nestedType.isRecord())
              ? nestedType.asSubclass(Record.class)
              : null;
      return new Option(type, names, help, nestedSchema);
    }

    String name() {
      return names.iterator().next();
    }

    boolean isVarargs() {
      return type == Type.VARARGS;
    }

    boolean isRequired() {
      return type == Type.REQUIRED;
    }

    boolean isFlag() {
      return type == Type.FLAG;
    }
  }

  private final Lookup lookup;
  private final Class<R> schema;
  private final List<Option> options;
  private final ArgumentsProcessor processor;
  private final boolean nested;

  public CommandLineInterface(Lookup lookup, Class<R> schema) {
    this(lookup, schema, ArgumentsProcessor.DEFAULT);
  }

  public CommandLineInterface(Lookup lookup, Class<R> schema, ArgumentsProcessor processor) {
    this(lookup, schema, Option.scan(schema), processor);
  }

  private CommandLineInterface(
      Lookup lookup, Class<R> schema, List<Option> options, ArgumentsProcessor processor) {
    this(lookup, schema, options, processor, false);
  }

  private CommandLineInterface(
      Lookup lookup,
      Class<R> schema,
      List<Option> options,
      ArgumentsProcessor processor,
      boolean nested) {
    requireNonNull(lookup, "lookup is null");
    requireNonNull(schema, "schema is null");
    requireNonNull(options, "options is null");
    requireNonNull(processor, "processor is null");
    var opts = List.copyOf(options);
    if (opts.isEmpty()) throw new IllegalArgumentException("At least one option is expected");
    checkDuplicates(opts);
    checkVarargs(opts);
    this.lookup = lookup;
    this.schema = schema;
    this.options = opts;
    this.processor = processor;
    this.nested = nested;
  }

  private static void checkDuplicates(List<Option> options) {
    var optionsByName = new HashMap<String, Option>();
    for (var option : options) {
      var names = option.names();
      for (var name : names) {
        var otherOption = optionsByName.put(name, option);
        if (otherOption == option)
          throw new IllegalArgumentException(
              "option " + option + " declares duplicated name " + name);
        if (otherOption != null)
          throw new IllegalArgumentException(
              "options " + option + " and " + otherOption + " both declares name " + name);
      }
    }
  }

  private static void checkVarargs(List<Option> options) {
    var varargs = options.stream().filter(Option::isVarargs).toList();
    if (varargs.size() > 1) {
      throw new IllegalArgumentException("Too many varargs types specified: " + varargs);
    }
    if (varargs.size() == 1 && !(options.get(options.size() - 1).isVarargs())) {
      throw new IllegalArgumentException("varargs is not at last index: " + options);
    }
  }

  private Object[] split(ArrayDeque<String> pendingArguments) {
    var requiredOptions =
        options.stream().filter(Option::isRequired).collect(toCollection(ArrayDeque::new));
    var optionsByName = new HashMap<String, Option>();
    var workspace = new LinkedHashMap<String, Object>();
    var flagCount = options.stream().filter(Option::isFlag).count();
    var flagPattern = flagCount == 0 ? null : Pattern.compile("^-[a-zA-Z]{1," + flagCount + "}$");
    for (var option : options) {
      for (var name : option.names()) {
        optionsByName.put(name, option);
      }
      workspace.put(option.name(), option.type().defaultValue());
    }

    while (true) {
      if (pendingArguments.isEmpty()) {
        if (requiredOptions.isEmpty()) return workspace.values().toArray();
        throw new IllegalArgumentException("Required option(s) missing: " + requiredOptions);
      }
      // acquire next argument
      var argument = pendingArguments.removeFirst();
      int separator = argument.indexOf('=');
      var noValue = separator == -1;
      var maybeName = noValue ? argument : argument.substring(0, separator);
      var maybeValue = noValue ? "" : argument.substring(separator + 1);
      // try well-known option first
      if (optionsByName.containsKey(maybeName)) {
        var option = optionsByName.get(maybeName);
        var name = option.name();
        workspace.put(
            name,
            switch (option.type()) {
              case FLAG -> noValue || parseBoolean(maybeValue);
              case KEY_VALUE -> {
                var value =
                    option.nestedSchema() != null
                        ? splitNested(pendingArguments, option)
                        : noValue ? pendingArguments.pop() : maybeValue;
                yield Optional.of(value);
              }
              case REPEATABLE -> {
                var value =
                    option.nestedSchema() != null
                        ? List.of(splitNested(pendingArguments, option))
                        : noValue
                            ? List.of(pendingArguments.pop())
                            : List.of(maybeValue.split(","));
                var elements = (List<?>) workspace.get(name);
                yield Stream.concat(elements.stream(), value.stream()).toList();
              }
              case VARARGS, REQUIRED -> throw new AssertionError("Unnamed name? " + name);
            });
        continue;
      }
      // maybe a combination of single letter flags?
      if (flagPattern != null && flagPattern.matcher(argument).matches()) {
        var flags = argument.substring(1).chars().mapToObj(c -> "-" + (char) c).toList();
        if (flags.stream().allMatch(optionsByName::containsKey)) {
          flags.forEach(flag -> workspace.put(optionsByName.get(flag).name(), true));
          continue;
        }
      }
      // try required option
      if (!requiredOptions.isEmpty()) {
        var requiredOption = requiredOptions.pop();
        workspace.put(requiredOption.name(), argument);
        continue;
      }
      // restore pending arguments deque
      pendingArguments.addFirst(argument);
      if (nested) return workspace.values().toArray();
      // try globbing all pending arguments into a varargs collector
      var varargsOption = options.get(options.size() - 1);
      if (varargsOption.isVarargs()) {
        workspace.put(varargsOption.name(), pendingArguments.toArray(String[]::new));
        return workspace.values().toArray();
      }
      throw new IllegalArgumentException("Unhandled arguments: " + pendingArguments);
    }
  }

  private Object splitNested(ArrayDeque<String> pendingArguments, Option option) {
    var nestedSchema = option.nestedSchema;
    var nestedOptions = Option.scan(nestedSchema);
    var nestedSplitter =
        new CommandLineInterface<>(lookup, nestedSchema, nestedOptions, processor, true);
    var constructor = constructor(lookup, nestedSchema);
    return createRecord(constructor, nestedSplitter.split(pendingArguments));
  }

  public R split(String... args) {
    requireNonNull(args, "args is null");
    return split(Arrays.stream(args));
  }

  public R split(Stream<String> args) {
    requireNonNull(args, "args is null");
    var constructor = constructor(lookup, schema).asFixedArity();
    var values = processor.process(args).collect(toCollection(ArrayDeque::new));
    var arguments = split(values);
    return schema.cast(createRecord(constructor, arguments));
  }

  private static MethodHandle constructor(Lookup lookup, Class<?> schema) {
    var components = schema.getRecordComponents();
    var types = Stream.of(components).map(RecordComponent::getType).toArray(Class[]::new);
    try {
      return lookup.findConstructor(schema, MethodType.methodType(void.class, types));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Object createRecord(MethodHandle constructor, Object[] args) {
    try {
      return constructor.invokeWithArguments(args);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
  }

  public String help() {
    return help(2);
  }

  public String help(int indent) {
    if (indent < 0) throw new IllegalArgumentException("invalid indent " + indent);
    var joiner = new StringJoiner("\n");
    for (var option : options.stream().sorted(Comparator.comparing(Option::name)).toList()) {
      var text = option.help();
      if (text.isEmpty()) continue;
      var suffix =
          switch (option.type()) {
            case FLAG -> " (flag)";
            case KEY_VALUE -> " <value>";
            case REPEATABLE -> " <value> (repeatable)";
            case REQUIRED -> " (required)";
            case VARARGS -> "...";
          };
      var names = String.join(", ", option.names());
      joiner.add(names + suffix);
      joiner.add(text.indent(indent).stripTrailing());
    }
    return joiner.toString();
  }

  @FunctionalInterface
  public interface ArgumentsProcessor {
    ArgumentsProcessor IDENTITY = arguments -> arguments;
    ArgumentsProcessor TRIM = arguments -> arguments.map(String::trim);
    ArgumentsProcessor PRUNE = arguments -> arguments.filter(not(String::isEmpty));
    ArgumentsProcessor NORMALIZE = TRIM.andThen(PRUNE);
    ArgumentsProcessor EXPAND = ArgumentsProcessor::expandAtFileArguments;
    ArgumentsProcessor DEFAULT = NORMALIZE.andThen(EXPAND);

    private static Stream<String> expandAtFileArguments(Stream<String> source) {
      return source.mapMulti(
          (argument, consumer) -> {
            if (argument.startsWith("@") && !(argument.startsWith("@@"))) {
              var file = Path.of(argument.substring(1));
              expandArgumentsFile(file, consumer);
              return;
            }
            consumer.accept(argument);
          });
    }

    private static void expandArgumentsFile(Path file, Consumer<String> consumer) {
      List<String> lines;
      try {
        lines = Files.readAllLines(file);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      for (var line : lines) {
        line = line.strip();
        if (line.isEmpty()) continue;
        if (line.startsWith("#")) continue;
        if (line.startsWith("@") && !line.startsWith("@@")) {
          throw new IllegalArgumentException("Expand arguments file not allowed: " + line);
        }
        consumer.accept(line);
      }
    }

    Stream<String> process(Stream<String> arguments);

    default ArgumentsProcessor andThen(ArgumentsProcessor after) {
      requireNonNull(after, "after is null");
      return stream -> after.process(process(stream));
    }
  }
}
