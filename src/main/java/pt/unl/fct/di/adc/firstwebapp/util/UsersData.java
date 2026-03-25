package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.datastore.Entity;

public class UsersData {
  public String userId;
  public String username;
  public String email;
  public String role;

  public UsersData(Entity entity) {
    this.username = entity.getString("username");
    this.role = entity.getString("role");
  }
}
