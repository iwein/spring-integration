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

package org.springframework.integration.channel.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.core.MessageSelector;
import org.springframework.util.Assert;

/**
 * A {@link ChannelInterceptor} that publishes a copy of the intercepted message
 * to a secondary target while still sending the original message to the main channel.
 * 
 * @author Mark Fisher
 */
public class WireTap extends ChannelInterceptorAdapter implements Lifecycle {

	private static final Log logger = LogFactory.getLog(WireTap.class);

	private final MessageChannel channel;

	private volatile long timeout = 0;

	private final MessageSelector selector;

	private volatile boolean running = true;


	/**
	 * Create a new wire tap with <em>no</em> {@link MessageSelector}.
	 * 
	 * @param channel the MessageChannel to which intercepted messages will be sent
	 */
	public WireTap(MessageChannel channel) {
		this(channel, null);
	}

	/**
	 * Create a new wire tap with the provided {@link MessageSelector}.
	 * 
	 * @param channel the channel to which intercepted messages will be sent
	 * @param selector the selector that must accept a message for it to be
	 * sent to the intercepting channel
	 */
	public WireTap(MessageChannel channel, MessageSelector selector) {
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;
		this.selector = selector;
	}


	/**
	 * Specify the timeout value for sending to the intercepting target.
	 * 
	 * @param timeout the timeout in milliseconds
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Check whether the wire tap is currently running.
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Restart the wire tap if it has been stopped. It is running by default.
	 */
	public void start() {
		this.running = true;
	}

	/**
	 * Stop the wire tap. To restart, invoke {@link #start()}.
	 */
	public void stop() {
		this.running = false;
	}

	/**
	 * Intercept the Message and, <em>if accepted</em> by the {@link MessageSelector},
	 * send it to the secondary target. If this wire tap's {@link MessageSelector} is
	 * <code>null</code>, it will accept all messages.
	 */
	@Override
	public <T> Message<T>  preSend(Message<T>  message, MessageChannel<T> channel) {
		if (this.running && (this.selector == null || this.selector.accept(message))) {
			boolean sent = (this.timeout >= 0)
					? this.channel.send(message, this.timeout)
					: this.channel.send(message);
			if (!sent && logger.isWarnEnabled()) {
				logger.warn("failed to send message to WireTap channel '" + this.channel + "'");
			}
		}
		return message;
	}

}
