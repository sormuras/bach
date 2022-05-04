package com.github.sormuras.bach.project.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Download implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {

    var calls = new ArrayList<ToolCall>();
    for (var external : bach.project().tools().externals()) {
      var directory = bach.configuration().paths().root(".bach", "external-tools", external.name());
      if (external.from().isPresent()) {
        var source = external.from().get();
        var begin = source.lastIndexOf('/') + 1;
        var end = source.indexOf('#', begin);
        var name = source.substring(begin, end != -1 ? end : source.length());
        var target = directory.resolve(name);
        calls.add(ToolCall.of("load-and-verify", target, source));
      }
      for (var asset : external.assets()) {
        var target = directory.resolve(asset.name());
        var source = asset.from();
        calls.add(ToolCall.of("load-and-verify", target, source));
      }
    }
    calls.forEach(bach::run);

    return 0;
  }
}
