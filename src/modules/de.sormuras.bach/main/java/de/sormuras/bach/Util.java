package de.sormuras.bach;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/*BODY*/
/** Static helpers. */
public /*STATIC*/ class Util {

  static Optional<Method> findApiMethod(Class<?> container, String name) {
    try {
      var method = container.getMethod(name);
      return isApiMethod(method) ? Optional.of(method) : Optional.empty();
    } catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }

  static boolean isApiMethod(Method method) {
    if (method.getDeclaringClass().equals(Object.class)) return false;
    if (Modifier.isStatic(method.getModifiers())) return false;
    return method.getParameterCount() == 0;
  }
}
