package com.vinayemani.devsearch;

/**
 * A marker exception thrown to indicate that the GitHubAPIClient is being created with invalid credentials. Users
 * must catch this and create a new client with valid credentials. 
 * 
 * @author Vinay E.
 *
 */
public class InvalidCredentialsException extends Exception {
	private static final long serialVersionUID = 7160587941161519299L;
	
	public InvalidCredentialsException(String message) {
		super(message);
	}
}
