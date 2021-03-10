package com.vinayemani.devsearch.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * UserKey represents a single user search key. We support searching for users based on their
 * first name, last name and location attributes.  
 * 
 * @author Vinay E.
 * 
 */
public class UserKey {
	private final String firstName;
	private final String lastName;
	private final String location;
	
	public UserKey(String firstName, String lastName, String location) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.location = location;
	}
	
	private static String empty(String s) {
		if (s == null) {
			return "";
		}
		
		return s;
	}
	
	/**
	 * Constructs a url encoded query string from the firstName, lastName and location attributes.
	 * 
	 * @return A url encoded query parameter string for github api.
	 */
	public String constructQueryParamString() {
		// url_encode(<name> type:user in:fullname location:location)
		String name = empty(firstName) + " " + empty(lastName);
		if (name.equals(" ")) {
			name = "";
		}
		
		String query = name + " type:user in:fullname";
		if (location != null && location.length() > 0) {
			query = query + " " + "location:" + location;
		}
		
		try {
			return URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		
		return "";
	}
}
