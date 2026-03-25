package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000 * 60 * 15; // 15 min

	public String username;
	public String tokenID;
	public long issuedAt;
	public long expiresAt;
	public Role role;

	public AuthToken() {
	}

	public AuthToken(String username, Role role) {
		this.username = username;
		this.tokenID = UUID.randomUUID().toString();
		this.issuedAt = System.currentTimeMillis();
		this.expiresAt = this.issuedAt + EXPIRATION_TIME;
		this.role = role;
	}
}
