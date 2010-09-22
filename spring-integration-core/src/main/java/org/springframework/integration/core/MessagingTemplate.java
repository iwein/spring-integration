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

package org.springframework.integration.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.*;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.ChannelResolutionException;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.integration.support.converter.MessageConverter;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This is the central class for invoking message exchange operations across
 * {@link MessageChannel}s. It supports one-way send and receive calls as well
 * as request/reply.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MessagingTemplate implements MessagingOperations, BeanFactoryAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageChannel defaultChannel;

	private volatile ChannelResolver channelResolver;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile long sendTimeout = -1;

	private volatile long receiveTimeout = -1;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	/**
	 * Create a MessagingTemplate with no default channel. Note, that one
	 * may be provided by invoking {@link #setDefaultChannel(MessageChannel)}.
	 */
	public MessagingTemplate() {
	}

	/**
	 * Create a MessagingTemplate with the given default channel.
	 */
	public MessagingTemplate(MessageChannel defaultChannel) {
		this.defaultChannel = defaultChannel;
	}


	/**
	 * Specify the default MessageChannel to use when invoking the send and/or
	 * receive methods that do not expect a channel parameter.
	 */
	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	/**
	 * Set the {@link ChannelResolver} that is to be used to resolve
	 * {@link MessageChannel} references for this template.
	 * <p>When running within an application context, the default resolver is a
	 * {@link BeanFactoryChannelResolver}.
	 */
	public void setChannelResolver(ChannelResolver channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.channelResolver = channelResolver;
	}

	/**
	 * Set the {@link MessageConverter} that is to be used to convert
	 * between Messages and objects for this template.
	 * <p>The default is {@link SimpleMessageConverter}.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * Specify the timeout value to use for send operations.
	 * 
	 * @param sendTimeout the send timeout in milliseconds
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * Specify the timeout value to use for receive operations.
	 *  
	 * @param receiveTimeout the receive timeout in milliseconds
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.channelResolver == null && beanFactory != null) {
			this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		}
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			this.initialized = true;
		}
	}

	public <P> void send(final Message<P> message) {
		this.send(this.getRequiredDefaultChannel(), message);
	}

	public <P> void send(final MessageChannel channel, final Message<P> message) {
		this.doSend(channel, message);
	}

	public <P> void send(final String channelName, final Message<P> message) {
		this.send(this.resolveChannelName(channelName), message);
	}

	public <T> void convertAndSend(T object) {
		Message<?> message = this.messageConverter.toMessage(object);
		if (message != null) {
			this.send(message);
		}
	}

	public <T> void convertAndSend(MessageChannel channel, T object) {
		Message<?> message = this.messageConverter.toMessage(object);
		if (message != null) {
			this.send(channel, message);
		}
	}

	public <T> void convertAndSend(String channelName, T object) {
		Message<?> message = this.messageConverter.toMessage(object);
		if (message != null) {
			this.send(channelName, message);
		}
	}

	public <T> void convertAndSend(T object, MessagePostProcessor postProcessor) {
		Message<?> message = this.messageConverter.toMessage(object);
		message = postProcessor.postProcessMessage(message);
		if (message != null) {
			this.send(message);
		}
	}

	public <T> void convertAndSend(MessageChannel channel, T object, MessagePostProcessor postProcessor) {
		Message<?> message = this.messageConverter.toMessage(object);
		message = postProcessor.postProcessMessage(message);
		if (message != null) {
			this.send(channel, message);
		}
	}

	public <T> void convertAndSend(String channelName, T object, MessagePostProcessor postProcessor) {
		Message<?> message = this.messageConverter.toMessage(object);
		message = postProcessor.postProcessMessage(message);
		if (message != null) {
			this.send(channelName, message);
		}
	}

	public <P> Message<P> receive() {
		MessageChannel channel = this.getRequiredDefaultChannel();
		Assert.state(channel instanceof PollableChannel,
				"The 'defaultChannel' must be a PollableChannel for receive operations.");
		return this.receive((PollableChannel) channel);
	}

	public <P> Message<P> receive(final PollableChannel channel) {
		return this.doReceive(channel);
	}

	public <P> Message<P> receive(String channelName) {
		MessageChannel channel = this.resolveChannelName(channelName);
		Assert.isInstanceOf(PollableChannel.class, channel,
				"A PollableChannel is required for receive operations. ");
		return this.receive((PollableChannel) channel);
	}

	public Object receiveAndConvert() throws MessagingException {
		Message<?> message = this.receive();
		return (message != null) ? this.messageConverter.fromMessage(message) : null;
	}

	public Object receiveAndConvert(PollableChannel channel) throws MessagingException {
		Message<?> message = this.receive(channel);
		return (message != null) ? this.messageConverter.fromMessage(message) : null;
	}

	public Object receiveAndConvert(String channelName) throws MessagingException {
		Message<?> message = this.receive(channelName);
		return (message != null) ? this.messageConverter.fromMessage(message) : null;
	}

	public Message<?> sendAndReceive(final Message<?> requestMessage) {
		return this.sendAndReceive(this.getRequiredDefaultChannel(), requestMessage);
	}

	public Message<?> sendAndReceive(final MessageChannel channel, final Message<?> requestMessage) {
		return this.doSendAndReceive(channel, requestMessage);
	}

	public Message<?> sendAndReceive(final String channelName, final Message<?> requestMessage) {
		return this.sendAndReceive(this.resolveChannelName(channelName), requestMessage);
	}

	public Object convertSendAndReceive(final Object request) {
		Message<?> requestMessage = this.messageConverter.toMessage(request);
		Message<?> replyMessage = this.sendAndReceive(requestMessage);
		return this.messageConverter.fromMessage(replyMessage);
	}

	public Object convertSendAndReceive(final MessageChannel channel, final Object request) {
		Message<?> requestMessage = this.messageConverter.toMessage(request);
		Message<?> replyMessage = this.sendAndReceive(channel, requestMessage);
		return this.messageConverter.fromMessage(replyMessage);
	}

	public Object convertSendAndReceive(final String channelName, final Object request) {
		Message<?> requestMessage = this.messageConverter.toMessage(request);
		Message<?> replyMessage = this.sendAndReceive(channelName, requestMessage);
		return this.messageConverter.fromMessage(replyMessage);
	}

	public Object convertSendAndReceive(final Object request, MessagePostProcessor requestPostProcessor) {
		Message<?> requestMessage = this.messageConverter.toMessage(request);
		requestMessage = requestPostProcessor.postProcessMessage(requestMessage);
		Message<?> replyMessage = this.sendAndReceive(requestMessage);
		return this.messageConverter.fromMessage(replyMessage);
	}

	public Object convertSendAndReceive(final MessageChannel channel, final Object request, MessagePostProcessor requestPostProcessor) {
		Message<?> requestMessage = this.messageConverter.toMessage(request);
		requestMessage = requestPostProcessor.postProcessMessage(requestMessage);
		Message<?> replyMessage = this.sendAndReceive(channel, requestMessage);
		return this.messageConverter.fromMessage(replyMessage);
	}

	public Object convertSendAndReceive(final String channelName, final Object request, MessagePostProcessor requestPostProcessor) {
		Message<?> requestMessage = this.messageConverter.toMessage(request);
		requestMessage = requestPostProcessor.postProcessMessage(requestMessage);
		Message<?> replyMessage = this.sendAndReceive(channelName, requestMessage);
		return this.messageConverter.fromMessage(replyMessage);
	}

	private <T> void  doSend(MessageChannel<T> channel, Message<T> message) {
		Assert.notNull(channel, "channel must not be null");
		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0)
				? channel.send(message, timeout)
				: channel.send(message);
		if (!sent) {
			throw new MessageDeliveryException(message,
					"failed to send message to channel '" + channel + "' within timeout: " + timeout);
		}
	}

	@SuppressWarnings("unchecked")
	private <P> Message<P> doReceive(PollableChannel channel) {
		Assert.notNull(channel, "channel must not be null");
		long timeout = this.receiveTimeout;
		Message<?> message = (timeout >= 0)
				? channel.receive(timeout)
				: channel.receive();
		if (message == null && this.logger.isTraceEnabled()) {
			this.logger.trace("failed to receive message from channel '" + channel + "' within timeout: " + timeout);
		}
		return (Message<P>) message;
	}

	private <S, R> Message<R> doSendAndReceive(MessageChannel channel, Message<S> requestMessage) {
		Object originalReplyChannelHeader = requestMessage.getHeaders().getReplyChannel();
		Object originalErrorChannelHeader = requestMessage.getHeaders().getErrorChannel();
		TemporaryReplyChannel replyChannel = new TemporaryReplyChannel(this.receiveTimeout);
		requestMessage = MessageBuilder.fromMessage(requestMessage)
				.setReplyChannel(replyChannel)
				.setErrorChannel(replyChannel)
				.build();
		this.doSend(channel, requestMessage);
		Message<R> reply = this.doReceive(replyChannel);
		if (reply != null) {
			reply = MessageBuilder.fromMessage(reply)
					.setHeader(MessageHeaders.REPLY_CHANNEL, originalReplyChannelHeader)
					.setHeader(MessageHeaders.ERROR_CHANNEL, originalErrorChannelHeader)
					.build();
		}
		return reply;
	}

	private MessageChannel getRequiredDefaultChannel() {
		Assert.state(this.defaultChannel != null,
				"No 'defaultChannel' specified for MessagingTemplate. "
				+ "Unable to invoke methods without an explicit channel argument.");
		return this.defaultChannel;
	}

	private ChannelResolver getRequiredChannelResolver() {
		Assert.state(this.channelResolver != null,
				"No 'channelResolver' specified for MessagingTemplate. "
				+ "Unable to invoke methods with a channel name argument.");
		return this.channelResolver;
	}

	/**
	 * Resolve the given channel name into a {@link MessageChannel},
	 * via this template's {@link ChannelResolver} if available.
	 * @param channelName the name of the channel
	 * @return the resolved {@link MessageChannel}
	 * @throws IllegalStateException if this template does not have a ChannelResolver
	 * @throws ChannelResolutionException if the channel name cannot be resolved
	 * @see #setChannelResolver
	 */
	protected MessageChannel resolveChannelName(String channelName) {
		return getRequiredChannelResolver().resolveChannelName(channelName);
	}


	private static class TemporaryReplyChannel<T> implements PollableChannel<T> {

		private volatile Message<T> message;

		private final long receiveTimeout;

		private final CountDownLatch latch = new CountDownLatch(1);


		public TemporaryReplyChannel(long receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}


		public Message<T> receive() {
			return this.receive(-1);
		}

		public Message<T> receive(long timeout) {
			try {
				if (this.receiveTimeout < 0) {
					this.latch.await();
				}
				else {
					this.latch.await(this.receiveTimeout, TimeUnit.MILLISECONDS);
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return this.message;
		}

		public boolean send(Message<? extends T> message) {
			return this.send(message, -1);
		}

		@SuppressWarnings("unchecked")
		public boolean send(Message<? extends T> message, long timeout) {
			this.message = (Message<T>) message;
			this.latch.countDown();
			return true;
		}
	}
}
