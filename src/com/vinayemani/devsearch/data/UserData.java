package com.vinayemani.devsearch.data;

import java.util.List;

/**
 * UserData represents data retrieved about a single user. This data consists of 
 * her public profile info as an {@link UserProfile} object and the list of repos she
 * is contributing to.
 * 
 * @author Vinay E.
 *
 */
@lombok.Getter
public class UserData {
	private final UserProfile profile;
	private List<RepoData> repos;
	
	public UserData(UserProfile profile, List<RepoData> repos) {
		this.profile = profile;
		
		this.repos = repos;
	}
}
