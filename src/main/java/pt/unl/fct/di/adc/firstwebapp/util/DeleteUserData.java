package pt.unl.fct.di.adc.firstwebapp.util;

public class DeleteUserData {
  public Input input;
  public AuthToken token;

  public class Input {
    public String username;
  }

  public DeleteUserData() {
  }

  public DeleteUserData(AuthToken token, String username) {
    this.input = new Input();
    this.token = token;
    this.input.username = username;
  }
}
