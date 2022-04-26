package com.github.sormuras.bach;

import java.io.PrintWriter;

public record Printer(PrintWriter out, PrintWriter err) {
  void out(String string) {
    out.println(string);
  }

  void err(String string) {
    err.println(string);
  }
}
