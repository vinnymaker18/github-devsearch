package com.vinayemani.devsearch.data;

/**
 * RepoData stores data about a single repository. Only public repositories are supported.
 * Following attributes are supported - owner's login id and whether the repo is a fork.  
 *  
 * @author Vinay E.
 *
 */
@lombok.Getter
@lombok.AllArgsConstructor
public class RepoData {
	private final int numCommits;
	private final String name;
	
	@Override
	public String toString() {
		return String.format("Repo(name=%s, numCommits=%d)", name, numCommits);
	}
}
