package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 2; // 2h

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

	public boolean isExpired() {
		return System.currentTimeMillis() > expiresAt;
	}

	public boolean hasPermission(Role... allowedRoles) { // maked in a secure way even thought in the exercise we check
																												// role after expiration
		for (Role allowedRole : allowedRoles) {
			if (this.role == allowedRole) {
				return !isExpired();
			}
		}
		return false;
	}

}
