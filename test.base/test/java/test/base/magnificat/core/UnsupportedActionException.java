package test.base.magnificat.core;

public class UnsupportedActionException extends UnsupportedOperationException {
  public UnsupportedActionException(String action) {
    super("Unsupported action: " + action);
  }
}
