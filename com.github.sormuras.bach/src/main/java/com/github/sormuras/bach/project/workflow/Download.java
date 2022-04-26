package com.github.sormuras.bach.project.workflow;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Download implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    /*
    var calls = new ArrayList<Tool.Call>();
    for (var external : bach.configuration.project.tools.externals) {
        var directory = bach.configuration.paths.root(".bach", "external-tools", external.name);
        if (external.from.isPresent()) {
            var source = external.from.get();
            var begin = source.lastIndexOf('/') + 1;
            var end = source.indexOf('#', begin);
            var name = source.substring(begin, end != -1 ? end : source.length());
            var target = directory.resolve(name);
            calls.add(Tool.Call.of("load-and-verify", target, source));
        }
        for (var asset : external.assets) {
            var target = directory.resolve(asset.name);
            var source = asset.from;
            var call = Tool.Call.of("load-and-verify", target, source);
            if (source.startsWith("string:")) {
                var size = source.getBytes(StandardCharsets.UTF_8).length - 7;
                call = call.with("SIZE=" + size);
            }
            calls.add(call);
        }
    }
    calls.forEach(bach::run);
    */
    return 0;
  }
}
