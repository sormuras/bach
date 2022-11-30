package run.duke;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;

public interface CommandLineInterface {

  static <R extends Record> Parser<R> parser(Lookup lookup, Class<R> schema) {
    return new Parser<>(lookup, schema);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Name {
    String[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Help {
    String[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  @interface Cardinality {
    int value();
  }

  // compact flags: -fg
  // sub-records

  record Option(
      Type type,
      Set<String> names,
      String help,
      int cardinality,
      Class<? extends Record> subSchema) {
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
      Objects.requireNonNull(type, "type is null");
      Objects.requireNonNull(names, "named is null");
      Objects.requireNonNull(help, "help is null");
      names = Collections.unmodifiableSet(new LinkedHashSet<>(names));
      if (names.isEmpty()) {
        throw new IllegalArgumentException("no name defined");
      }
      if (cardinality < 1) {
        throw new IllegalArgumentException("invalid cardinality " + cardinality);
      }
    }

    public static List<Option> scan(Class<? extends Record> schema) {
      Objects.requireNonNull(schema, "schema is null");
      var recordComponents = schema.getRecordComponents();
      if (recordComponents == null) {
        throw new IllegalArgumentException("the schema is not a record");
      }
      return Arrays.stream(recordComponents).map(Option::of).toList();
    }

    public static Option of(RecordComponent component) {
      Objects.requireNonNull(component, "component is null");
      var name = component.getAnnotation(Name.class);
      var help = component.getAnnotation(Help.class);
      var cardinality = component.getAnnotation(Cardinality.class);
      return new Option(
          Type.valueOf(component.getType()),
          name != null
              ? new LinkedHashSet<>(Arrays.asList(name.value()))
              : Set.of(component.getName().replace('_', '-')),
          help != null ? String.join("\n", help.value()) : "",
          cardinality != null ? cardinality.value() : 1,
          (component.getGenericType() instanceof ParameterizedType type
                  && type.getActualTypeArguments()[0] instanceof Class<?> subOption
                  && subOption.isRecord())
              ? subOption.asSubclass(Record.class)
              : null);
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
  }

  final class Parser<R extends Record> {
    private final Lookup lookup;
    private final Class<R> schema;
    private final List<Option> options;
    private final ArgumentsProcessor processor;
    private final boolean sub;

    public Parser(Lookup lookup, Class<R> schema) {
      this(lookup, schema, ArgumentsProcessor.DEFAULT);
    }

    public Parser(Lookup lookup, Class<R> schema, ArgumentsProcessor processor) {
      this(lookup, schema, Option.scan(schema), processor);
    }

    public Parser(
        Lookup lookup, Class<R> schema, List<Option> options, ArgumentsProcessor processor) {
      this(lookup, schema, options, processor, false);
    }

    private Parser(
        Lookup lookup,
        Class<R> schema,
        List<Option> options,
        ArgumentsProcessor processor,
        boolean sub) {
      Objects.requireNonNull(lookup, "lookup is null");
      Objects.requireNonNull(schema, "schema is null");
      Objects.requireNonNull(options, "options is null");
      Objects.requireNonNull(processor, "processor is null");
      var opts = List.copyOf(options);
      if (opts.isEmpty()) {
        throw new IllegalArgumentException("At least one option is expected");
      }
      checkDuplicates(opts);
      checkVarargs(opts);
      this.lookup = lookup;
      this.schema = schema;
      this.options = opts;
      this.processor = processor;
      this.sub = sub;
    }

    private static void checkDuplicates(List<Option> options) {
      var optionsByName = new HashMap<String, Option>();
      for (var option : options) {
        var names = option.names();
        for (var name : names) {
          var otherOption = optionsByName.put(name, option);
          if (otherOption == option) {
            throw new IllegalArgumentException(
                "option " + option + " declares duplicated name " + name);
          }
          if (otherOption != null) {
            throw new IllegalArgumentException(
                "options " + option + " and " + otherOption + " both declares name " + name);
          }
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

    private Object[] parse(ArrayDeque<String> pendingArguments) {
      var requiredOptions =
          options.stream().filter(Option::isRequired).collect(toCollection(ArrayDeque::new));
      var optionsByName = new HashMap<String, Option>();
      var workspace = new LinkedHashMap<String, Object>();
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
        var pop = separator == -1;
        var maybeName = pop ? argument : argument.substring(0, separator);
        // try well-known option first
        if (optionsByName.containsKey(maybeName)) {
          var option = optionsByName.get(maybeName);
          var name = option.name();
          switch (option.type()) {
            case FLAG -> workspace.put(name, true);
            case KEY_VALUE -> {
              var value =
                  option.subSchema() != null
                      ? parseSub(pendingArguments, option)
                      : pop ? pendingArguments.pop() : argument.substring(separator + 1);
              workspace.put(name, Optional.of(value));
            }
            case REPEATABLE -> {
              var times = option.cardinality();
              var value =
                  option.subSchema() != null
                      ? List.of(parseSub(pendingArguments, option))
                      : pop
                          ? IntStream.range(0, times)
                              .mapToObj(__ -> pendingArguments.pop())
                              .toList()
                          : List.of(argument.substring(separator + 1).split(","));
              @SuppressWarnings("unchecked")
              var elements = (List<String>) workspace.get(name);
              workspace.put(name, Stream.concat(elements.stream(), value.stream()).toList());
            }
            default -> throw new IllegalStateException("Programming error");
          }
          continue;
        }
        // maybe a combination of single letter flags?
        if (argument.matches("^-[a-zA-Z]{1,5}$")) {
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
        if (sub) return workspace.values().toArray();
        // try globbing all pending arguments into a varargs collector
        var varargsOption = options.get(options.size() - 1);
        if (varargsOption.isVarargs()) {
          workspace.put(varargsOption.name(), pendingArguments.toArray(String[]::new));
          return workspace.values().toArray();
        }
        throw new IllegalArgumentException("Unhandled arguments: " + pendingArguments);
      }
    }

    private Object parseSub(ArrayDeque<String> pendingArguments, Option option) {
      var schema = option.subSchema;
      var subParser = new Parser<>(lookup, schema, Option.scan(schema), processor, true);
      var constructor = constructor(lookup, schema);
      return createRecord(schema, constructor, subParser.parse(pendingArguments));
    }

    public R parse(String... args) {
      Objects.requireNonNull(args, "args is null");
      return parse(Arrays.stream(args));
    }

    public R parse(Stream<String> args) {
      Objects.requireNonNull(args, "args is null");
      var constructor = constructor(lookup, schema);
      var values = processor.process(args).collect(toCollection(ArrayDeque::new));
      var objects = parse(values);
      var arguments = constructor.isVarargsCollector() ? spreadVarargs(objects) : objects;
      return schema.cast(createRecord(schema, constructor, arguments));
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

    private static Record createRecord(
        Class<? extends Record> schema, MethodHandle constructor, Object[] arguments) {
      try {
        return schema.cast(constructor.invokeWithArguments(arguments));
      } catch (RuntimeException | Error e) {
        throw e;
      } catch (Throwable e) {
        throw new UndeclaredThrowableException(e);
      }
    }

    // From [ ..., x, ["1", "2", "3"]] to [..., x, "1", "2", "3"]
    private static Object[] spreadVarargs(Object[] source) {
      var head = source.length - 1;
      var last = (String[]) source[head];
      var tail = Array.getLength(last);
      var target = new Object[head + tail];
      System.arraycopy(source, 0, target, 0, head);
      System.arraycopy(last, 0, target, head, tail);
      return target;
    }

    public String help() {
      return help(2);
    }

    public String help(int indent) {
      if (indent < 0) {
        throw new IllegalArgumentException("invalid indent " + indent);
      }
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
  }

  interface ArgumentsProcessor {
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
      return stream -> after.process(process(stream));
    }
  }
}
