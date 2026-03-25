package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangeRoleData {
  public Input input;
  public AuthToken token;

  public class Input {
    public String username;
    public Role newRole;
  }

  public ChangeRoleData() {
  }

  public ChangeRoleData(String username, Role role, AuthToken token) {
    this.input = new Input();
    this.token = token;
    this.input.username = username;
    this.input.newRole = role;
  }
}
