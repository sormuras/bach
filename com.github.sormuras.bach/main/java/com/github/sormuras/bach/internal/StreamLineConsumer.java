package com.github.sormuras.bach.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

record StreamLineConsumer(InputStream stream, Consumer<String> consumer) implements Runnable {
  public void run() {
    new BufferedReader(new InputStreamReader(stream)).lines().forEach(consumer);
  }
}
