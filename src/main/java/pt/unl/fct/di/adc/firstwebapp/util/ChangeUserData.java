package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangeUserData {
  public Input input;
  public AuthToken token;

  public class Input {
    public String username;
    public Role newRole;
  }

  public ChangeUserData() {
  }

  public ChangeUserData(String username, Role role, AuthToken token) {
    this.input = new Input();
    this.token = token;
    this.input.username = username;
    this.input.newRole = role;
  }
}
