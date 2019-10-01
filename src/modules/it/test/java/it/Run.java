package it;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public class Run implements ToolProvider {
    @Override
    public String name() {
        return "test(it)";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
        out.println("\n\n (-: \n\n");
        return 0;
    }
}
