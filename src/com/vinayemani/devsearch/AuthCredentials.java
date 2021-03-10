package com.vinayemani.devsearch;

import java.util.Base64;

/**
 * AuthCredentials objects store the credentials required for making successful api requests.
 * 
 * @author Vinay E.
 *
 */
class AuthCredentials {
	
	@lombok.Getter
	private final AuthScheme authScheme;
	
	private final String[] authParams;
	
	private AuthCredentials(AuthScheme scheme, String[] authParams) {
		this.authScheme = scheme;
		this.authParams = authParams;
	}
	
	// In case of no authentication, this can be made a singleton. 
	public static final AuthCredentials AUTH_CREDS_NO_AUTH = new AuthCredentials(AuthScheme.NO_AUTH, new String[] {});
	
	/** AuthManager object for no auth. case. */
	public static AuthCredentials createNoAuthCredentials() {
		return AUTH_CREDS_NO_AUTH;
	}
	
	/** Create and return a credentials object for the basic auth. case */
	public static AuthCredentials createBasicAuthCredentials(String username, String password) {
		return new AuthCredentials(AuthScheme.BASIC, new String[] {username, password});
	}
	
	/** Create and return a new credentials object for OAuth2 case. */
	public static AuthCredentials createOAuth2AuthCredentials(String token) {
		return new AuthCredentials(AuthScheme.OAUTH2, new String[] {token});
	}
	
	/**
	 * A basic sanity check that can first be done without going to the server. e.g, this looks for valid username/password
	 * in the basic auth case and a valid token in the oauth case. 
	 * 
	 * @return True if this object passes the sanity check and false otherwise.
	 */
	boolean isGood() {
		if (authParams == null) {
			return false;
		}
		
		if (authScheme == AuthScheme.BASIC) {
			return !(authParams.length != 2 || authParams[0] == null || authParams[1] == null);
		} else if (authScheme == AuthScheme.OAUTH2) {
			return authParams.length == 1 && authParams[0] != null;
		}
		
		return true;
	}
	
	/**
	 * GitHub servers require a 'Authorization' header to be passed. Its value typically depends on the
	 * auth scheme and credentials.
	 *  
	 * @return Authorization header value to be passed in the request.
	 */
	public String getAuthHeader() {
		if (authScheme == AuthScheme.OAUTH2) {
			// Header line will look like Authorization: token [oath_token]
			String token = authParams[0];
			return "token " + token;
		} else if (authScheme == AuthScheme.BASIC) {
			String usr = authParams[0] , pwd = authParams[1];
			String toEncode = usr + ":" + pwd;
			return "Basic " + Base64.getEncoder().encodeToString(toEncode.getBytes());
		}
		else {
			// Header not required in other cases.
			return "";
		}
	}
}