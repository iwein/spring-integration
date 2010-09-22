/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSelector;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple implementation of a message channel. Each {@link Message} is placed in
 * a {@link BlockingQueue} whose capacity may be specified upon construction.
 * The capacity must be a positive integer value. For a zero-capacity version
 * based upon a {@link java.util.concurrent.SynchronousQueue}, consider the
 * {@link RendezvousChannel}.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Iwein Fuld
 */
public class QueueChannel<T> extends AbstractPollableChannel<T> {

	private final BlockingQueue<Message<T>> queue;

	/**
	 * Create a channel with the specified queue.
	 */
	public QueueChannel(BlockingQueue<Message<T>> queue) {
		Assert.notNull(queue, "'queue' must not be null");
		this.queue = queue;
	}

	/**
	 * Create a channel with the specified queue capacity.
	 */
	public QueueChannel(int capacity) {
		Assert.isTrue(capacity > 0, "The capacity must be a positive integer. " +
				"For a zero-capacity alternative, consider using a 'RendezvousChannel'.");
		this.queue = new LinkedBlockingQueue<Message<T>>(capacity);
	}

	/**
	 * Create a channel with "unbounded" queue capacity. The actual capacity value is
	 * {@link Integer#MAX_VALUE}. Note that a bounded queue is recommended, since an
	 * unbounded queue may lead to OutOfMemoryErrors.
	 */
	public QueueChannel() {
		this(new LinkedBlockingQueue<Message<T>>());
	}

	protected boolean doSend(Message<? extends T> message, long timeout) {
		Assert.notNull(message, "'message' must not be null");
		Message<T> castMessage = (Message<T>) message;
		try {
			if (timeout > 0) {
				return this.queue.offer(castMessage, timeout, TimeUnit.MILLISECONDS);
			}
			if (timeout == 0) {
				return this.queue.offer(castMessage);
			}
			queue.put(castMessage);
			return true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	protected Message<T> doReceive(long timeout) {
		try {
			if (timeout > 0) {
				return queue.poll(timeout, TimeUnit.MILLISECONDS);
			}
			if (timeout == 0) {
				return queue.poll();
			}
			return queue.take();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	/**
	 * Remove all {@link Message Messages} from this channel.
	 */
	public List<Message<?>> clear() {
		List<Message<?>> clearedMessages = new ArrayList<Message<?>>();
		this.queue.drainTo(clearedMessages);
		return clearedMessages;
	}

	/**
	 * Remove any {@link Message Messages} that are not accepted by the provided selector.
	 */
	public List<Message<?>> purge(MessageSelector selector) {
		if (selector == null) {
			return this.clear();
		}
		List<Message<?>> purgedMessages = new ArrayList<Message<?>>();
		Object[] array = this.queue.toArray();
		for (Object o : array) {
			Message<?> message = (Message<?>) o;
			if (!selector.accept(message) && this.queue.remove(message)) {
				purgedMessages.add(message);
			}
		}
		return purgedMessages;
	}

	public int getQueueSize() {
		return this.queue.size();
	}

	public int getRemainingCapacity() {
		return this.queue.remainingCapacity();
	}
}
