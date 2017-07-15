package hallo;

import com.greetings.Greeter;

public class Hallo implements Greeter {

    public String greet(String name) {
        return "Hallo " + name;
    }

}
