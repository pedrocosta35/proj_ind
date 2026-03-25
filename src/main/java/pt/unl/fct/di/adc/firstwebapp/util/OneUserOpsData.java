package pt.unl.fct.di.adc.firstwebapp.util;

public class OneUserOpsData {
  public Input input;
  public AuthToken token;

  public class Input {
    public String username;
  }

  public OneUserOpsData() {
  }

  public OneUserOpsData(AuthToken token, String username) {
    this.input = new Input();
    this.token = token;
    this.input.username = username;
  }
}
