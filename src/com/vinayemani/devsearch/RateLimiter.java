package com.vinayemani.devsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vinayemani.devsearch.WorkQueue.Worker;
import com.vinayemani.devsearch.WorkQueue.QueueFinisher;
import com.vinayemani.devsearch.WorkQueue.RateLimitFetcher;
import com.vinayemani.devsearch.data.APIRateLimit;
import com.vinayemani.devsearch.data.UserData;
import com.vinayemani.devsearch.data.UserKey;

/**
 * RateLimiter implements the rate limiting logic required to make efficient use of api calls while
 * honoring rate limits.
 * 
 * GitHub API has 2 separate rate limits for its search and core apis. The usual pattern is to search for a user
 * first using a search api call and then retrieve her profile data using core api calls. If there is a large number of
 * users to search for, we can run these 2 types of queries in parallel in 2 threads, so whenever a particular rate limit
 * is reached, only that thread is blocked, while the other thread can continue. 
 *  
 * It uses two WorkQueues(a thread and a queue) to run core and search queries. When a user search is finished, the best 
 * match is then pushed to the core queue to retrieve its data.
 * 
 * @author Vinay E.
 *
 */
class RateLimiter {
	public RateLimiter(Connection conn) {
		this.conn = conn;
	}
	
	public Map<Long, UserData> getSearchResults(List<UserKey> users) {
		// Maintain a map of collected results.
		Map<Long, UserData> results = new HashMap<>();
				
		// Initialize the work queues.
		WorkQueue<String, UserData> coreQ = new WorkQueue<>("core", new Worker<String, UserData>() {
			@Override
			public APICallResult<UserData> produce(String userLogin) {
				return GitHubAPIClient.fetchSingleUserData(userLogin, conn);
			}
			
			@Override
			public void onSuccess(long keyId, UserData output) {
				results.put(keyId, output);
			}
		});
		
		coreQ.setFinisher(new QueueFinisher() {
			@Override
			public void onQueueFinished() {}
		});
		
		coreQ.setRateLimitFetcher(new RateLimitFetcher() {
			@Override
			public APIRateLimit fetchRateLimit() {
				APIRateLimit[] limits = GitHubAPIClient.getRateLimits(conn);
				return limits[0];
			}
		});
		
		WorkQueue<UserKey, String> searchQ = new WorkQueue<>("search", new Worker<UserKey, String>() {
			@Override
			public APICallResult<String> produce(UserKey input) {
				return GitHubAPIClient.searchForSingleUser(input, conn);
			}
			
			@Override
			public void onSuccess(long keyId, String output) {
				coreQ.pushNewJob(keyId, output);
			}
		});
		
		searchQ.setFinisher(new QueueFinisher() {			
			@Override
			public void onQueueFinished() {
				coreQ.signalEndOfJobs();
			}
		});
		
		searchQ.setRateLimitFetcher(new RateLimitFetcher() {
			@Override
			public APIRateLimit fetchRateLimit() {
				APIRateLimit[] limits = GitHubAPIClient.getRateLimits(conn);
				return limits[1];
			}
		});
		
		// Push user search keys onto search queue.
		long keyId = 0;
		for (UserKey user : users) {
			searchQ.pushNewJob(keyId, user);
			keyId++;
		}
		// signal end of jobs to the search queue.
		searchQ.signalEndOfJobs();
		
		// Wait until core queue finishes all its jobs.
		coreQ.waitUntilFinish();
		
		// return the accumulated results.
		return results;
	}
	
	private Connection conn;
}
