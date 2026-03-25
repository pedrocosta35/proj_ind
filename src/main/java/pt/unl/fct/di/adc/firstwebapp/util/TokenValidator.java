package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;

public class TokenValidator {

  private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

  public static boolean validate(String username, String tokenID, Role... allowedRoles) {
    Key tokenKey = datastore.newKeyFactory()
        .addAncestors(PathElement.of("User", username))
        .setKind("AuthToken")
        .newKey(tokenID);

    Entity tokenEntity = datastore.get(tokenKey);
    if (tokenEntity == null)
      return false;
    Timestamp expiration = tokenEntity.getTimestamp("expiresAt");
    if (System.currentTimeMillis() > expiration.toDate().getTime()) { // Token expired, delete it so it will be quicker
                                                                      // to check next time and return false
      datastore.delete(tokenKey);
      return false;
    }
    if (allowedRoles.length > 0) {
      Role tokenRole = Role.valueOf(tokenEntity.getString("role"));
      for (Role allowed : allowedRoles) {
        if (tokenRole == allowed)
          return true;
      }
      return false;
    }
    return true;
  }
}