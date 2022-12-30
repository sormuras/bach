// Generated on 2022-12-30T07:08:20.910024+01:00[Europe/Berlin]
package run.duke;

import static java.lang.Boolean.parseBoolean;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;

import java.io.Serial;
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
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public interface CommandLineInterface {

  abstract sealed class AbstractOption<T> implements Option<T>
      permits Option.Branch,
          Option.Flag,
          Option.Single,
          Option.Repeatable,
          Option.Required,
          Option.Varargs {
    final OptionType type;
    final Set<String> names;
    final String help;
    final Schema<?> nestedSchema;

    AbstractOption(OptionType type, Set<String> names, String help, Schema<?> nestedSchema) {
      requireNonNull(type, "type is null");
      requireNonNull(names, "names is null");
      requireNonNull(help, "help null");
      this.type = type;
      this.names = NameSet.copyOf(names);
      this.help = help;
      this.nestedSchema = nestedSchema;
    }

    @Override
    public final OptionType type() {
      return type;
    }

    @Override
    public final Set<String> names() {
      return names;
    }

    @Override
    public final String help() {
      return help;
    }

    @Override
    public final Schema<?> nestedSchema() {
      return nestedSchema;
    }

    @Override
    public final String toString() {
      return type + names.toString();
    }

    // helper methods

    static Option<?> newOption(
        OptionType type,
        String[] names,
        Function<Object, ?> converter,
        String help,
        Schema<?> schema) {
      var nameSet = NameSet.of(names);
      return switch (type) {
        case BRANCH -> new Branch<>(nameSet, v -> converter.apply(v), help, schema);
        case FLAG -> new Flag(nameSet, v -> (Boolean) converter.apply(v), help);
        case SINGLE -> new Single<>(nameSet, v -> (Optional<?>) converter.apply(v), help, schema);
        case REPEATABLE -> new Repeatable<>(
            nameSet, v -> (List<?>) converter.apply(v), help, schema);
        case REQUIRED -> new Required<>(nameSet, converter, help);
        case VARARGS -> new Varargs<>(nameSet, v -> (Object[]) converter.apply(v), help);
      };
    }

    @SuppressWarnings("unchecked")
    static <T> T defaultValue(Option<T> option) {
      return (T) option.type().defaultValue;
    }

    @SuppressWarnings("unchecked")
    static <T> T applyConverter(Option<T> option, Object arg) {
      if (option instanceof Option.Branch<T> branch) {
        return branch.converter.apply((T) arg);
      }
      if (option instanceof Option.Flag flag) {
        return (T) flag.converter.apply((Boolean) arg);
      }
      if (option instanceof Option.Single<?> single) {
        return (T) single.converter.apply((Optional<String>) arg);
      }
      if (option instanceof Option.Repeatable<?> repeatable) {
        return (T) repeatable.converter.apply((List<String>) arg);
      }
      if (option instanceof Option.Required<T> required) {
        return required.converter.apply((String) arg);
      }
      if (option instanceof Option.Varargs<?> varargs) {
        return (T) varargs.converter.apply((String[]) arg);
      }
      throw new AssertionError();
    }

    static String name(Option<?> option) {
      return option.names().iterator().next();
    }

    static boolean isVarargs(Option<?> option) {
      return option instanceof Option.Varargs<?>;
    }

    static boolean isRequired(Option<?> option) {
      return option instanceof Option.Required<?>;
    }

    static boolean isFlag(Option<?> option) {
      return option instanceof Option.Flag;
    }

    static boolean isPositional(Option<?> option) {
      return option instanceof Option.Required<?> || option instanceof Option.Varargs<?>;
    }
  }

  /**
   * An immutable associative table that links an {@link Option} to its value (an argument).
   *
   * <p>Calling {@link Splitter#of(Option[])} with some options returns a splitter of type
   * Splitter&lt;ArgumentMap&gt; configured with the options. On this splitter, calling {@link
   * Splitter#split(Stream)} with a stream of the command line arguments returns an argument map
   * containing the value (the argument) of each option.
   *
   * <pre>
   *   var flagF = Option.flag("-f");
   *   var splitter = Splitter.of(flagFJ);
   *   var argumentMap = splitter.split("-f");
   *
   *   var flagFValue = argumentMap.argument(flagF);
   *   System.out.println(flagFValue);  // true
   *   </pre>
   */
  public final class ArgumentMap {
    private final LinkedHashMap<Option<?>, Object> argumentMap;

    private ArgumentMap(LinkedHashMap<Option<?>, Object> argumentMap) {
      this.argumentMap = argumentMap;
    }

    /**
     * Returns the value (the argument) of an option.
     *
     * @param option the option from which we need the corresponding argument
     * @return the argument of the option.
     * @param <T> the type of the argument
     * @throws IllegalStateException if the option has no argument because the {@link Splitter} that
     *     generates this map was not configured with that option.
     */
    @SuppressWarnings("unchecked")
    public <T> T argument(Option<T> option) {
      Objects.requireNonNull(option, "option is null");
      var argument = argumentMap.get(option);
      if (argument == null) {
        throw new IllegalStateException("no argument for option " + this);
      }
      return (T) argument;
    }

    // TODO add more methods
    // because we ask in Option.map() to produce a value of an equivalent type
    // we know that we have records, boolean, Optional, List, array or any other values

    /**
     * Returns a string representation the arguments associated with each option.
     *
     * @return a string representation the arguments associated with each option.
     */
    @Override
    public String toString() {
      return argumentMap.toString();
    }

    static Schema<ArgumentMap> toSchema(Option<?>... options) {
      var opt = List.of(options);
      return new Schema<>(
          opt,
          data ->
              new ArgumentMap(
                  range(0, opt.size())
                      .boxed()
                      .collect(
                          toMap(
                              opt::get,
                              data::get,
                              (_1, _2) -> {
                                throw null;
                              },
                              LinkedHashMap::new))));
    }
  }

  /**
   * Find a converter (a conversion function) that converts a String/boolean/record to a Java
   * generic type.
   *
   * <p>This API proposes a toolkit to build your own revolver by composition.
   *
   * <p>For example, the default resolver is defined as such
   *
   * <pre>
   *   ConverterResolver defaultResolver =
   *       ConverterResolver.of(ConverterResolver::basic)
   *           .or(ConverterResolver::enumerated)
   *           .or(ConverterResolver::reflected)
   *           .unwrap();
   * </pre>
   *
   * {@code ConverterResolver::base} is a resolver that associate the identity for the base types,
   * String, boolean, Boolean and records.
   *
   * <p>{@code ConverterResolver::enumerated} is a resolver that calls {@code Enum.valueOf} if the
   * type is an enum
   *
   * <p>{@code ConverterResolver::reflected} is a resolver that tries to find a static method
   * factory to convert a type from String inside the type.
   *
   * <p>{@code or(resolver)} executes the first resolver and if there is no converter found,
   * executes the second converter.
   *
   * <p>{@code unwrap()} provides a resolver that unwrap Optional, List and array and executes the
   * resolver on the component type.
   *
   * <p>A converter resolver is used by {@link Splitter#of(Lookup, Class, ConverterResolver)} that
   * specify how the value of an option is converted to the type of the corresponding record
   * component.
   */
  @FunctionalInterface
  public interface ConverterResolver {
    /**
     * A class used to send a generic Java {@link Type} to a converter resolver.
     *
     * @param <R> the Java type.
     * @see #resolve(Lookup, TypeReference)
     */
    interface TypeReference<R> {
      private Type extract() {
        var genericInterfaces = getClass().getGenericInterfaces();
        if (genericInterfaces.length == 1
            && genericInterfaces[0] instanceof ParameterizedType parameterizedType
            && parameterizedType.getRawType() == TypeReference.class) {
          return parameterizedType.getActualTypeArguments()[0];
        }
        throw new IllegalStateException(
            "the TypeReference is malformed " + Arrays.toString(genericInterfaces));
      }
    }

    /**
     * Returns a converter (a conversion function) for a Java type.
     *
     * @param lookup the lookup used if reflection is involved to find the converter.
     * @param type a generic java type
     * @return a converter (a conversion function) for a specific type or Optional.empty() if no
     *     converter is found.
     */
    Optional<Function<Object, ?>> resolve(Lookup lookup, Type type);

    /**
     * Returns a converter (a conversion function) for a Java type specified as type argument of a
     * type reference.
     *
     * @param lookup the lookup used if reflection is involved to find the converter.
     * @param typeReference the type reference used to extract the Java generic type from.
     * @return a converter (a conversion function) for a Java type specified as type argument of a
     *     type reference.
     * @param <R> the generic Java type.
     */
    @SuppressWarnings("unchecked")
    default <R> Optional<Function<Object, R>> resolve(
        Lookup lookup, TypeReference<R> typeReference) {
      Objects.requireNonNull(lookup, "lookup is null");
      Objects.requireNonNull(typeReference, "typeReference is null");
      return (Optional<Function<Object, R>>)
          (Optional<? extends Function<?, ?>>) resolve(lookup, typeReference.extract());
    }

    /**
     * Returns a resolver that will first try to find the converter (a conversion function) from the
     * current resolver and if not available from the resolver taken as parameter.
     *
     * @param resolver a second resolver.
     * @return a new resolver that will resolve using the current resolver and the second resolver
     *     otherwise.
     */
    default ConverterResolver or(ConverterResolver resolver) {
      requireNonNull(resolver, "resolver is null");
      return (lookup, valueType) ->
          resolve(lookup, valueType).or(() -> resolver.resolve(lookup, valueType));
    }

    /**
     * Returns a new resolver that will unwrap Optional, List or array and calls the current
     * resolver with the component type.
     *
     * @return a new resolver that will unwrap Optional, List or array and calls the current
     *     resolver with the component type.
     */
    default ConverterResolver unwrap() {
      return (lookup, valueType) -> unwrap(lookup, valueType, this);
    }

    /**
     * Returns the resolver taken as parameter. This allows to type a lambda from right to left.
     *
     * <p>This does not compile
     *
     * <pre>
     *   var resolver = (lookup, valueType) ->  ....
     * </pre>
     *
     * But this does
     *
     * <pre>
     *   var resolver = ConverterResolver.Of((lookup, valueType) -> ...);
     * </pre>
     *
     * @param resolver the resolver.
     * @return the resolver taken as parameter.
     */
    static ConverterResolver of(ConverterResolver resolver) {
      requireNonNull(resolver, "resolver is null");
      return resolver;
    }

    /**
     * Returns a resolver that will call the converter if the predicate is true.
     *
     * @param predicate a predicate on the valueType (as a {@code java.lang.Class})
     * @param converter the converter to call
     * @return a resolver that will call the converter if the predicate is true.
     */
    static ConverterResolver when(
        Predicate<? super Class<?>> predicate, Function<Object, ?> converter) {
      requireNonNull(predicate, "predicate is null");
      requireNonNull(converter, "converter is null");
      return (lookup, valueType) ->
          Optional.<Function<Object, ?>>of(converter)
              .filter(__ -> valueType instanceof Class<?> clazz && predicate.test(clazz));
    }

    /**
     * Returns a resolver that will call the converter if the valueType is the one taken as
     * parameter.
     *
     * <p>The implementation is equivalent to
     *
     * <pre>
     *   when(valueType::equals, converter)
     * </pre>
     *
     * @param valueType the value type to check
     * @param converter the converter to call
     * @return a resolver that will call the converter if the valueType is the one taken as
     *     parameter.
     */
    static ConverterResolver when(Class<?> valueType, Function<Object, ?> converter) {
      requireNonNull(valueType, "valueType is null");
      requireNonNull(converter, "converter is null");
      return when(valueType::equals, converter);
    }

    /**
     * Return the default resolver. The default resolver first try to unwrap Optional, Liat or any
     * object array then calls {@link #basic(Lookup, Type)}, {@link #enumerated(Lookup, Type)} and
     * then {@link #reflected(Lookup, Type)}.
     *
     * <p>The implementation is equivalent to
     *
     * <pre>
     *   ConverterResolver.of(ConverterResolver::basic)
     *       .or(ConverterResolver::enumerated)
     *       .or(ConverterResolver::reflected)
     *       .unwrap();
     * </pre>
     *
     * @return the default resolver.
     */
    static ConverterResolver defaultResolver() {
      final class Default {
        private static final ConverterResolver DEFAULT_RESOLVER =
            of(ConverterResolver::basic)
                .or(ConverterResolver::enumerated)
                .or(ConverterResolver::reflected)
                .unwrap();
      }
      return Default.DEFAULT_RESOLVER;
    }

    /**
     * Returns a new converter from a conversion function typed as {@code type}.
     *
     * @param converter a conversion function
     * @param type the type of the parameter of the conversion function.
     * @return a new converter from a conversion function typed as {@code type}.
     * @param <T> type of the conversion function parameter.
     */
    static <T> Function<Object, ?> converter(
        Function<? super T, ?> converter, Class<? extends T> type) {
      requireNonNull(converter, "converter is null");
      requireNonNull(type, "type is null");
      return converter.compose(type::cast);
    }

    /**
     * Returns a new converter from a conversion function typed as String.
     *
     * <p>This implementation is equivalent to
     *
     * <pre>
     *   converter(converter, String.class)
     * </pre>
     *
     * @param converter a conversion function.
     * @return a new converter from a conversion function typed as String..
     */
    static Function<Object, ?> stringConverter(Function<? super String, ?> converter) {
      requireNonNull(converter, "converter is null");
      return converter(converter, String.class);
    }

    private static Function<Object, ?> converter(MethodHandle mh) {
      Objects.requireNonNull(mh, "mh is null");
      return arg -> {
        try {
          return mh.invoke(arg);
        } catch (RuntimeException | Error e) {
          throw e;
        } catch (Throwable e) {
          throw new UndeclaredThrowableException(e);
        }
      };
    }

    private static Optional<Function<Object, ?>> unwrap(
        Lookup lookup, Type valueType, ConverterResolver resolver) {
      requireNonNull(lookup, "lookup is null");
      requireNonNull(valueType, "valueType is null");
      requireNonNull(resolver, "resolver is null");
      if (valueType instanceof ParameterizedType parameterizedType) {
        var raw = (Class<?>) parameterizedType.getRawType();
        if (raw == Optional.class) {
          var actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
          return resolver
              .resolve(lookup, actualTypeArgument)
              .map(f -> arg -> ((Optional<?>) arg).map(f));
        }
        if (raw == List.class) {
          var actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
          return resolver
              .resolve(lookup, actualTypeArgument)
              .map(f -> arg -> ((List<?>) arg).stream().map(f).toList());
        }
      }
      if (valueType instanceof Class<?> clazz && Object[].class.isAssignableFrom(clazz)) {
        var componentType = clazz.getComponentType();
        return resolver
            .resolve(lookup, componentType)
            .map(
                f ->
                    arg ->
                        Arrays.stream(((Object[]) arg))
                            .map(f)
                            .toArray(size -> (Object[]) Array.newInstance(componentType, size)));
      }
      return resolver.resolve(lookup, valueType);
    }

    /**
     * Returns the function identity for the types String, Boolean, boolean or Record.
     *
     * @param lookup an unused lookup
     * @param valueType the type of the return type of the function
     * @return the function identity for the types String, Boolean, boolean or Record.
     */
    static Optional<Function<Object, ?>> basic(Lookup lookup, Type valueType) {
      requireNonNull(lookup, "lookup is null");
      requireNonNull(valueType, "valueType is null");
      return Optional.of(valueType)
          .flatMap(
              type -> {
                if (type instanceof Class<?> clazz) {
                  if (clazz == String.class
                      || clazz == Boolean.class
                      || clazz == boolean.class
                      || clazz.isRecord()) {
                    return Optional.of(Function.identity());
                  }
                }
                return Optional.empty();
              });
    }

    /**
     * Returns the function {@code Enum.valueOf} for any enums.
     *
     * @param lookup an unused lookup
     * @param valueType the type of the return type of the function
     * @return the function {@code Enum.valueOf} for any enums.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static Optional<Function<Object, ?>> enumerated(Lookup lookup, Type valueType) {
      requireNonNull(lookup, "lookup is null");
      requireNonNull(valueType, "valueType is null");
      if (valueType instanceof Class<?> clazz && clazz.isEnum()) {
        return Optional.of(arg -> Enum.valueOf((Class) clazz, (String) arg));
      }
      return Optional.empty();
    }

    /**
     * Returns the function that parses a String to a value type.
     *
     * <p>This implementation tries the static methods (in that order)
     *
     * <pre>
     *   valueType ValueType.valueOf(String)
     *   valueType ValueType.of(String)
     *   valueType ValueType.of(String, String...)
     *   valueType ValueType.parse(String)
     *   valueType ValueType.parse(CharSequence)
     * </pre>
     *
     * @param lookup the lookup used to try to find the functions
     * @param valueType the type of the return type of the function
     * @return the function that parses a String to a value type if available.
     */
    static Optional<Function<Object, ?>> reflected(Lookup lookup, Type valueType) {
      requireNonNull(lookup, "lookup is null");
      requireNonNull(valueType, "valueType is null");
      return Optional.of(valueType)
          .flatMap(
              type -> {
                if (valueType instanceof Class<?> clazz) {
                  return valueOfMethod(lookup, clazz).map(ConverterResolver::converter);
                }
                return Optional.empty();
              });
    }

    private static Optional<MethodHandle> valueOfMethod(Lookup lookup, Class<?> type) {
      record Factory(String name, MethodType method) {}
      var factories =
          List.of(
              new Factory("valueOf", methodType(type, String.class)),
              new Factory("of", methodType(type, String.class)),
              new Factory("of", methodType(type, String.class, String[].class)),
              new Factory("parse", methodType(type, String.class)),
              new Factory("parse", methodType(type, CharSequence.class)));
      for (var factory : factories) {
        MethodHandle mh;
        try {
          mh = lookup.findStatic(type, factory.name(), factory.method());
        } catch (NoSuchMethodException | IllegalAccessException e) {
          continue; // does not exist, try next
        }
        // we only allow X.of(String, String...) with a varargs, not X.of(String, String[])
        if (factory.name().equals("of")
            && mh.type().parameterType(mh.type().parameterCount() - 1).isArray()
            && !mh.isVarargsCollector()) {
          continue; // try next
        }
        return Optional.of(mh);
      }
      return Optional.empty();
    }
  }

  /**
   * Allow to specify {@link Option#help()} the help text} of an option defined as a record
   * component.
   *
   * <p>This annotation allows to specify the help text as a string
   *
   * <pre>
   *  record Command (
   *    &#064;Help("this is an hep text")
   *    boolean verbose
   *  }
   * </pre>
   *
   * or using several strings that will be joined using a space.
   *
   * <pre>
   *  record Command (
   *    &#064;Help({ "this is an hep text", "on several parts"})
   *    boolean verbose
   *  }
   * </pre>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  public @interface Help {
    /**
     * Returns the option help texts.
     *
     * @return the option help texts.
     */
    String[] value();
  }

  /** Helper class that generate the help associated to a command line arguments. */
  public class Manual {

    private Manual() {
      throw new AssertionError();
    }

    /**
     * Generate the help of a command line application. This is a convenient method equivalent to
     *
     * <pre>
     *   help(schema, 2)
     * </pre>
     *
     * @param schema the schema of the command line.
     * @return a string describing each argument of the command line.
     */
    public static String help(Schema<?> schema) {
      return help(schema, 2);
    }

    /**
     * Generate the help of a command line application.
     *
     * @param schema the schema of the command line.
     * @param indent the number of spaces when indenting.
     * @return a string describing each argument of the command line.
     */
    public static String help(Schema<?> schema, int indent) {
      requireNonNull(schema, "schema is null");
      if (indent < 0) throw new IllegalArgumentException("invalid indent " + indent);
      var joiner = new StringJoiner("\n");
      for (var option :
          schema.options.stream().sorted(Comparator.comparing(AbstractOption::name)).toList()) {
        var text = option.help();
        if (text.isEmpty()) continue;
        var suffix =
            switch (option.type()) {
              case BRANCH -> " (branch)";
              case FLAG -> " (flag)";
              case SINGLE -> " <value>";
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

  /**
   * Allow to specify {@link Option#names() the names} of an option defined as a record component.
   *
   * <p>By default, the name of the option is derived from the name of the record component, with
   * the '_' replaced by '-'.
   *
   * <pre>
   *  record Command (
   *    boolean __verbose
   *  }
   *  </pre>
   *
   * This annotation allows to specify a name thos is not a vid Java identifier
   *
   * <pre>
   *  record Command (
   *    &#064;Name("-v")
   *    boolean verbose
   *  }
   * </pre>
   *
   * or several names
   *
   * <pre>
   *  record Command (
   *    &#064;Name({ "-v", "--verbose" })
   *    boolean verbose
   *  }
   * </pre>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.RECORD_COMPONENT)
  public @interface Name {
    /**
     * Returns the option names.
     *
     * @return the option names.
     */
    String[] value();
  }

  final class NameSet extends AbstractSet<String> {
    private final LinkedHashSet<String> set;

    private NameSet(LinkedHashSet<String> set) {
      this.set = set;
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public Iterator<String> iterator() {
      var iterator = set.iterator();
      return new Iterator<>() {
        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public String next() {
          return iterator.next();
        }
      };
    }

    @Override
    public boolean contains(Object o) {
      requireNonNull(o, "o is null");
      return set.contains(o);
    }

    @Override
    public Spliterator<String> spliterator() {
      return Spliterators.spliterator(
          this, Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.NONNULL);
    }

    public static Set<String> of(String... names) {
      requireNonNull(names, "names is null");
      return copyOf(Arrays.asList(names));
    }

    public static Set<String> copyOf(Collection<? extends String> names) {
      requireNonNull(names, "names is null");
      if (names instanceof NameSet nameSet) {
        return nameSet;
      }
      if (names.isEmpty()) {
        throw new IllegalArgumentException("names is empty");
      }
      var set = new LinkedHashSet<String>();
      for (var name : names) {
        requireNonNull(name, "name is null");
        if (name.isEmpty()) {
          throw new IllegalArgumentException("one name is empty");
        }
        if (!set.add(name)) {
          throw new IllegalArgumentException("duplicate names " + name);
        }
      }
      return new NameSet(set);
    }
  }

  /**
   * An Option is a description of one of more arguments of the command line. Option are immutable
   * classes and can be shared between several {@link Schema schemas}.
   *
   * <p>There two kinds of options, optional options with three sub-categories {@link
   * #flag(String...) FLAG}, {@link #single(String...) SINGLE} and {@link #repeatable(String...)
   * REPETABLE} and positional options with two sub-categories {@link #required(String...) REQUIRED}
   * and {@link #varargs(String...) VARARGS}.
   *
   * <p>&nbsp;
   *
   * <table>
   *   <caption>Overview of the different options</caption>
   *   <tr>
   *     <th>Option type</th><th>positional</th><th>arguments ?</th><th>Java type</th><th>default value</th>
   *   </tr><tr>
   *     <td>FLAG</td><td>no</td><td>no argument</td><td>boolean or Boolean</td><td>false</td>
   *  </tr><tr>
   *     <td>SINGLE</td><td>no</td><td>one argument</td><td>java.util.Optional</td><td>Optional.&lt;String&gt;empty()</td>
   *  </tr><tr>
   *     <td>REPEATABLE</td><td>no</td><td>many arguments</td><td>java.util.List</td><td>List.&lt;String&gt;of()</td>
   *  </tr><tr>
   *    <td>REQUIRED</td><td>yes</td><td>an argument</td><td>any Object</td><td>null</td>
   *  </tr><tr>
   *    <td>VARARGS</td><td>yes</td><td>many arguments</td><td>any object array</td><td>new String[0]</td>
   *  </tr>
   * </table>
   *
   * <p>Options are created using factory methods, {@link #flag(String...)}, {@link
   * #single(String...)}, {@link #repeatable(String...)}, {@link #required(String...)} or {@link
   * #varargs(String...)}.
   *
   * <p>Options have a {@link #type() type}, case-sensitive {@link #names() names} and optionally a
   * {@link #help() help text}, a {@link #defaultValue(Object) default value} and {@link
   * #nestedSchema() a nested schema}.
   *
   * <p>The argument(s) of an option can be converted using Options specific {@code convert()}
   * methods, {@link Flag#convert(UnaryOperator)}, {@link Single#convert(Function)}, {@link
   * Repeatable#convert(Function)}, {@link Required#convert(Function)} and {@link
   * Varargs#convert(Function, IntFunction)}.
   *
   * <p>Options are grouped into a {@link Schema#Schema(List, Function) schema} that is used to
   * create a {@link Splitter#of(Schema) Splitter} that parses the command line. The value of
   * argument(s) of an option are retrieved using {@link #argument(ArgumentMap)}.
   *
   * @param <T> type of the argument(s) described by this Option.
   */
  public sealed interface Option<T> permits AbstractOption {

    /**
     * An option that branch to a nested schema.
     *
     * @param <T> the type of the argument corresponding to that option.
     */
    final class Branch<T> extends AbstractOption<T> {
      final UnaryOperator<T> converter;

      Branch(Set<String> names, UnaryOperator<T> converter, String help, Schema<?> nestedSchema) {
        super(OptionType.BRANCH, names, help, nestedSchema);
        this.converter = requireNonNull(converter, "converter is null");
        requireNonNull(nestedSchema, "schema is null");
      }

      /**
       * Create a branch option with several names, a conversion function and a nested schema.
       *
       * @param names names of the option.
       * @param converter a conversion function.
       * @param nestedSchema the nested schema
       * @throws IllegalArgumentException if a name is empty or if names are duplicated.
       */
      public Branch(List<String> names, UnaryOperator<T> converter, Schema<T> nestedSchema) {
        this(NameSet.copyOf(names), converter, "", nestedSchema);
      }

      @Override
      public Branch<T> help(String helpText) {
        requireNonNull(helpText, "helpText is null");
        if (!help.isEmpty()) {
          throw new IllegalStateException("option already has an help text");
        }
        return new Branch<>(names, converter, helpText, nestedSchema);
      }

      /**
       * Returns a new option that converts the argument to a value of the same type.
       *
       * @param mapper the function to apply to do the conversion.
       * @return a new option that converts the argument to a value of the same type.
       */
      public Branch<T> convert(UnaryOperator<T> mapper) {
        requireNonNull(mapper, "mapper is null");
        return new Branch<>(names, v -> mapper.apply(converter.apply(v)), help, nestedSchema);
      }

      @Override
      public Branch<T> defaultValue(T value) {
        return convert(v -> v == null ? value : v);
      }
    }

    /** A boolean optional option. */
    final class Flag extends AbstractOption<Boolean> {
      final UnaryOperator<Boolean> converter;

      Flag(Set<String> names, UnaryOperator<Boolean> converter, String help) {
        super(OptionType.FLAG, names, help, null);
        this.converter = requireNonNull(converter, "converter is null");
      }

      /**
       * Create a flag option with several names, a conversion function, a help text and a nested
       * schema
       *
       * @param names names of the option.
       * @param converter a conversion function.
       * @throws IllegalArgumentException if a name is empty or if names are duplicated.
       * @see #flag(String...)
       */
      public Flag(List<String> names, UnaryOperator<Boolean> converter) {
        this(NameSet.copyOf(names), converter, "");
      }

      @Override
      public Flag help(String helpText) {
        requireNonNull(names, "helpText is null");
        if (!help.isEmpty()) {
          throw new IllegalStateException("option already has an help text");
        }
        return new Flag(names, converter, helpText);
      }

      /**
       * Returns a new option that converts the argument to a boolean value.
       *
       * @param mapper the function to apply to do the conversion.
       * @return a new option that converts the argument to a boolean value.
       */
      public Flag convert(UnaryOperator<Boolean> mapper) {
        requireNonNull(mapper, "mapper is null");
        return new Flag(names, v -> mapper.apply(converter.apply(v)), help);
      }

      @Override
      public Flag defaultValue(Boolean value) {
        requireNonNull(value, "value is null");
        return convert(v -> !v && value);
      }
    }

    /** An optional key/value option. */
    final class Single<T> extends AbstractOption<Optional<T>> {
      final Function<Optional<?>, ? extends Optional<T>> converter;

      Single(
          Set<String> names,
          Function<Optional<?>, ? extends Optional<T>> converter,
          String help,
          Schema<?> nestedSchema) {
        super(OptionType.SINGLE, names, help, nestedSchema);
        this.converter = requireNonNull(converter, "converter is null");
      }

      /**
       * Create a single option with several names, a conversion function, a help text and a nested
       * schema
       *
       * @param names names of the option.
       * @param converter a conversion function.
       * @throws IllegalArgumentException if a name is empty or if names are duplicated.
       * @see #single(String...)
       */
      @SuppressWarnings("unchecked")
      public Single(
          List<String> names, Function<? super Optional<String>, ? extends Optional<T>> converter) {
        this(
            NameSet.copyOf(names),
            (Function<Optional<?>, ? extends Optional<T>>) converter,
            "",
            null);
      }

      @Override
      public Single<T> help(String helpText) {
        requireNonNull(names, "helpText is null");
        if (!help.isEmpty()) {
          throw new IllegalStateException("option already has an help text");
        }
        return new Single<>(names, converter, helpText, nestedSchema);
      }

      /**
       * Returns a new option with the nested schema. The newly created option has no defined
       * conversion.
       *
       * @param nestedSchema the nested schema of the new option.
       * @return a new option with the nested schema.
       * @throws IllegalStateException if the option already has a nested schema set.
       */
      @SuppressWarnings("unchecked")
      public <U> Single<U> nestedSchema(Schema<U> nestedSchema) {
        requireNonNull(nestedSchema, "nestedSchema is null");
        if (this.nestedSchema != null) {
          throw new IllegalStateException("a nested schema is already set");
        }
        return new Single<>(names, opt -> (Optional<U>) opt, help, nestedSchema);
      }

      /**
       * Returns a new option that converts the argument if it exists to another value.
       *
       * @param mapper the function to apply to do the conversion.
       * @return a new option that converts the argument if it exists to another value.
       */
      public <U> Single<U> convert(Function<? super T, ? extends U> mapper) {
        requireNonNull(mapper, "mapper is null");
        return new Single<>(names, converter.andThen(v -> v.map(mapper)), help, nestedSchema);
      }

      @Override
      public Single<T> defaultValue(Optional<T> value) {
        requireNonNull(value, "value is null");
        return new Single<>(names, converter.andThen(v -> v.or(() -> value)), help, nestedSchema);
      }
    }

    /**
     * An optional repeatable key/value option.
     *
     * @param <T> the type of the argument corresponding to that option.
     */
    final class Repeatable<T> extends AbstractOption<List<T>> {
      final Function<List<?>, ? extends List<T>> converter;

      Repeatable(
          Set<String> names,
          Function<List<?>, ? extends List<T>> converter,
          String help,
          Schema<?> nestedSchema) {
        super(OptionType.REPEATABLE, names, help, nestedSchema);
        this.converter = requireNonNull(converter, "converter is null");
      }

      /**
       * Create a repeatable option with several names, a conversion function, a help text and a
       * nested schema
       *
       * @param names names of the option.
       * @param converter a conversion function.
       * @throws IllegalArgumentException if a name is empty or if names are duplicated.
       * @see #repeatable(String...)
       */
      @SuppressWarnings("unchecked")
      public Repeatable(
          List<String> names, Function<? super List<String>, ? extends List<T>> converter) {
        this(NameSet.copyOf(names), (Function<List<?>, ? extends List<T>>) converter, "", null);
      }

      @Override
      public Repeatable<T> help(String helpText) {
        requireNonNull(names, "helpText is null");
        if (!help.isEmpty()) {
          throw new IllegalStateException("option already has an help text");
        }
        return new Repeatable<>(names, converter, helpText, nestedSchema);
      }

      /**
       * Returns a new option with the nested schema. The newly created option has no defined
       * conversion.
       *
       * @param nestedSchema the nested schema of the new option.
       * @return a new option with the nested schema.
       * @throws IllegalStateException if the option already has a nested schema set.
       */
      @SuppressWarnings("unchecked")
      public <U> Repeatable<U> nestedSchema(Schema<U> nestedSchema) {
        requireNonNull(nestedSchema, "nestedSchema is null");
        if (this.nestedSchema != null) {
          throw new IllegalStateException("a nested schema is already set");
        }
        return new Repeatable<>(names, x -> (List<U>) x, help, nestedSchema);
      }

      /**
       * Returns a new option that converts each argument to another value.
       *
       * @param mapper the function to apply to do the conversion.
       * @return a new option that converts each argument to another value.
       */
      public <U> Repeatable<U> convert(Function<? super T, ? extends U> mapper) {
        requireNonNull(mapper, "mapper is null");
        return new Repeatable<>(
            names,
            converter.andThen(list -> list.stream().<U>map(mapper).toList()),
            help,
            nestedSchema);
      }

      @Override
      public Repeatable<T> defaultValue(List<T> value) {
        requireNonNull(value, "value is null");
        return new Repeatable<>(
            names, converter.andThen(list -> list.isEmpty() ? value : list), help, nestedSchema);
      }
    }

    /**
     * A required positional option.
     *
     * @param <T> the type of the argument corresponding to that option.
     */
    final class Required<T> extends AbstractOption<T> {
      final Function<? super String, ? extends T> converter;

      Required(Set<String> names, Function<? super String, ? extends T> converter, String help) {
        super(OptionType.REQUIRED, names, help, null);
        this.converter = requireNonNull(converter, "converter is null");
      }

      /**
       * Create a required option with several names, a conversion function, a help text and a
       * nested schema
       *
       * @param names names of the option.
       * @param converter a conversion function.
       * @throws IllegalArgumentException if a name is empty or if names are duplicated.
       * @see #required(String...)
       */
      public Required(List<String> names, Function<? super String, ? extends T> converter) {
        this(NameSet.copyOf(names), converter, "");
      }

      @Override
      public Required<T> help(String helpText) {
        requireNonNull(names, "helpText is null");
        if (!help.isEmpty()) {
          throw new IllegalStateException("option already has an help text");
        }
        return new Required<>(names, converter, helpText);
      }

      /**
       * Returns a new option that converts the argument to another value.
       *
       * @param mapper the function to apply to do the conversion.
       * @return a new option that converts the argument to another value.
       */
      public <U> Required<U> convert(Function<? super T, ? extends U> mapper) {
        requireNonNull(mapper, "mapper is null");
        return new Required<>(names, converter.andThen(mapper), help);
      }

      @Override
      public Required<T> defaultValue(T value) {
        return convert(v -> v == null ? value : v);
      }
    }

    /**
     * An option corresponding to the rest of the positional arguments..
     *
     * @param <T> the type of the argument corresponding to that option.
     */
    final class Varargs<T> extends AbstractOption<T[]> {
      final Function<? super String[], ? extends T[]> converter;

      Varargs(Set<String> names, Function<? super String[], ? extends T[]> converter, String help) {
        super(OptionType.VARARGS, names, help, null);
        this.converter = requireNonNull(converter, "converter is null");
      }

      /**
       * Create a varargs option with several names, a conversion function, a help text and a nested
       * schema
       *
       * @param names names of the option.
       * @param converter a conversion function.
       * @throws IllegalArgumentException if a name is empty or if names are duplicated.
       * @see #varargs(String...)
       */
      public Varargs(List<String> names, Function<? super String[], ? extends T[]> converter) {
        this(NameSet.copyOf(names), converter, "");
      }

      @Override
      public Varargs<T> help(String helpText) {
        requireNonNull(names, "helpText is null");
        if (!help.isEmpty()) {
          throw new IllegalStateException("option already has an help text");
        }
        return new Varargs<>(names, converter, helpText);
      }

      /**
       * Returns a new option that converts each argument to another value.
       *
       * @param mapper the function to apply to do the conversion.
       * @param generator an array generator
       * @return a new option that converts each argument to another value.
       */
      public <U> Varargs<U> convert(
          Function<? super T, ? extends U> mapper, IntFunction<U[]> generator) {
        requireNonNull(mapper, "mapper is null");
        return new Varargs<>(
            names, converter.andThen(v -> Arrays.stream(v).map(mapper).toArray(generator)), help);
      }

      @Override
      public Varargs<T> defaultValue(T[] value) {
        requireNonNull(value, "value is null");
        return new Varargs<>(names, converter.andThen(v -> v.length == 0 ? value : v), help);
      }
    }

    /**
     * Returns the type of the option.
     *
     * @return the type of the option.
     */
    OptionType type();

    /**
     * Returns the names of the options.
     *
     * @return the named of the options.
     */
    Set<String> names();

    /**
     * Returns the text of the help message.
     *
     * @return the text of the help message or an empty string if no text is set.
     */
    String help();

    /**
     * Returns the nested schema if it exists.
     *
     * @return the nested schema or {@code null} otherwise.
     */
    Schema<?> nestedSchema();

    /**
     * Creates an option of type {@link OptionType#FLAG} with names.
     *
     * @param names the names of the options.
     * @return a new {@link OptionType#FLAG} option.
     * @throws IllegalArgumentException is there are duplicated names.
     */
    static Flag flag(String... names) {
      return new Flag(NameSet.of(names), value -> value, "");
    }

    /**
     * Creates an option of type {@link OptionType#SINGLE} with names.
     *
     * @param names the names of the options.
     * @return a new {@link OptionType#SINGLE} option.
     * @throws IllegalArgumentException is there are duplicated names.
     */
    @SuppressWarnings("unchecked")
    static Single<String> single(String... names) {
      return new Single<>(NameSet.of(names), value -> (Optional<String>) value, "", null);
    }

    /**
     * Creates an option of type {@link OptionType#REPEATABLE} with names.
     *
     * @param names the names of the options.
     * @return a new {@link OptionType#REPEATABLE} option.
     * @throws IllegalArgumentException is there are duplicated names.
     */
    @SuppressWarnings("unchecked")
    static Repeatable<String> repeatable(String... names) {
      return new Repeatable<>(NameSet.of(names), value -> (List<String>) value, "", null);
    }

    /**
     * Creates an option of type {@link OptionType#REQUIRED} with names.
     *
     * @param names the names of the options.
     * @return a new {@link OptionType#REQUIRED} option.
     * @throws IllegalArgumentException is there are duplicated names.
     */
    static Required<String> required(String... names) {
      return new Required<>(NameSet.of(names), value -> value, "");
    }

    /**
     * Creates an option of type {@link OptionType#VARARGS} with names.
     *
     * @param names the names of the options.
     * @return a new {@link OptionType#VARARGS} option.
     * @throws IllegalArgumentException is there are duplicated names.
     */
    static Varargs<String> varargs(String... names) {
      return new Varargs<>(NameSet.of(names), value -> value, "");
    }

    /**
     * Returns a string representation of the option.
     *
     * @return a string representation of the option using its {@link #type()} and its {@link
     *     #names()}.
     */
    @Override
    String toString();

    /**
     * Returns a new option configured with a default value.
     *
     * @param value the default value
     * @return a new option configured with a default value.
     */
    Option<T> defaultValue(T value);

    /**
     * Returns a new option with the help text.
     *
     * @param helpText the help text of the new option.
     * @return a new option with the help text.
     * @throws IllegalStateException if the option already has a help text set.
     */
    Option<T> help(String helpText);

    /**
     * Returns the value of the argument associated with the current option.
     *
     * @param argumentMap an argument map produced by {@link Splitter#split(Stream)}.
     * @return the value of the argument associated with the current option.
     * @throws IllegalStateException if there is no argument value associated with the current
     *     option in the {@link ArgumentMap}.
     */
    default T argument(ArgumentMap argumentMap) {
      requireNonNull(argumentMap, "dataMap is null");
      return argumentMap.argument(this);
    }
  }

  /** Type of {@link Option}. */
  public enum OptionType {
    BRANCH(null),
    /** An optional flag, like {@code --verbose}, {@code quiet=true} or {@code quiet=false}. */
    FLAG(false),
    /** An optional key-value pair, like {@code --version 47.11} or {@code version=47.11}. */
    SINGLE(Optional.empty()),
    /**
     * An optional and repeatable key, like {@code --with alpha --with omega} or {@code
     * --with=alpha,omega}
     */
    REPEATABLE(List.of()),
    /** A required positional option */
    REQUIRED(null),
    /** An array of all unhandled arguments. */
    VARARGS(new String[0]);

    final Object defaultValue;

    OptionType(Object defaultValue) {
      this.defaultValue = defaultValue;
    }
  }

  /**
   * Uses {@link Record}s to derive a {@link Schema} from the {@link RecordComponent}s as well as
   * container for the result values.
   */
  class RecordSchemaSupport {
    private RecordSchemaSupport() {
      throw new AssertionError();
    }

    public static <T extends Record> Schema<T> toSchema(
        Lookup lookup, Class<T> schema, ConverterResolver resolver) {
      return new Schema<>(
          Stream.of(schema.getRecordComponents())
              .map(component -> toOption(lookup, component, resolver))
              .toList(),
          values -> createRecord(schema, values, lookup));
    }

    private static Option<?> toOption(
        Lookup lookup, RecordComponent component, ConverterResolver resolver) {
      var nameAnno = component.getAnnotation(Name.class);
      var helpAnno = component.getAnnotation(Help.class);
      var names =
          nameAnno != null
              ? nameAnno.value()
              : new String[] {component.getName().replace('_', '-')};
      var type = optionTypeFrom(component.getType());
      var help = helpAnno != null ? String.join("\n", helpAnno.value()) : "";
      var nestedSchema = toNestedSchema(component);
      var converter = resolveConverter(lookup, component, resolver);
      var optionSchema = nestedSchema == null ? null : toSchema(lookup, nestedSchema, resolver);
      return AbstractOption.newOption(type, names, converter, help, optionSchema);
    }

    private static Function<Object, ?> resolveConverter(
        Lookup lookup, RecordComponent component, ConverterResolver resolver) {
      return resolver
          .resolve(lookup, component.getGenericType())
          .orElseThrow(
              () -> new UnsupportedOperationException("no converter for component " + component));
    }

    private static OptionType optionTypeFrom(Class<?> type) {
      if (type.isRecord()) return OptionType.BRANCH;
      if (type == Boolean.class || type == boolean.class) return OptionType.FLAG;
      if (type == Optional.class) return OptionType.SINGLE;
      if (type == List.class) return OptionType.REPEATABLE;
      if (type.isArray()) return OptionType.VARARGS;
      return OptionType.REQUIRED;
    }

    private static Class<? extends Record> toNestedSchema(RecordComponent component) {
      if (component.getType().isRecord()) return component.getType().asSubclass(Record.class);
      return (component.getGenericType() instanceof ParameterizedType paramType
              && paramType.getActualTypeArguments()[0] instanceof Class<?> nestedType
              && nestedType.isRecord())
          ? nestedType.asSubclass(Record.class)
          : null;
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

    private static <T extends Record> T createRecord(
        Class<T> schema, List<Object> values, Lookup lookup) {
      try {
        return schema.cast(
            constructor(lookup, schema).asFixedArity().invokeWithArguments(values.toArray()));
      } catch (RuntimeException | Error e) {
        throw e;
      } catch (Throwable e) {
        throw new UndeclaredThrowableException(e);
      }
    }
  }

  /**
   * Schema used to split the command line into arguments. The arguments are described using {@link
   * Option}s, the {@link Splitter} splits command line arguments following the recipe defined by
   * the options.
   *
   * <p>The option list should follow these rules:
   *
   * <ul>
   *   <li>there is at least one option
   *   <li>no two option share the same name
   *   <li>there is only zero or one varargs
   *   <li>the varargs should appear after all required.
   * </ul>
   *
   * <p>The schema also specify a finalizer function that is called with the list of arguments
   * decoded by the {@link Splitter} so the arguments can be bundled into a more user-friendly
   * class.
   *
   * <p>This class is deigned to be immutable so the finalizer should not do any side effects.
   *
   * @param <T> the type of the value bundling the command line arguments.
   * @see Splitter
   */
  public class Schema<T> {
    final List<Option<?>> options;
    private final Function<? super List<Object>, ? extends T> finalizer;

    /**
     * Create a schema from a list of options and a finalizer function.
     *
     * @param options the options.
     * @param finalizer a side effect free function.
     * @throws IllegalArgumentException if the list of options is empty, if the same option is
     *     specified twice, if at least two options share the same name, if there are more than one
     *     varargs, if the varargs is not specified after the required option.
     */
    public Schema(
        List<? extends Option<?>> options, Function<? super List<Object>, ? extends T> finalizer) {
      requireNonNull(options, "options is null");
      requireNonNull(finalizer, "finalizer is null");
      var opts = List.<Option<?>>copyOf(options);
      checkCardinality(opts);
      checkDuplicates(opts);
      checkVarargs(opts);
      this.options = opts;
      this.finalizer = finalizer;
    }

    private static void checkCardinality(List<Option<?>> options) {
      if (options.isEmpty()) throw new IllegalArgumentException("At least one option is expected");
    }

    private static void checkDuplicates(List<Option<?>> options) {
      var optionsByName = new HashMap<String, Option<?>>();
      for (var option : options) {
        var names = option.names();
        for (var name : names) {
          var otherOption = optionsByName.put(name, option);
          if (otherOption != null)
            throw new IllegalArgumentException(
                "options " + option + " and " + otherOption + " both declares name " + name);
        }
      }
    }

    private static void checkVarargs(List<Option<?>> options) {
      var varargs = options.stream().filter(AbstractOption::isVarargs).toList();
      if (varargs.isEmpty()) return;
      if (varargs.size() > 1)
        throw new IllegalArgumentException("Too many varargs types specified: " + varargs);
      var positionals = options.stream().filter(AbstractOption::isPositional).toList();
      if (!AbstractOption.isVarargs(positionals.get(positionals.size() - 1)))
        throw new IllegalArgumentException("varargs is not at last positional option: " + options);
    }

    T split(boolean nested, ArrayDeque<String> pendingArguments) {
      var requiredOptions =
          options.stream()
              .filter(AbstractOption::isRequired)
              .collect(toCollection(ArrayDeque::new));
      var optionalOptionByName = optionalOptionByName(options);
      var workspace = new Workspace(options);
      var flagCount = options.stream().filter(AbstractOption::isFlag).count();
      var flagPattern = flagCount == 0 ? null : Pattern.compile("^-[a-zA-Z]{1," + flagCount + "}$");

      var doubleDashMode = false;
      while (true) {
        if (pendingArguments.isEmpty()) {
          if (requiredOptions.isEmpty()) return workspace.create(finalizer);
          throw new SplittingException("Required option(s) missing: " + requiredOptions);
        }
        // acquire next argument
        var argument = pendingArguments.removeFirst();
        if ("--".equals(argument)) {
          doubleDashMode = true;
          continue;
        }
        var separator = argument.indexOf('=');
        var longForm = separator == -1;
        var argumentName = longForm ? argument : argument.substring(0, separator);
        var shortFormValue = longForm ? null : unQuote(argument.substring(separator + 1));
        // try well-known option first
        if (!doubleDashMode && optionalOptionByName.containsKey(argumentName)) {
          var option = optionalOptionByName.get(argumentName);
          if (option.type() == OptionType.BRANCH) {
            workspace.set(option, splitNested(pendingArguments, option));
            if (!pendingArguments.isEmpty())
              throw new SplittingException("Too many arguments: " + pendingArguments);
            return workspace.create(finalizer);
          }
          var optionValue =
              switch (option.type()) {
                case FLAG -> longForm || parseBoolean(shortFormValue);
                case SINGLE -> {
                  var value =
                      option.nestedSchema() != null
                          ? splitNested(pendingArguments, option)
                          : longForm ? nextArgument(pendingArguments, option) : shortFormValue;
                  yield Optional.of(value);
                }
                case REPEATABLE -> {
                  var value =
                      option.nestedSchema() != null
                          ? Stream.of(splitNested(pendingArguments, option))
                          : longForm
                              ? Stream.of(nextArgument(pendingArguments, option))
                              : Arrays.stream(shortFormValue.split(","));
                  var elements = (List<?>) workspace.get(option);
                  yield Stream.concat(elements.stream(), value).toList();
                }
                case BRANCH, VARARGS, REQUIRED -> throw new AssertionError("" + option);
              };
          workspace.set(option, optionValue);
          continue; // with next argument
        }
        // maybe a combination of single letter flags?
        if (!doubleDashMode && flagPattern != null && flagPattern.matcher(argument).matches()) {
          var flags = argument.substring(1).chars().mapToObj(c -> "-" + (char) c).toList();
          if (flags.stream().allMatch(optionalOptionByName::containsKey)) {
            flags.forEach(
                flag -> {
                  var option = optionalOptionByName.get(flag);
                  workspace.set(option, true);
                });
            continue;
          }
        }
        // try required option
        if (!requiredOptions.isEmpty()) {
          var requiredOption = requiredOptions.removeFirst();
          workspace.set(requiredOption, argument);
          continue;
        }
        // restore pending arguments deque
        pendingArguments.addFirst(argument);
        if (nested) return workspace.create(finalizer);
        // try globbing all pending arguments into a varargs collector
        var varargsOption =
            options.stream().filter(AbstractOption::isVarargs).findFirst().orElse(null);
        if (varargsOption != null) {
          workspace.set(varargsOption, pendingArguments.toArray(String[]::new));
          return workspace.create(finalizer);
        }
        throw new SplittingException("Unhandled arguments: " + pendingArguments);
      }
    }

    private static HashMap<String, Option<?>> optionalOptionByName(List<Option<?>> options) {
      var optionalOptionByName = new HashMap<String, Option<?>>();
      for (var option : options) {
        if (AbstractOption.isPositional(option)) {
          continue; // skip positional option
        }
        for (var name : option.names()) {
          optionalOptionByName.put(name, option);
        }
      }
      return optionalOptionByName;
    }

    private static String nextArgument(ArrayDeque<String> pendingArguments, Option<?> option) {
      if (pendingArguments.isEmpty()) {
        throw new SplittingException("no argument available for option " + option);
      }
      return pendingArguments.removeFirst();
    }

    private static String unQuote(String str) {
      return str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"'
          ? str.substring(1, str.length() - 1)
          : str;
    }

    private static Object splitNested(ArrayDeque<String> pendingArguments, Option<?> option) {
      return option.nestedSchema().split(true, pendingArguments);
    }

    private static final class Workspace {
      private final List<Option<?>> options;
      private final IdentityHashMap<Option<?>, Integer> indexMap;
      private final Object[] array;

      private Workspace(List<Option<?>> options) {
        this.options = options;
        indexMap =
            range(0, options.size())
                .boxed()
                .collect(
                    toMap(
                        options::get,
                        i -> i,
                        (_1, _2) -> {
                          throw null;
                        },
                        IdentityHashMap::new));
        var array = new Object[options.size()];
        Arrays.setAll(array, i -> AbstractOption.defaultValue(options.get(i)));
        this.array = array;
      }

      Object get(Option<?> option) {
        return array[indexMap.get(option)];
      }

      void set(Option<?> option, Object value) {
        array[indexMap.get(option)] = value;
      }

      private static Object convert(Option<?> option, Object value) {
        try {
          return AbstractOption.applyConverter(option, value);
        } catch (RuntimeException e) {
          throw new SplittingException("error while calling converter for option " + option, e);
        }
      }

      <T> T create(Function<? super List<Object>, ? extends T> finalizer) {
        var values =
            range(0, options.size()).mapToObj(i -> convert(options.get(i), array[i])).toList();
        return finalizer.apply(values);
      }
    }
  }

  /**
   * Splits the command line arguments. All the implementations of this interface must be
   * thread-safe.
   *
   * <p>A Splitter is created from a schema using {@link #of(Schema)}. Once configured the Splitter
   * can be used to split arguments using {@link #split(Stream)}.
   *
   * <pre>
   *   Schema&lt;X&gt; schema = ...
   *   Splitter&lt;X&gt; splitter = Splitter.of(schema);
   *   X result = splitter.split(args);
   * </pre>
   *
   * <h2>Using a record to define a schema</h2>
   *
   * <p>There is a convenient method {@link #of(Lookup, Class) of(lookup, class)} that uses a record
   * both as a schema and also as a storage for the arguments.
   *
   * <pre>
   *   record Command(
   *     &#064;Name("-v")  boolean verbose,
   *     Optional&lt;Level&gt; __level,
   *     List&lt;String&gt; __data,
   *     Path destination,
   *     Path... files) {}
   *
   *   Splitter&lt;Command&gt; splitter = Splitter.of(MethodHandles.lookup(), Command.class);
   *   Command command = splitter.split(args);
   * </pre>
   *
   * Each component of the record is transformed to an {@link Option}. The component type determines
   * the option type, a boolean/Boolean is a {@link Option.Flag Flag}, an Optional is a {@link
   * Option.Single Single}, a List is a {@link Option.Repeatable Repeatable}, an array is a {@link
   * Option.Varargs Varargs}, a record is a {@link Option.Branch Branch} otherwise it's a {@link
   * Option.Required Required}.
   *
   * <p>The annotations &#064;{@link Name} and &#064;{@link Help} specify respectively {@link
   * Option#names() several names} and a {@link Option#help() help text} of an option.
   *
   * <p>The conversion from strings are handled by the {@link ConverterResolver#defaultResolver()
   * default resolver}.
   *
   * <p>&nbsp;
   *
   * <h2>Using a list of options to define a schema</h2>
   *
   * <p>There is a convenient method {@link #of(Option[]) of(options...)} that uses an array of
   * options as schema and store the resulting arguments in an {@link ArgumentMap}.
   *
   * <pre>
   *   var verbose = Option.flag("-v");
   *   var level = Option.single("--level").convert(Level::valueOf);
   *   var data = Option.repeatable("--data");
   *   var destination = Option.required("destination").convert(Path::of);
   *   var files = Option.varargs("files").convert(Path::of, Path[]::new);
   *
   *   Splitter&lt;ArgumentMap&gt; splitter = Splitter.of(verbose, level, data, destination, files);
   *   ArgumentMap argumentMap = splitter.split(args);
   * </pre>
   *
   * This example defines the same schema as the record above but using the programmatic API.
   *
   * <p>&nbsp;
   *
   * <h2>Argument pre-processing</h2>
   *
   * <p>The méthodes {@link #withEach(UnaryOperator)} and {@link #withExpand(Function)} allows to
   * pre-process the arguments and respectively modify an argument or expand it into several
   * arguments.
   *
   * @param <T> the type of the class bundling all the arguments extracted from the command line.
   */
  @FunctionalInterface
  public interface Splitter<T> {

    /**
     * Returns a splitter configured from a record class and using the {@link
     * ConverterResolver#defaultResolver() default resolver}. The result of the method {@link
     * #split(Stream)} is an instance of the record class created using the lookup. The lookup is
     * also used to find the conversion functions if needed.
     *
     * @param lookup a lookup object.
     * @param schema a record class defining the schema.
     * @return a splitter configured from a record class.
     * @throws IllegalArgumentException if the record is not a valid schema.
     * @param <R> the type of the record.
     */
    static <R extends Record> Splitter<R> of(Lookup lookup, Class<R> schema) {
      return of(lookup, schema, ConverterResolver.defaultResolver());
    }

    /**
     * Returns a splitter configured from a record class and a conversion function resolver. The
     * result of the method {@link #split(Stream)} is an instance of the record class created using
     * the lookup. The lookup is also used by the resolver to find the conversion functions if
     * needed.
     *
     * @param lookup a lookup object.
     * @param schema a record class defining the schema.
     * @param resolver a conversion function resolver
     * @return a splitter configured from a record class.
     * @throws IllegalArgumentException if the record is not a valid schema.
     * @param <R> the type of the record.
     */
    static <R extends Record> Splitter<R> of(
        Lookup lookup, Class<R> schema, ConverterResolver resolver) {
      requireNonNull(schema, "schema is null");
      requireNonNull(lookup, "lookup is null");
      requireNonNull(resolver, "resolver is null");
      return of(RecordSchemaSupport.toSchema(lookup, schema, resolver));
    }

    /**
     * Returns a splitter configured from the options. The result of the method {@link
     * #split(Stream)} is an instance of {@link ArgumentMap}.
     *
     * @param options the options defining the schema.
     * @return a splitter configured from the options.
     */
    static Splitter<ArgumentMap> of(Option<?>... options) {
      requireNonNull(options, "options is null");
      return of(ArgumentMap.toSchema(options));
    }

    /**
     * Returns a splitter configured from a schema.
     *
     * @param schema the schema used to configure the splitter.
     * @return a splitter configured from a schema.
     * @param <T> type of the return type of the method {@link #split(Stream)}.
     */
    static <T> Splitter<T> of(Schema<T> schema) {
      Objects.requireNonNull(schema, "schema is null");
      return args -> {
        requireNonNull(args, "args is null");
        return schema.split(false, args.collect(toCollection(ArrayDeque::new)));
      };
    }

    /**
     * Splits the command line argument into different values following the recipe of the {@link
     * Schema} used to create this splitter.
     *
     * @param args the command line arguments.
     * @return an object gathering the values of the arguments.
     * @throws SplittingException if the arguments does not match the schema.
     */
    T split(Stream<String> args);

    /**
     * Splits the command line argument into different values following the recipe of the {@link
     * Schema} used to create this splitter. This is a convenient method equivalent to
     *
     * <pre>
     *   split(Arrays.stream(args))
     * </pre>
     *
     * @param args the command line arguments.
     * @return an object gathering the values of the arguments.
     * @throws SplittingException if the arguments does not match the schema.
     */
    default T split(String... args) {
      requireNonNull(args, "args is null");
      return split(Arrays.stream(args));
    }

    /**
     * Splits the command line argument into different values following the recipe of the {@link
     * Schema} used to create this splitter. This is a convenient method equivalent to
     *
     * <pre>
     *   split(args.stream())
     * </pre>
     *
     * @param args the command line arguments.
     * @return an object gathering the values of the arguments.
     * @throws SplittingException if the arguments does not match the schema.
     */
    default T split(List<String> args) {
      requireNonNull(args, "args is null");
      return split(args.stream());
    }

    /*
    Argument preprocessing
     */

    /**
     * Returns a splitter that will call the preprocessor on all arguments of the command line when
     * {@link #split(Stream)} is called.
     *
     * @param preprocessor an argument pre-processor.
     * @return a splitter that will call the preprocessor on all arguments of the command line.
     * @see #split(Stream)
     */
    default Splitter<T> withEach(UnaryOperator<String> preprocessor) {
      requireNonNull(preprocessor, "preprocessor is null");
      return args -> split(args.map(preprocessor));
    }

    /**
     * Returns a splitter that will call the preprocessor on all arguments of the command line when
     * {@link #split(Stream)} is called.
     *
     * @param preprocessor an argument pre-processor that can expand each argument into multiple
     *     arguments.
     * @return a splitter that will call the preprocessor on all arguments of the command line.
     * @see #split(Stream)
     */
    default Splitter<T> withExpand(
        Function<? super String, ? extends Stream<String>> preprocessor) {
      requireNonNull(preprocessor, "preprocessor is null");
      return args -> split(args.flatMap(preprocessor));
    }
  }

  /**
   * Exception that occurs when splitting the arguments.
   *
   * @see Splitter#split(Stream)
   */
  public class SplittingException extends RuntimeException {

    @Serial private static final long serialVersionUID = 6958903301611893552L;

    /**
     * Creates a splitting exception with a message and a cause.
     *
     * @param message a message.
     * @param cause a cause.
     */
    public SplittingException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * Creates a splitting exception with a message.
     *
     * @param message a message.
     */
    public SplittingException(String message) {
      super(message);
    }

    /**
     * Creates a splitting exception with a cause.
     *
     * @param cause a cause.
     */
    public SplittingException(Throwable cause) {
      super(cause);
    }
  }
}
