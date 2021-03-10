package com.vinayemani.devsearch;

/**
 * Authorization scheme type for GitHub api requests. Clients can authenticate using 
 * personal access tokens(PATs), OAuth2 tokens or simply make unauthorized requests.
 * 
 * @author Vinay E.
 *
 */
public enum AuthScheme {
	NO_AUTH,
	BASIC,
	OAUTH2,
	INVALID
}
