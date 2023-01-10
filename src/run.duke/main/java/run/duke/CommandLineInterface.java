// Generated on 2023-01-09T11:41:58.423041900+01:00[Europe/Berlin]
package run.duke;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class CommandLineInterface {

  /**
   * A command line argument model with focus on converting a sequence of arguments to an object
   * representation as well as allowing to print a man page or validate the input.
   *
   * @param <T> target type of the object that represents the resulting command line arguments
   */
  public interface CommandLine<T> {

    /**
     * @return lists all options of a command in their declaration order
     */
    List<? extends Option> options();

    /**
     * @param test filter that returns true for those {@link OptionType}s to keep
     * @return lists all options of a command of the given type in their declaration order
     */
    default Stream<? extends Option> options(Predicate<OptionType> test) {
      return options().stream().filter(opt -> test.test(opt.type()));
    }

    /**
     * Completes the handling of command options. This is called when all {@link Option#add(String)}
     * calls have been made.
     *
     * @return the result value of the command
     */
    T complete();

    interface Option {

      String name();

      OptionType type();

      List<String> handles();

      Class<?> of();

      Optional<? extends Factory<?>> sub();

      /**
       * Adds or sets the option raw value as extracted from the command line arguments.
       *
       * @param value raw value to add or set for this option
       */
      void add(String value);
    }

    /**
     * Creates new instances of a {@link CommandLine} state.
     *
     * @param <T> type of the command result value
     */
    @FunctionalInterface
    interface Factory<T> {

      /**
       * @return a fresh instance of a command with empty (initial) state
       */
      CommandLine<T> create();
    }

    /**
     * Create a new builder where aggregation and result state are the same.
     *
     * @param init creates a new instance of the result value
     * @return a builder to add options to the command
     * @param <T> type of the aggregation and command result state
     */
    static <T> Builder<T, T> builder(Supplier<T> init) {
      return builder(init, Function.identity());
    }

    /**
     * Create a new builder with dedicated intermediate state.
     *
     * @param init creates a new instance of the aggregation state, must be a pure function
     * @param exit transforms the aggregation state into the final result, must be a pure function
     * @return a builder to add options to the command
     * @param <A> type of the aggregation state
     * @param <T> type of the command result value
     */
    static <A, T> Builder<A, T> builder(Supplier<A> init, Function<A, T> exit) {
      return new Builder<>(List.of(), init, exit);
    }

    /**
     * Allows to programmatically compose a command. Once all options have been added a {@link
     * Factory} is created using {@link #build()}.
     *
     * @param <A> type of the aggregation state (the intermediate value used to collect option
     *     values)
     * @param <T> type of the result value created from the aggregation state
     */
    final class Builder<A, T> {

      private final List<OptionValue<A, ?>> options;
      private final Supplier<A> init;
      private final Function<A, T> exit;

      private Builder(List<OptionValue<A, ?>> options, Supplier<A> init, Function<A, T> exit) {
        requireNonNull(init, "init is null");
        requireNonNull(exit, "exit is null");
        this.options = options;
        this.init = init;
        this.exit = exit;
      }

      public Factory<T> build() {
        // this is to hide the create method from the Builder class even if it is implemented here
        // the copy is made so that the builder instance used can be changed further without
        // affecting the factory
        return new Builder<>(List.copyOf(options), init, exit)::createCommand;
      }

      private CommandLine<T> createCommand() {
        A state = init.get();
        var optionValues = options.stream().map(OptionValue::empty).toList();
        record Instance<A, T>(
            List<? extends OptionValue<A, ?>> options, A state, Function<A, T> exit)
            implements CommandLine<T> {
          @Override
          public T complete() {
            options.forEach(opt -> opt.complete(state));
            return exit.apply(state);
          }
        }
        checkNoHandleCollisions(optionValues);
        checkVarargs(optionValues);
        return new Instance<>(optionValues, state, exit);
      }

      private Builder<A, T> add(OptionValue<A, ?> option) {
        return new Builder<>(
            Stream.concat(options.stream(), Stream.of(option)).toList(), init, exit);
      }

      private <V> Builder<A, T> add(
          String name,
          OptionType type,
          String[] handles,
          Class<V> of,
          Function<String, V> from,
          BiConsumer<A, List<V>> to,
          Factory<V> sub) {
        return add(
            new OptionValue<>(
                name, type, List.of(handles), of, from, to, Optional.ofNullable(sub), List.of()));
      }

      public <V> Builder<A, T> addSub(
          String name, Class<V> of, BiConsumer<A, V> to, Factory<V> from, String... handles) {
        return add(name, OptionType.SUB, handles, of, str -> null, valueToList(to, null), from);
      }

      public Builder<A, T> addFlag(String name, BiConsumer<A, Boolean> to, String... handles) {
        return add(
            name,
            OptionType.FLAG,
            handles,
            Boolean.class,
            Boolean::valueOf,
            valueToList(to, false),
            null);
      }

      public Builder<A, T> addOptional(
          String name, BiConsumer<A, Optional<String>> to, String... handles) {
        return addOptional(name, String.class, Function.identity(), to, handles);
      }

      public <V> Builder<A, T> addOptional(
          String name,
          Class<V> of,
          Function<String, V> from,
          BiConsumer<A, Optional<V>> to,
          String... handles) {
        return add(name, OptionType.OPTIONAL, handles, of, from, optionalToList(to), null);
      }

      public <V> Builder<A, T> addOptional(
          String name,
          Class<V> of,
          BiConsumer<A, Optional<V>> to,
          Factory<V> from,
          String... handles) {
        return add(name, OptionType.OPTIONAL, handles, of, str -> null, optionalToList(to), from);
      }

      public Builder<A, T> addRequired(String name, BiConsumer<A, String> to, String... handles) {
        return addRequired(name, String.class, Function.identity(), to, handles);
      }

      public <V> Builder<A, T> addRequired(
          String name,
          Class<V> of,
          Function<String, V> from,
          BiConsumer<A, V> to,
          String... handles) {
        return add(name, OptionType.REQUIRED, handles, of, from, valueToList(to, null), null);
      }

      public Builder<A, T> addRepeatable(
          String name, BiConsumer<A, List<String>> to, String... handles) {
        return addRepeatable(name, String.class, Function.identity(), to, handles);
      }

      public <V> Builder<A, T> addRepeatable(
          String name,
          Class<V> of,
          Function<String, V> from,
          BiConsumer<A, List<V>> to,
          String... handles) {
        return add(name, OptionType.REPEATABLE, handles, of, from, to, null);
      }

      public <V> Builder<A, T> addRepeatable(
          String name, Class<V> of, BiConsumer<A, List<V>> to, Factory<V> from, String... handles) {
        return add(name, OptionType.REPEATABLE, handles, of, str -> null, to, from);
      }

      public Builder<A, T> addVarargs(String name, BiConsumer<A, String[]> to, String... handles) {
        return addVarargs(name, String.class, Function.identity(), to, handles);
      }

      public <V> Builder<A, T> addVarargs(
          String name,
          Class<V> of,
          Function<String, V> from,
          BiConsumer<A, V[]> to,
          String... handles) {
        return add(name, OptionType.VARARGS, handles, of, from, arrayToList(of, to), null);
      }

      @SuppressWarnings("unchecked")
      private <V> BiConsumer<A, List<V>> arrayToList(Class<V> of, BiConsumer<A, V[]> to) {
        return (state, list) ->
            to.accept(state, list.toArray(size -> (V[]) Array.newInstance(of, size)));
      }

      private <V> BiConsumer<A, List<V>> valueToList(BiConsumer<A, V> to, V defaultValue) {
        return (state, list) -> to.accept(state, list.isEmpty() ? defaultValue : list.get(0));
      }

      private <V> BiConsumer<A, List<V>> optionalToList(BiConsumer<A, Optional<V>> to) {
        return (state, list) ->
            to.accept(state, list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
      }

      private static void checkNoHandleCollisions(List<? extends Option> options) {
        for (int i = 0; i < options.size(); i++) {
          var a = options.get(i);
          for (int j = i + 1; j < options.size(); j++) {
            var b = options.get(j);
            if (a.handles().stream().anyMatch(handle -> b.handles().contains(handle))) {
              throw new IllegalArgumentException(
                  "options "
                      + a.name()
                      + " and "
                      + b.name()
                      + " both declares handle(s) "
                      + new ArrayList<>(a.handles()).retainAll(b.handles()));
            }
          }
        }
      }

      private static void checkVarargs(List<? extends Option> options) {
        var varargs = options.stream().filter(opt -> opt.type().isVarargs()).toList();
        if (varargs.isEmpty()) return;
        if (varargs.size() > 1)
          throw new IllegalArgumentException("Too many varargs types specified: " + varargs);
        var positionals = options.stream().filter(opt -> opt.type().isPositional()).toList();
        if (!positionals.get(positionals.size() - 1).type().isVarargs())
          throw new IllegalArgumentException(
              "varargs is not at last positional option: " + options);
      }

      private record OptionValue<A, T>(
          String name,
          OptionType type,
          List<String> handles,
          Class<T> of,
          Function<String, T> from,
          BiConsumer<A, List<T>> to,
          Optional<Factory<T>> sub,
          List<T> values)
          implements Option {

        public OptionValue {
          requireNonNull(name, "name is null");
          requireNonNull(type, "type is null");
          requireNonNull(handles, "handles is null");
          requireNonNull(of, "of is null");
          requireNonNull(from, "from is null");
          if (handles.isEmpty() && !type.isPositional())
            throw new IllegalArgumentException(
                "Option of type " + type + " must have at least one handle");
        }

        @Override
        public void add(String value) {
          values.add(from.apply(value));
        }

        void complete(A target) {
          to.accept(target, values);
        }

        OptionValue<A, T> empty() {
          ArrayList<T> copy = new ArrayList<>();
          Optional<Factory<T>> linkedSub = sub.map(factory -> link(factory, copy));
          return new OptionValue<>(name, type, handles, of, from, to, linkedSub, copy);
        }

        /**
         * Links sub-command results, so they automatically end up in the values once they are
         * completed. This allows to not have an additional method in the public API which accepts
         * non {@link String} results.
         */
        private Factory<T> link(Factory<T> factory, List<T> values) {
          return () ->
              new CommandLine<>() {
                CommandLine<T> cmd = factory.create();

                @Override
                public List<? extends Option> options() {
                  return cmd.options();
                }

                @Override
                public T complete() {
                  T value = cmd.complete();
                  values.add(of.cast(value));
                  return value;
                }
              };
        }
      }
    }
  }

  static class FactorySupport {
    private FactorySupport() {
      throw new AssertionError();
    }

    static <T> CommandLine.Factory<T> factory(Lookup lookup, Class<T> schema) {
      requireNonNull(lookup, "lookup is null");
      requireNonNull(schema, "schema is null");

      if (schema.isRecord()) return recordFactory(lookup, schema);
      if (schema.isInterface()) return proxyFactory(lookup, schema);
      return pojoFactory(lookup, schema);
    }

    /*
    Using Java POJOs as target types (requires getter/setters)
     */

    private static <T> CommandLine.Factory<T> pojoFactory(Lookup lookup, Class<T> schema) {
      Predicate<Field> filter = m -> !m.isSynthetic() && !Modifier.isStatic(m.getModifiers());
      // FIXME fields are not order as declared
      List<Field> properties = Stream.of(schema.getDeclaredFields()).filter(filter).toList();
      CommandLine.Builder<Object[], T> cmd =
          CommandLine.builder(
              () -> new Object[properties.size()],
              values -> newPojo(lookup, schema, properties, values));
      for (int i = 0; i < properties.size(); i++) {
        int index = i;
        Field f = properties.get(index);
        BiConsumer<Object[], Object> to = (values, value) -> values[index] = value;
        cmd = addOption(lookup, cmd, f, f.getName(), f.getType(), f.getGenericType(), to);
      }
      return cmd.build();
    }

    private static <T> T newPojo(
        Lookup lookup, Class<T> schema, List<Field> properties, Object[] values) {
      {
        try {
          MethodHandle noArgsConstructor = lookup.unreflectConstructor(schema.getConstructor());
          T target = schema.cast(noArgsConstructor.invoke());
          for (int i = 0; i < properties.size(); i++) {
            MethodHandle setter = lookup.unreflectSetter(properties.get(i));
            setter.invoke(target, values[i]);
          }
          return target;
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      }
    }

    /*
    Using Java Records as target types
     */

    private static <T> CommandLine.Factory<T> recordFactory(Lookup lookup, Class<T> schema) {
      RecordComponent[] components = schema.getRecordComponents();
      CommandLine.Builder<Object[], T> cmd =
          CommandLine.builder(
              () -> new Object[components.length], values -> createRecord(schema, values, lookup));
      for (int i = 0; i < components.length; i++) {
        int index = i;
        RecordComponent cmp = components[index];
        BiConsumer<Object[], Object> to = (values, value) -> values[index] = value;
        cmd = addOption(lookup, cmd, cmp, cmp.getName(), cmp.getType(), cmp.getGenericType(), to);
      }
      return cmd.build();
    }

    /*
    Using Java Proxies as target types
     */

    private static <T> CommandLine.Factory<T> proxyFactory(Lookup lookup, Class<T> schema) {
      Method[] methods = schema.getMethods();
      // FIXME methods are not order as declared
      CommandLine.Builder<Object[], T> cmd =
          CommandLine.builder(
              () -> new Object[methods.length],
              values -> newProxy(schema, List.of(methods), values));
      for (int i = 0; i < methods.length; i++) {
        Method m = methods[i];
        if (!m.isSynthetic() && !Modifier.isStatic(m.getModifiers())) {
          int index = i;
          BiConsumer<Object[], Object> to = (values, value) -> values[index] = value;
          cmd =
              addOption(
                  lookup, cmd, m, m.getName(), m.getReturnType(), m.getGenericReturnType(), to);
        }
      }
      return cmd.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> T newProxy(Class<T> of, List<Method> methods, Object[] values) {
      return (T)
          Proxy.newProxyInstance(
              of.getClassLoader(),
              new Class<?>[] {of},
              (proxy, method, args) -> values[methods.indexOf(method)]);
    }

    /*
    Shared methods...
     */

    @SuppressWarnings("unchecked")
    private static <T> CommandLine.Builder<Object[], T> addOption(
        Lookup lookup,
        CommandLine.Builder<Object[], T> builder,
        AnnotatedElement source,
        String name,
        Class<?> type,
        Type genericType,
        BiConsumer<Object[], Object> to) {
      var optionType = OptionType.of(type);

      var handlesSource = source.getAnnotation(Name.class);
      var handles =
          handlesSource != null
              ? handlesSource.value()
              : optionType.isPositional() && !name.startsWith("-")
                  ? new String[0]
                  : new String[] {name.replace('_', '-')};
      var subCommandType = toSubCommandType(type, genericType);
      var valueType = valueTypeFrom(type, genericType);
      @SuppressWarnings("rawtypes")
      CommandLine.Factory subCommand =
          subCommandType == null ? null : factory(lookup, subCommandType);
      return addOption(lookup, builder, name, optionType, handles, valueType, to, subCommand);
    }

    private static <T, V> CommandLine.Builder<Object[], T> addOption(
        Lookup lookup,
        CommandLine.Builder<Object[], T> builder,
        String name,
        OptionType type,
        String[] names,
        Class<V> of,
        BiConsumer<Object[], Object> to,
        CommandLine.Factory<V> subCommand) {
      var valueConverter = valueConverter(lookup, of);
      return switch (type) {
        case SUB -> builder.addSub(name, of, to::accept, subCommand, names);
        case FLAG -> builder.addFlag(name, to::accept, names);
        case OPTIONAL -> subCommand == null
            ? builder.addOptional(name, of, valueConverter, to::accept, names)
            : builder.addOptional(name, of, to::accept, subCommand, names);
        case REPEATABLE -> subCommand == null
            ? builder.addRepeatable(name, of, valueConverter, to::accept, names)
            : builder.addRepeatable(name, of, to::accept, subCommand, names);
        case REQUIRED -> builder.addRequired(name, of, valueConverter, to::accept, names);
        case VARARGS -> builder.addVarargs(name, of, valueConverter, to::accept, names);
      };
    }

    private static Class<?> valueTypeFrom(Class<?> type, Type genericType) {
      if (type.isRecord()) return type;
      if (type == Boolean.class || type == boolean.class) return Boolean.class;
      if (type.isArray()) return type.getComponentType();
      if (type == List.class || type == Optional.class) {
        return (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
      }
      return type;
    }

    private static Class<? extends Record> toSubCommandType(Class<?> type, Type genericType) {
      if (type.isRecord()) return type.asSubclass(Record.class);
      return (genericType instanceof ParameterizedType paramType
              && paramType.getActualTypeArguments()[0] instanceof Class<?> nestedType
              && nestedType.isRecord())
          ? nestedType.asSubclass(Record.class)
          : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<String, T> valueConverter(Lookup lookup, Class<T> of) {
      if (of == String.class || of.isRecord()) return (Function) Function.identity();
      MethodHandle mh = valueOfMethod(lookup, of);
      return arg -> {
        try {
          return of.cast(mh.invoke(arg));
        } catch (Throwable e) {
          throw new IllegalArgumentException(
              format("Not a valid value for type %s: %s", of.getSimpleName(), arg), e);
        }
      };
    }

    private static <T> MethodHandle valueOfMethod(Lookup lookup, Class<T> valueType) {
      record Factory(String name, MethodType method) {}
      List<Factory> factories =
          List.of( //
              new Factory("valueOf", MethodType.methodType(valueType, String.class)), //
              new Factory("of", MethodType.methodType(valueType, String.class)), //
              new Factory("of", MethodType.methodType(valueType, String.class, String[].class)), //
              new Factory("parse", MethodType.methodType(valueType, String.class)),
              new Factory("parse", MethodType.methodType(valueType, CharSequence.class)));
      for (Factory factory : factories) {
        try {
          return lookup.findStatic(valueType, factory.name(), factory.method());
        } catch (NoSuchMethodException | IllegalAccessException e) {
          // try next
        }
      }
      throw new UnsupportedOperationException("Unsupported conversion from String to " + valueType);
    }

    private static MethodHandle constructor(Lookup lookup, Class<?> schema, Class<?>... types) {
      try {
        return lookup.findConstructor(schema, MethodType.methodType(void.class, types));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }

    private static <T> T createRecord(Class<T> schema, Object[] values, Lookup lookup) {
      var components = schema.getRecordComponents();
      var types = Stream.of(components).map(RecordComponent::getType).toArray(Class[]::new);
      try {
        return schema.cast(
            constructor(lookup, schema, types).asFixedArity().invokeWithArguments(values));
      } catch (RuntimeException | Error e) {
        throw e;
      } catch (Throwable e) {
        throw new UndeclaredThrowableException(e);
      }
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  public @interface Help {
    String[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.RECORD_COMPONENT, ElementType.METHOD, ElementType.FIELD})
  public @interface Name {
    String[] value();
  }

  public enum OptionType {
    /**
     * A branch to enter a sub-command, all remaining arguments are expected to belong to the
     * sub-command
     */
    SUB,
    /** An optional flag, like {@code --verbose}. */
    FLAG,
    /** An optional key-value pair, like {@code --version 47.11}. */
    OPTIONAL,
    /** A key-value that can occur zero or more times, like {@code --with alpha --with omega} */
    REPEATABLE,
    /** A required positional option */
    REQUIRED,
    /** A collection of all unhandled arguments. */
    VARARGS;

    public static OptionType of(Class<?> valueType) {
      if (valueType.isRecord()) return SUB;
      if (valueType == Boolean.class || valueType == boolean.class) return FLAG;
      if (valueType == Optional.class) return OPTIONAL;
      if (valueType == List.class) return REPEATABLE;
      if (valueType.isArray()) return VARARGS;
      return REQUIRED;
    }

    public boolean isVarargs() {
      return this == OptionType.VARARGS;
    }

    public boolean isRequired() {
      return this == OptionType.REQUIRED;
    }

    public boolean isFlag() {
      return this == OptionType.FLAG;
    }

    public boolean isPositional() {
      return this == OptionType.VARARGS || this == OptionType.REQUIRED;
    }
  }

  @FunctionalInterface
  public interface Splitter<T> {

    static <R> Splitter<R> of(Lookup lookup, Class<R> schema) {
      return of(FactorySupport.factory(lookup, schema));
    }

    static <X> Splitter<X> of(CommandLine.Factory<X> cmd) {
      Objects.requireNonNull(cmd, "schema is null");
      return args -> {
        requireNonNull(args, "args is null");
        return split(cmd, false, args.collect(toCollection(ArrayDeque::new)));
      };
    }

    T split(Stream<String> args);

    default T split(String... args) {
      requireNonNull(args, "args is null");
      return split(Stream.of(args));
    }

    default T split(List<String> args) {
      requireNonNull(args, "args is null");
      return split(args.stream());
    }

    /*
    Argument preprocessing
     */

    default Splitter<T> withEach(UnaryOperator<String> preprocessor) {
      requireNonNull(preprocessor, "preprocessor is null");
      return args -> split(args.map(preprocessor));
    }

    default Splitter<T> withExpand(
        Function<? super String, ? extends Stream<String>> preprocessor) {
      requireNonNull(preprocessor, "preprocessor is null");
      return args -> split(args.flatMap(preprocessor));
    }

    default Splitter<T> withAdjust(UnaryOperator<Stream<String>> preprocessor) {
      requireNonNull(preprocessor, "preprocessor is null");
      return args -> split(preprocessor.apply(args));
    }

    default Splitter<T> withSplitAssignment() {
      Pattern assign = Pattern.compile("^([-\\w]+)=(.*)$");
      return withExpand(
          arg -> {
            Matcher m = assign.matcher(arg);
            return m.matches() ? Stream.of(m.group(1), m.group(2)) : Stream.of(arg);
          });
    }

    default Splitter<T> withRemoveQuotes() {
      BiPredicate<String, Character> startsAndEndsWith =
          (str, c) -> str.charAt(0) == c && str.charAt(str.length() - 1) == c;
      return withEach(
          arg ->
              startsAndEndsWith.test(arg, '"') || startsAndEndsWith.test(arg, '\'')
                  ? arg.substring(1, arg.length() - 1)
                  : arg);
    }

    /*
    Implementation
     */

    private static <X> X split(
        CommandLine.Factory<X> cmd, boolean nested, Deque<String> remainingArgs) {
      var res = cmd.create();
      var options = res.options();
      var requiredOptions =
          res.options(OptionType::isRequired).collect(toCollection(ArrayDeque::new));
      var optionsByHandle = new HashMap<String, CommandLine.Option>();
      options.forEach(opt -> opt.handles().forEach(handle -> optionsByHandle.put(handle, opt)));
      var flagCount = res.options(OptionType::isFlag).count();
      var flagPattern = flagCount == 0 ? null : Pattern.compile("^-[a-zA-Z]{1," + flagCount + "}$");

      boolean doubleDashMode = false;
      while (true) {
        if (remainingArgs.isEmpty()) {
          if (requiredOptions.isEmpty()) return res.complete();
          throw new IllegalArgumentException("Required option(s) missing: " + requiredOptions);
        }
        // acquire next argument
        var handle = remainingArgs.removeFirst();
        if ("--".equals(handle)) {
          doubleDashMode = true;
          continue;
        }
        // try well-known option first
        if (!doubleDashMode && optionsByHandle.containsKey(handle)) {
          var option = optionsByHandle.get(handle);
          var name = option.name();
          if (option.type() == OptionType.SUB) {
            split(option, remainingArgs);
            if (!remainingArgs.isEmpty())
              throw new IllegalArgumentException("Too many arguments: " + remainingArgs);
            return res.complete();
          }
          switch (option.type()) {
            case FLAG -> {
              var maybeValue = remainingArgs.peekFirst();
              if ("true".equals(maybeValue) || "false".equals(maybeValue))
                option.add(remainingArgs.pop());
              else option.add("true");
            }
            case OPTIONAL -> {
              if (option.sub().isPresent()) split(option, remainingArgs);
              else option.add(remainingArgs.pop());
            }
            case REQUIRED -> {
              requiredOptions.remove(option);
              option.add(remainingArgs.pop());
            }
            case REPEATABLE -> {
              if (option.sub().isPresent()) split(option, remainingArgs);
              else Stream.of(remainingArgs.pop().split(",")).forEach(option::add);
            }
            default -> throw new AssertionError("Unnamed name? " + name);
          }
          continue; // with next argument
        }
        // maybe a combination of single letter flags?
        if (!doubleDashMode && flagPattern != null && flagPattern.matcher(handle).matches()) {
          var flags = handle.substring(1).chars().mapToObj(c -> "-" + (char) c).toList();
          if (flags.stream().allMatch(optionsByHandle::containsKey)) {
            flags.forEach(flag -> optionsByHandle.get(flag).add("true"));
            continue;
          }
        }
        // try required option
        if (!requiredOptions.isEmpty()) {
          requiredOptions.pop().add(handle);
          continue;
        }
        // restore pending arguments deque
        remainingArgs.addFirst(handle);
        if (nested) return res.complete();
        // try globbing all pending arguments into a varargs collector
        var varargsOption = res.options(OptionType::isVarargs).findFirst().orElse(null);
        if (varargsOption != null) {
          remainingArgs.forEach(varargsOption::add);
          remainingArgs.clear();
          return res.complete();
        }
        throw new IllegalArgumentException("Unhandled arguments: " + remainingArgs);
      }
    }

    private static void split(CommandLine.Option option, Deque<String> remainingArgs) {
      split(option.sub().orElseThrow(), option.type() != OptionType.SUB, remainingArgs);
    }
  }

  private CommandLineInterface() {
    throw new AssertionError();
  }
}
