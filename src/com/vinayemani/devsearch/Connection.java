package com.vinayemani.devsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A Connection object prevents a simple interface for making successful api requests, by hiding the state and logic 
 * required. e.g., it stores the credentials, sets certain request headers etc...
 * 
 * @author Vinay E.
 *
 */
class Connection {
	public static final String RESP_CODE_KEY = "respCode";
	public static final int RESP_CODE_OK = 200;
	public static final int RESP_CODE_UNAUTHORIZED = 401;
	public static final int RESP_CODE_FORBIDDEN = 403;
	public static final int RESP_CODE_NOT_FOUND = 404;

	public static final String RESP_DATA_KEY = "data";
	
	private static final String AUTH_HEADER_KEY = "Authorization";
	private static final String USER_AGENT_HEADER_KEY = "User-Agent";
	private static final String ACCEPT_HEADER_KEY = "Accept";
	private static final String ACCEPT_HEADER_VALUE = "application/vnd.github.v3+json";
	private static final String DEV_SEARCH_TOOL_APP_NAME = "Dev Search Tool";
	
	private final AuthCredentials credentials;
	
	public Connection(AuthCredentials credentials) {
		this.credentials = credentials;
	}
	
	// A small helper routine to construct get requests for GitHub API.
	private HttpGet buildAPIGetRequest(URI uri) {
		HttpGet get = new HttpGet(uri);
		
		// GitHub documentation suggests 'Accept' header be included in requests.
		get.addHeader(ACCEPT_HEADER_KEY, ACCEPT_HEADER_VALUE);
		
		// User-Agent can't be empty for github requests.
		get.addHeader(USER_AGENT_HEADER_KEY, DEV_SEARCH_TOOL_APP_NAME);
		
		// Authentication is done via passing the Authorization header.
		AuthScheme scheme = credentials.getAuthScheme();
		if (scheme == AuthScheme.BASIC || scheme == AuthScheme.OAUTH2) {
			get.addHeader(AUTH_HEADER_KEY, credentials.getAuthHeader());
		}
		return get;
	}
	
	/**
	 * The primary interface of this class. Given an encoded url, it makes a request to the server
	 * and parses the output(json) into a {@link JSONObject} which can then be converted to application objects.
	 *   
	 * @param url An api end point url with special chars encoded.
	 * 
	 * @return Received response(json) as a JsonObject.
	 * 
	 * @throws IOException
	 */
	public JSONObject getResponse(URI url) throws IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse resp = null;
		HttpGet get = buildAPIGetRequest(url);
		
		JSONObject obj = new JSONObject();
		try {
			resp = client.execute(get);
			int respCode = resp.getStatusLine().getStatusCode();
			obj.put(RESP_CODE_KEY, respCode);
			obj.put(RESP_DATA_KEY, new JSONObject(readStringFromEntity(resp.getEntity())));
			return obj;
		} catch (IOException e) {
			if (resp != null) {
				resp.close();
			}
		}
		return null;
	}
	
	/**
	 * Similar to {@link #getResponse(URI)}, this method parses the json response into a JsonArray object.
	 * This is used in cases where the response is an array of elements, e.g., when we are listing a user's repos.
	 *   
	 * @param url An api end point url with special chars encoded.
	 * 
	 * @return Received response(json) as a JsonArray.
	 * 
	 * @throws IOException
	 */
	public JSONObject getSequence(URI uri) throws IOException {
		CloseableHttpClient client = HttpClients.createDefault();
		CloseableHttpResponse resp = null;
		HttpGet get = buildAPIGetRequest(uri);
		
		JSONObject obj = new JSONObject();
		try {
			resp = client.execute(get);
			int respCode = resp.getStatusLine().getStatusCode();
			obj.put(RESP_CODE_KEY, respCode);

			if (respCode == RESP_CODE_OK) {
				String str = readStringFromEntity(resp.getEntity());
				obj.put(RESP_DATA_KEY, new JSONArray(str));
			}
			
			return obj;
		} catch (IOException e) {
			if (resp != null) {
				resp.close();
			}
		}
		return null;
	}
	
	/** Reads the entire response body and constructs a string from it. */
	private String readStringFromEntity(HttpEntity entity) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
		StringBuilder builder = new StringBuilder();
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			
			builder.append(line + "\n");
		}
		
		reader.close();
		return builder.toString();
	}
}
