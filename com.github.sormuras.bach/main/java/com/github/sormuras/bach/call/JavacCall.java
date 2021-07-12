package com.github.sormuras.bach.call;

public interface JavacCall<C extends JavacCall<C>> extends CallWithJavacOrJavadoc<C> {

  default String name() {
    return "javac";
  }
}
