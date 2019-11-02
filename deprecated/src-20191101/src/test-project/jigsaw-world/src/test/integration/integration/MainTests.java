package integration;

public class MainTests {
  public static void main(String[] args){
    assert "integration".equals(MainTests.class.getModule().getName());
  }
}
