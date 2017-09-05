import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

class DisabledCondition implements ExecutionCondition {
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
    AnnotatedElement element = extensionContext.getElement().orElseThrow(AssertionError::new);
    Disabled disabled = findAnnotation(element, Disabled.class).orElseThrow(AssertionError::new);
    String name = disabled.value();
    String reason = name + "=" + System.getProperty(name);
    boolean result = Boolean.getBoolean(name);
    if (disabled.not()) {
      result = !result;
    }
    if (result) {
      return ConditionEvaluationResult.disabled(reason);
    }
    return ConditionEvaluationResult.enabled(reason);
  }
}
