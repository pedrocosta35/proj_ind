package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.datastore.Entity;

public class SessionsData {

  public String tokenId;
  public String username;
  public String role;
  public long expiresAt;

  public SessionsData(Entity entity) {
    this.tokenId = entity.getString("tokenId");
    this.username = entity.getString("username");
    this.role = entity.getString("role");
    this.expiresAt = entity.getLong("expiresAt");
  }
}
