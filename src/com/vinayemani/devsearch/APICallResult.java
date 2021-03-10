package com.vinayemani.devsearch;


/**
 * When we make an api call to the server, one of the following things can happen.
 * 
 * Success, we successfully received at least one matching result.
 * No match, there were no matching results.
 * Rate limit exceeded, we exceeded the rate limit and must now wait until next reset time.
 * Error, some other error occurred.
 * 
 * APICallResultType is an enum representing all these cases.
 * 
 * @author Vinay E.
 *
 */
enum APICallResultType {
	SUCCESS,
	NO_MATCH,
	RATE_LIMIT_EXCEEDED,
	ERROR
}

/**
 * APICallResult represents either a successfully fetched response object or a failure 
 * that occurred during the api request. 
 * 
 * @author Vinay E.
 */
@lombok.Getter
class APICallResult<Output> {
	private APICallResultType resultType;
	private Output result;
	
	private APICallResult(APICallResultType resultType, Output result) {
		this.resultType = resultType;
		this.result = result;
	}
	
	private APICallResult(APICallResultType type) {
		this(type, null);
	}
	
	public static <Item> APICallResult<Item> successResult(Item item) {
		return new APICallResult<Item>(APICallResultType.SUCCESS, item);
	}
	
	public static <Item> APICallResult<Item> rateLimitExceededResult() {
		return new APICallResult<>(APICallResultType.RATE_LIMIT_EXCEEDED);
	}
	
	public static <Item> APICallResult<Item> noMatchResult() {
		return new APICallResult<>(APICallResultType.NO_MATCH);
	}
	
	public static <Item> APICallResult<Item> errorResult() {
		return new APICallResult<>(APICallResultType.ERROR);
	}
}
