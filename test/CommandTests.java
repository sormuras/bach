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
        command.dump((format, args) -> strings.add(String.format('|' + format, args).trim()));
        assert Objects.equals("|executable", strings.get(0));
        assert Objects.equals("|--some-option", strings.get(1));
        assert Objects.equals("|  value", strings.get(2));
        assert Objects.equals("|-single-flag-without-values", strings.get(3));
        assert Objects.equals("|0", strings.get(4));
        assert Objects.equals("|1", strings.get(5));
        assert Objects.equals("|2", strings.get(6));
        assert Objects.equals("|3", strings.get(7));
        assert Objects.equals("|4", strings.get(8));
        assert Objects.equals("|... [omitted 4 arguments]", strings.get(9));
        assert Objects.equals("|9", strings.get(10));
    }

    public static void main(String[] args) {
        dump();
    }
}
