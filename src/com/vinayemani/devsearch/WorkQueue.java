package com.vinayemani.devsearch;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import com.vinayemani.devsearch.data.APIRateLimit;

/**
 * WorkQueue processes incoming tasks on a separate thread. It needs to be to told how to process
 * inputs to generate outputs, what to do once an output is successfully generated, how to check
 * for current rate limits for this category etc...
 * 
 * @author Vinay E.
 *
 */
class WorkQueue<Input, Output> {
	
	/**
	 * Worker interface tells the queue how to produce outputs from inputs and what to do when output is
	 * successfully produced.
	 * 
	 * @author Vinay E.
	 */
	interface Worker<Input, Output> {
		APICallResult<Output> produce(Input input);
		void onSuccess(long keyId, Output output);
	}

	/**
	 * QueueFinisher interface tells us what to do when all items have been processed and no new items will be added
	 * by its client. This enables the queue to clean up its resources and quit. 
	 * 
	 * @author Vinay E.
	 *
	 */
	interface QueueFinisher {
		void onQueueFinished();
	}
	
	/**
	 * RateLimitFetcher interfaces tells the queue how to fetch the current rate limits for its category.
	 * 
	 * @author Vinay E.
	 *
	 */
	interface RateLimitFetcher {
		APIRateLimit fetchRateLimit();
	}
	
	/**
	 * Items pushed into WorkQueue will have an associated id with them. Having an id makes it easy to
	 * identify items across queues.
	 * 
	 * @author Vinay E.
	 */
	class KeyedItem<Item> {
		private long keyId;
		private Item item;
		
		public KeyedItem(long keyId, Item item) {
			this.keyId = keyId;
			this.item = item;
		}
	}
	
	public WorkQueue(String category, Worker<Input, Output> worker) {
		this.category = category;
		this.endSignalled = false;
		inputQueue = new LinkedBlockingQueue<>();
		allWorkDone = new Semaphore(0);
		
		// define the work thread and start it.
		workHorse = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					if (inputQueue.isEmpty()) {
						synchronized(this) {
							if (endSignalled) {
								finisher.onQueueFinished();
								allWorkDone.release();
								break;
							}
						}
					}
					
					try {
						KeyedItem<Input> item = inputQueue.take();
						long key = item.keyId;
						APICallResult<Output> output = worker.produce(item.item);
						if (output.getResultType() == APICallResultType.ERROR) {
							// ignore this case.
						} else if (output.getResultType() == APICallResultType.NO_MATCH) {
							// no matching results found for the query. ignore this case.
						} else if (output.getResultType() == APICallResultType.RATE_LIMIT_EXCEEDED) {
							// Rate limit reached, put this item back in the queue and block until next reset.
							inputQueue.put(item);
							APIRateLimit limit = rateLimitFetcher.fetchRateLimit();
							long toWaitMs = limit.getResetTime() * 1000 - System.currentTimeMillis();
							toWaitMs = Math.max(toWaitMs, ONE_SEC_MS);
							Thread.sleep(toWaitMs);
						} else {
							// successful case, process it further.
							Output out = output.getResult();
							worker.onSuccess(key, out);
						}
					} catch (InterruptedException e) {
						// Some error, exit.
						break;
					}
				}
			}
		});
		
		workHorse.start();
	}
	
	public void setFinisher(QueueFinisher finisher) {
		this.finisher = finisher;
	}
	
	public void setRateLimitFetcher(RateLimitFetcher rateLimitFetcher) {
		this.rateLimitFetcher = rateLimitFetcher;
	}
	
	/**
	 * Pushes a new job to be processed onto the queue.
	 * 
	 * @param keyId Id of the item/job being pushed.
	 * @param item Item/job being pushed
	 */
	public void pushNewJob(long keyId, Input item) {
		inputQueue.add(new KeyedItem<Input>(keyId, item));
	}
	
	/**
	 * Called to signal this queue that no more new jobs will be pushed and once 
	 * existing jobs have been processed, the queue can close itself.
	 */
	public void signalEndOfJobs() {
		synchronized(this) {
			endSignalled = true;
		}
	}
	
	/**
	 * Blocks the calling thread until this queue finishes all its jobs.
	 */
	public void waitUntilFinish() {
		try {
			allWorkDone.acquire();
		} catch (InterruptedException e) {}
	}
	
	// This queue blocks for at least one second when it encounters a rate limit exceeded error.
	private static final long ONE_SEC_MS = 60 * 1000;
	
	// These define the behavior of the queue.
	private String category;
	private QueueFinisher finisher;
	private RateLimitFetcher rateLimitFetcher;
	
	// Internal implementation details.
	private boolean endSignalled;
	private BlockingQueue<KeyedItem<Input>> inputQueue;
	private Semaphore allWorkDone;
	private Thread workHorse;
}
