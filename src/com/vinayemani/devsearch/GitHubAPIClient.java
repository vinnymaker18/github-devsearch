package com.vinayemani.devsearch;

import static com.vinayemani.devsearch.Connection.RESP_CODE_KEY;
import static com.vinayemani.devsearch.Connection.RESP_CODE_OK;
import static com.vinayemani.devsearch.Connection.RESP_CODE_FORBIDDEN;
import static com.vinayemani.devsearch.Connection.RESP_DATA_KEY;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.vinayemani.devsearch.data.*;

/**
 * GitHubAPIClient is a easy to use interface for querying GitHub's API.
 * 
 * @author Vinay E.
 *
 */
public class GitHubAPIClient {
	// API end points.
	public static final String GITHUB_API_URL_ROOT = "https://api.github.com";
	public static final String SEARCH_USERS_ENDPOINT = "/search/users?q=";
	public static final String RATE_LIMIT_CHECK_ENDPOINT = "/rate_limit";
	public static final String GET_USER_ENDPOINT = "/users/";
	public static final String GET_REPO_ENDPOINT = "/repos/";
	
	// Rate limit JSON responses will have these keys.
	private static final String RATE_LIMIT_RESOURCES_KEY = "resources";
	private static final String RATE_LIMIT_CORE_KEY = "core";
	private static final String RATE_LIMIT_SEARCH_KEY = "search";
	
	// Rate limit reset periods(in sec) for core & search apis. 
	private static final int CORE_API_RESET_PERIOD_SEC = 3600;
	private static final int SEARCH_API_RESET_PERIOD_SEC = 60;
	
	// The connection state for this client.
	private Connection connection;
	
	private GitHubAPIClient(AuthCredentials credentials) throws InvalidCredentialsException {
		this.connection = new Connection(credentials);
		if (!verifyCredentials(credentials)) {
			throw new InvalidCredentialsException("Invalid credentials");
		}
	}
	
	/** Verifies credentials on the server. */
	private boolean verifyCredentials(AuthCredentials credentials) {
		// First a basic sanity check to see if all expected parts are present.
		if (credentials == null || !credentials.isGood()) {
			return false;
		}
		
		// For no auth case, we don't need to do anything else.
		if (credentials.getAuthScheme() == AuthScheme.NO_AUTH) {
			return true;
		}
		
		// Now, actually verify these credentials on the server by sending a rate_limit request and
		// receiving a 200 OK response.
		try {
			JSONObject resp = connection.getResponse(getFullURI(RATE_LIMIT_CHECK_ENDPOINT));
			return resp.getInt(RESP_CODE_KEY) == RESP_CODE_OK;
		} catch (IOException e) {
			// Connection aborted, we consider the verification failed.
		}
		
		return false;
	}
	
	private static URI getFullURI(String endPoint) {
		try {
			return new URI(GITHUB_API_URL_ROOT + endPoint);
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	/**
	 * Searches for a single user and returns the user login.
	 * @param key User key to be searched.
	 * 
	 * @return Login of the best match user for the search.
	 */
	static APICallResult<String> searchForSingleUser(UserKey key, Connection conn) {
		URI searchQry = getFullURI(SEARCH_USERS_ENDPOINT + key.constructQueryParamString());
		try {
			JSONObject resp = conn.getResponse(searchQry);
			if (resp.getInt(RESP_CODE_KEY) == RESP_CODE_FORBIDDEN) {
				// rate limit exceeded.
				return APICallResult.rateLimitExceededResult();
			} else {
				// successful output.
				JSONArray users = resp.getJSONObject("data").getJSONArray("items");
				if (users.length() == 0) {
					// no matching users found.
					return APICallResult.noMatchResult();
				}
				
				JSONObject bestMatchingUser = users.getJSONObject(0);
				return APICallResult.successResult(bestMatchingUser.getString("login"));
			}
		} catch (IOException e) {
			// Exception in search query, return error result.
			return APICallResult.errorResult();
		}
	}
	
	private static String getIfPresent(JSONObject obj, String key) {
		try {
			return obj.getString(key);
		} catch (JSONException e) {
			return "";
		}
	}
	
	private static UserProfile constructUserProfile(JSONObject obj) {
		// name, login, company, email, blog, location
		String name = getIfPresent(obj, "name");
		String login = getIfPresent(obj, "login");
		String email = getIfPresent(obj, "email");
		String company = getIfPresent(obj, "company");
		String blog = getIfPresent(obj, "blog");
		String location = getIfPresent(obj, "location");
		return new UserProfile(name, login, company, blog, location, email);
	}
	
	/**
	 * Fetches user data (public profile data + public repos + commits) given the login id. 
	 *  
	 * @param userLogin GitHub login id of a user.
	 * @param conn Connection object.
	 * 
	 * @return Data for a single user.
	 */
	static APICallResult<UserData> fetchSingleUserData(String userLogin, Connection conn) {
		URI getUserQry = getFullURI(GET_USER_ENDPOINT + userLogin);
		try {
			JSONObject resp = conn.getResponse(getUserQry);
			if (resp.getInt(RESP_CODE_KEY) == RESP_CODE_FORBIDDEN) {
				return APICallResult.rateLimitExceededResult();
			} else if (resp.getInt(RESP_CODE_KEY) != RESP_CODE_OK) {
				// no matching user found.
				return APICallResult.noMatchResult();
			} else {
				UserProfile profile = constructUserProfile(resp.getJSONObject(RESP_DATA_KEY));
				
				// Fetch repositories.
				URI userRepoQry = getFullURI(GET_USER_ENDPOINT + userLogin + "/repos?type=all");
				JSONObject reposResp = conn.getSequence(userRepoQry);
				if (reposResp.getInt(RESP_CODE_KEY) == RESP_CODE_FORBIDDEN) {
					return APICallResult.rateLimitExceededResult();
				} else if (reposResp.getInt(RESP_CODE_KEY) != RESP_CODE_OK) {
					return APICallResult.errorResult();
				}
				
				JSONArray repos = reposResp.getJSONArray(RESP_DATA_KEY);
				
				List<RepoData> userRepos = new ArrayList<>();
				int numRepos = repos.length();
				for (int i = 0;i < numRepos; i++) {
					JSONObject repo = repos.getJSONObject(i);
					String repoName = repo.getString("name");
					String ownerLogin = repo.getJSONObject("owner").getString("login");
					URI getUserRepoCommitsQry = getFullURI(GET_REPO_ENDPOINT + 
							ownerLogin + "/" + repoName + "/commits?author=" + userLogin);
					JSONObject commitsResp = conn.getSequence(getUserRepoCommitsQry);
					if (commitsResp.getInt(RESP_CODE_KEY) == RESP_CODE_FORBIDDEN) {
						return APICallResult.rateLimitExceededResult();
					} else if (commitsResp.getInt(RESP_CODE_KEY) != RESP_CODE_OK) {
						continue;
					}
					
					JSONArray commits = commitsResp.getJSONArray(RESP_DATA_KEY);
					userRepos.add(new RepoData(commits.length(), repoName));
				}
				
				return APICallResult.successResult(new UserData(profile, userRepos));
			}
		} catch (IOException e) {
			// Exception in get user query, error result is returned.
			return APICallResult.errorResult();
		}
	}
	
	/** 
	 * All constructors for GitHubAPIClient throw InvalidCredentialsException when initialized with wrong
	 * credentials.
	 */
	
	/** An api client using no authentication. */
	public GitHubAPIClient() throws InvalidCredentialsException {
		this(AuthCredentials.createNoAuthCredentials());
	}
	
	/** An api client using basic authentication. */
	public GitHubAPIClient(String username, String password) throws InvalidCredentialsException {
		this(AuthCredentials.createBasicAuthCredentials(username, password));
	}
	
	/** An api client using OAuth2 authentication. */
	public GitHubAPIClient(String oAuthToken) throws InvalidCredentialsException {
		this(AuthCredentials.createOAuth2AuthCredentials(oAuthToken));
	}
	
	/** Current APIRateLimits(core and search) for this user. */
	public static APIRateLimit[] getRateLimits(Connection connection) {
		APIRateLimit[] ret = new APIRateLimit[2];
		try {
			JSONObject resp = connection.getResponse(getFullURI(RATE_LIMIT_CHECK_ENDPOINT));
			JSONObject resources = resp.getJSONObject(RESP_DATA_KEY).getJSONObject(RATE_LIMIT_RESOURCES_KEY);
			ret[0] = APIRateLimit.fromJSONObject(resources.getJSONObject(RATE_LIMIT_CORE_KEY), CORE_API_RESET_PERIOD_SEC);
			ret[1] = APIRateLimit.fromJSONObject(resources.getJSONObject(RATE_LIMIT_SEARCH_KEY), SEARCH_API_RESET_PERIOD_SEC);
			return ret;
		} catch (IOException e) {
			return null;
		}
	}
	
	public APIRateLimit[] getRateLimits() {
		return getRateLimits(connection);
	}
	
	/**
	 * This is a low level api for searching GitHub users given their name, location attributes.
	 *  
	 * @param users List of user search keys
	 * 
	 * @return User data.
	 */
	public Map<Long, UserData> searchForUsers(List<UserKey> users) {
		RateLimiter limiter = new RateLimiter(connection);
		return limiter.getSearchResults(users);
	}
	
	/**
	 * This is a high level api for reading user keys from input file and writing user data to output file.
	 * 
	 * @param inputFile Input file path on the system.
	 * @param outputDest Output file path.
	 * 
	 * @return true if user data is successfully written to output destination, false otherwise. 
	 * @throws IOException
	 */
	public boolean searchForUsers(String inputFile, String outputDest) throws IOException, BadInputFileException {
		List<UserKey> userKeys = FileUtils.parseFile(inputFile);
		Map<Long, UserData> userData = searchForUsers(userKeys);
		
		return FileUtils.writeUserDataToFile(userData.values(), outputDest);
	}
}
