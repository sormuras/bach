import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandTests {
    private static void dump() {
        List<String> strings = new ArrayList<>();
        Bach.Command command = Bach.Command.of("executable");
        command.add("--some-option").add("value");
        command.add("-single-flag-without-values");
        command.limit(5);
        command.addAll("0", "1", "2", "3", "4");
        command.addAll("5", "6", "7", "8", "9");
        command.dump((format, args) -> strings.add(String.format(format, args)));
        assert Objects.equals(11, strings.size());
    }

    public static void main(String[] args) {
        dump();
    }
}
