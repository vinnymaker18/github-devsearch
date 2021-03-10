package com.vinayemani.devsearch.data;

/**
 * UserProfile is a POJO for holding the public profile details of a GitHub user. Currently, the following 
 * attributes are supported.
 * 
 * Name, login, company name, blog, location and email.
 * 
 * @author Vinay E.
 *
 */
@lombok.Getter
public class UserProfile {
	private final String name;
	private final String login;
	private final String companyName;
	private final String blogUrl;
	private final String location;
	private final String email;
	
	public UserProfile(String name, String login, String company, String blogUrl, String location, String email) {
		this.name = name;
		this.login = login;
		this.companyName = company;
		this.blogUrl = blogUrl;
		this.location = location;
		this.email = email;
	}
	
	@Override
	public String toString() {
		return String.format("UserProfile(name=%s, login=%s, company=%s, location=%s)", name, login, companyName, location);
	}
}
