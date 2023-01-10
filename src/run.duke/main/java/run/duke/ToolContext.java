package run.duke;

import java.util.Optional;

/** A constant supplier. */
@FunctionalInterface
public interface ToolContext {
  /**
   * Returns an instance of the given record type.
   *
   * @param key the record type to lookup
   * @return an instance of the given record type, may be {@code null}
   * @param <R> the record type
   */
  <R extends Record> R getConstant(Class<R> key);

  /**
   * Returns an instance of the given record type wrapped in an {@code Optional}.
   *
   * @param key the record type to lookup
   * @return an instance of the given record type, may be {@code Optional.empty()}
   * @param <R> the record type
   */
  default <R extends Record> Optional<R> findConstant(Class<R> key) {
    return Optional.ofNullable(getConstant(key));
  }
}
