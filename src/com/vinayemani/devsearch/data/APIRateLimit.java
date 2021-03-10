package com.vinayemani.devsearch.data;

import org.json.JSONObject;

/**
 * APIRateLimit represents the rate limit status for a user at a single point in time.  
 * 
 * @author Vinay E.
 *
 */
@lombok.Getter
public class APIRateLimit {
	private final long resetTime;
	private final int requestsLeft;
	private final int requestsMax;
	private final int resetPeriodSecs;
	
	private static final String RESET_KEY = "reset";
	private static final String LIMIT_KEY = "limit";
	private static final String REMAINING_KEY = "remaining";
	
	private APIRateLimit(long resetTime, int resetPeriodSecs, int left, int max) {
		this.resetTime = resetTime;
		this.requestsLeft = left;
		this.requestsMax = max;
		this.resetPeriodSecs = resetPeriodSecs;
	}
	
	/** Parses a json object into an APIRateLimit object */
	public static APIRateLimit fromJSONObject(JSONObject obj, int resetPeriod) {
		long resetTime = obj.getLong(RESET_KEY);
		int requestsLeft = obj.getInt(REMAINING_KEY);
		int requestsMax = obj.getInt(LIMIT_KEY);
		return new APIRateLimit(resetTime, resetPeriod, requestsLeft, requestsMax);
	}
	
	public String toString() {
		return String.format("nextResetTime=%d, requestsLeft=%d, requestsMax=%d, resetPeriod=%d", 
				resetTime, requestsLeft, requestsMax, resetPeriodSecs);
	}
}
