package test.base.resource;

import static org.junit.platform.commons.support.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.support.ReflectionSupport.findFields;
import static org.junit.platform.commons.support.ReflectionSupport.newInstance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.ReflectionSupport;

public class ResourceManager implements ParameterResolver, AfterEachCallback {

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
  public @interface New {

    Class<? extends ResourceSupplier<?>> value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Shared {
    String name();

    Class<? extends ResourceSupplier<?>> type();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Singleton {

    Class<? extends ResourceSupplier<?>> value();
  }

  private static final Namespace NAMESPACE = Namespace.create(ResourceManager.class);
  private static final AtomicLong NEW_COUNTER = new AtomicLong();

  private final Map<String, Set<ResourceSupplier<?>>> registry;

  public ResourceManager() {
    this.registry = new ConcurrentHashMap<>();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameter, ExtensionContext __) {
    return parameter.isAnnotated(New.class)
        ^ parameter.isAnnotated(Shared.class)
        ^ parameter.isAnnotated(Singleton.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameter, ExtensionContext extension) {
    ResourceSupplier<?> supplier = supplier(parameter, extension);
    Class<?> parameterType = parameter.getParameter().getType();
    if (ResourceSupplier.class.isAssignableFrom(parameterType)) {
      return supplier;
    }
    Object instance = supplier.get();
    if (parameterType.isAssignableFrom(instance.getClass())) {
      return instance;
    }
    try {
      return supplier.as(parameterType);
    } catch (UnsupportedOperationException ignore) {
      // fall-through
    }
    throw new ParameterResolutionException(
        "parameter type "
            + parameterType
            + " isn't compatible with "
            + ResourceSupplier.class
            + " nor "
            + instance.getClass());
  }

  private ResourceSupplier<?> supplier(ParameterContext parameter, ExtensionContext context) {
    Optional<New> newAnnotation = parameter.findAnnotation(New.class);
    if (newAnnotation.isPresent()) {
      Class<? extends ResourceSupplier<?>> type = newAnnotation.get().value();
      String key = type.getName() + '@' + NEW_COUNTER.incrementAndGet();
      ResourceSupplier<?> resourceSupplier = newInstance(type);
      assert context.getStore(NAMESPACE).get(key) == null;
      context.getStore(NAMESPACE).put(key, resourceSupplier);
      // remember all suppliers created for a constructor parameter resolution
      if (parameter.getDeclaringExecutable() instanceof Constructor) {
        String registryKey = parameter.getDeclaringExecutable().getDeclaringClass().getName();
        registry.computeIfAbsent(registryKey, k -> new HashSet<>()).add(resourceSupplier);
      }
      return resourceSupplier;
    }

    Optional<Shared> sharedAnnotation = parameter.findAnnotation(Shared.class);
    if (sharedAnnotation.isPresent()) {
      Class<? extends ResourceSupplier<?>> type = sharedAnnotation.get().type();
      String key = sharedAnnotation.get().name();
      return supplier(context, key, type);
    }

    Optional<Singleton> singletonAnnotation = parameter.findAnnotation(Singleton.class);
    if (singletonAnnotation.isPresent()) {
      Class<? extends ResourceSupplier<?>> type = singletonAnnotation.get().value();
      String key = type.getName();
      return supplier(context, key, type);
    }

    throw new ParameterResolutionException("Can't resolve resource supplier for: " + parameter);
  }

  private ResourceSupplier<?> supplier(
      ExtensionContext context, String key, Class<? extends ResourceSupplier<?>> type) {
    ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
    return store.getOrComputeIfAbsent(key, k -> newInstance(type), ResourceSupplier.class);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    if (context.getTestInstanceLifecycle().isEmpty()) {
      return; // engine node
    }
    var lifecycle = context.getTestInstanceLifecycle().get();
    if (lifecycle == TestInstance.Lifecycle.PER_CLASS) {
      return; // don't close class resources after each test method
    }
    var suppliers = registry.getOrDefault(context.getRequiredTestClass().getName(), Set.of());
    if (suppliers.isEmpty()) {
      return; // no "@New resource" constructor parameters registered for this test class
    }
    var optionalTestClass = context.getTestClass();
    if (optionalTestClass.isEmpty()) {
      return; // no test class, no cleanup
    }
    // find fields holding a reference to a registered supplier or the value it supplies
    // TODO exclude static fields?
    for (var field : findFields(optionalTestClass.get(), __ -> true, TOP_DOWN)) {
      var value =
          ReflectionSupport.tryToReadFieldValue(field, context.getRequiredTestInstance())
              .getOrThrow(RuntimeException::new);
      for (var supplier : suppliers) {
        if (supplier == value || supplier.get() == value) {
          context.publishReportEntry("closing resource", supplier.get().toString());
          try {
            supplier.close();
          } catch (Exception e) {
            // TODO Collect and report exceptions...
            throw new RuntimeException("closing resource supplier failed", e);
          }
        }
      }
    }
  }
}
