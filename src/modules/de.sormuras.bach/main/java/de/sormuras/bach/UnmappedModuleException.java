package de.sormuras.bach;

/*BODY*/
/** Unchecked exception thrown when a module name is not mapped. */
public /*static*/ class UnmappedModuleException extends IllegalStateException {

  public static String raise(String module) {
    throw new UnmappedModuleException(module);
  }

  private static final long serialVersionUID = 6985648789039587477L;

  private UnmappedModuleException(String module) {
    super("Module " + module + " is not mapped");
  }
}
