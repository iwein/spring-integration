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

package org.springframework.integration.support;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 */
public final class MessageBuilder<T> {

	private final T payload;

	private final Map<String, Object> headers = new HashMap<String, Object>();

	private final Message<T> originalMessage;

	private volatile boolean modified;

	/**
	 * Private constructor to be invoked from the static factory methods only.
	 */
	private MessageBuilder(T payload, Message<T> originalMessage) {
		Assert.notNull(payload, "payload must not be null");
		this.payload = payload;
		this.originalMessage = originalMessage;
		if (originalMessage != null) {
			this.copyHeaders(originalMessage.getHeaders());
		}
	}

	/**
	 * Create a builder for a new {@link Message} instance pre-populated with all of the headers copied from the
	 * provided message. The payload of the provided Message will also be used as the payload for the new message.
	 * 
	 * @param message the Message from which the payload and all headers will be copied
	 */
	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		Assert.notNull(message, "message must not be null");
		MessageBuilder<T> builder = new MessageBuilder<T>(message.getPayload(), message);
		return builder;
	}

	/**
	 * Create a builder for a new {@link Message} instance with the provided payload.
	 * 
	 * @param payload the payload for the new message
	 */
	public static <T> MessageBuilder<T> withPayload(T payload) {
		MessageBuilder<T> builder = new MessageBuilder<T>(payload, null);
		return builder;
	}

	/**
	 * Set the value for the given header name. If the provided value is <code>null</code>, the header will be removed.
	 */
	public MessageBuilder<T> setHeader(String headerName, Object headerValue) {
		if (StringUtils.hasLength(headerName) && !headerName.equals(MessageHeaders.ID)
				&& !headerName.equals(MessageHeaders.TIMESTAMP)) {
			this.verifyType(headerName, headerValue);
			this.modified = true;
			if (headerValue == null) {
				this.headers.remove(headerName);
			}
			else {
				this.headers.put(headerName, headerValue);
			}
		}
		return this;
	}

	/**
	 * Set the value for the given header name only if the header name is not already associated with a value.
	 */
	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		if (this.headers.get(headerName) == null) {
			this.setHeader(headerName, headerValue);
		}
		return this;
	}

	/**
	 * Remove the value for the given header name.
	 */
	public MessageBuilder<T> removeHeader(String headerName) {
		if (StringUtils.hasLength(headerName) && !headerName.equals(MessageHeaders.ID)
				&& !headerName.equals(MessageHeaders.TIMESTAMP)) {
			this.modified = true;
			this.headers.remove(headerName);
		}
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will overwrite any existing values. Use {
	 * {@link #copyHeadersIfAbsent(Map)} to avoid overwriting values. Note that the 'id' and 'timestamp' header values
	 * will never be overwritten.
	 * 
	 * @see MessageHeaders#ID
	 * @see MessageHeaders#TIMESTAMP
	 */
	public MessageBuilder<T> copyHeaders(Map<String, ?> headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			this.setHeader(key, headersToCopy.get(key));
		}
		return this;
	}

	/**
	 * Copy the name-value pairs from the provided Map. This operation will <em>not</em> overwrite any existing values.
	 */
	public MessageBuilder<T> copyHeadersIfAbsent(Map<String, Object> headersToCopy) {
		Set<String> keys = headersToCopy.keySet();
		for (String key : keys) {
			this.setHeaderIfAbsent(key, headersToCopy.get(key));
		}
		return this;
	}

	public MessageBuilder<T> setExpirationDate(Long expirationDate) {
		return this.setHeader(MessageHeaders.EXPIRATION_DATE, expirationDate);
	}

	public MessageBuilder<T> setExpirationDate(Date expirationDate) {
		if (expirationDate != null) {
			return this.setHeader(MessageHeaders.EXPIRATION_DATE, expirationDate.getTime());
		}
		else {
			return this.setHeader(MessageHeaders.EXPIRATION_DATE, null);
		}
	}

	public MessageBuilder<T> setCorrelationId(Object correlationId) {
		return this.setHeader(MessageHeaders.CORRELATION_ID, correlationId);
	}

	public MessageBuilder<T> pushSequenceDetails(Object correlationId, int sequenceNumber, int sequenceSize) {
		Object incomingCorrelationId = headers.get(MessageHeaders.CORRELATION_ID);
		@SuppressWarnings("unchecked")
		List<List<Object>> incomingSequenceDetails = (List<List<Object>>) headers.get(MessageHeaders.SEQUENCE_DETAILS);
		if (incomingCorrelationId != null) {
			if (incomingSequenceDetails == null) {
				incomingSequenceDetails = new ArrayList<List<Object>>();
			}
			else {
				incomingSequenceDetails = new ArrayList<List<Object>>(incomingSequenceDetails);
			}
			incomingSequenceDetails.add(Arrays.asList(incomingCorrelationId, headers
					.get(MessageHeaders.SEQUENCE_NUMBER), headers.get(MessageHeaders.SEQUENCE_SIZE)));
			incomingSequenceDetails = Collections.unmodifiableList(incomingSequenceDetails);
		}
		if (incomingSequenceDetails != null) {
			setHeader(MessageHeaders.SEQUENCE_DETAILS, incomingSequenceDetails);
		}
		return setCorrelationId(correlationId).setSequenceNumber(sequenceNumber).setSequenceSize(sequenceSize);
	}

	public MessageBuilder<T> popSequenceDetails() {
		String key = MessageHeaders.SEQUENCE_DETAILS;
		if (!headers.containsKey(key)) {
			return this;
		}
		@SuppressWarnings("unchecked")
		List<List<Object>> incomingSequenceDetails = new ArrayList<List<Object>>((List<List<Object>>) headers.get(key));
		List<Object> sequenceDetails = incomingSequenceDetails.remove(incomingSequenceDetails.size() - 1);
		Assert.state(sequenceDetails.size() == 3, "Wrong sequence details (not created by MessageBuilder?): "
				+ sequenceDetails);
		setCorrelationId(sequenceDetails.get(0));
		Integer sequenceNumber = (Integer) sequenceDetails.get(1);
		Integer sequenceSize = (Integer) sequenceDetails.get(2);
		if (sequenceSize > 0) {
			setSequenceNumber(sequenceNumber);
			setSequenceSize(sequenceSize);
		}
		if (!incomingSequenceDetails.isEmpty()) {
			headers.put(MessageHeaders.SEQUENCE_DETAILS, incomingSequenceDetails);
		}
		return this;
	}

	public MessageBuilder<T> setReplyChannel(MessageChannel replyChannel) {
		return this.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel);
	}

	public MessageBuilder<T> setReplyChannelName(String replyChannelName) {
		return this.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannelName);
	}

	public MessageBuilder<T> setErrorChannel(MessageChannel errorChannel) {
		return this.setHeader(MessageHeaders.ERROR_CHANNEL, errorChannel);
	}

	public MessageBuilder<T> setErrorChannelName(String errorChannelName) {
		return this.setHeader(MessageHeaders.ERROR_CHANNEL, errorChannelName);
	}

	public MessageBuilder<T> setSequenceNumber(Integer sequenceNumber) {
		return this.setHeader(MessageHeaders.SEQUENCE_NUMBER, sequenceNumber);
	}

	public MessageBuilder<T> setSequenceSize(Integer sequenceSize) {
		return this.setHeader(MessageHeaders.SEQUENCE_SIZE, sequenceSize);
	}

	public MessageBuilder<T> setPriority(Integer priority) {
		return this.setHeader(MessageHeaders.PRIORITY, priority);
	}

	@SuppressWarnings("unchecked")
	public Message<T> build() {
		if (!this.modified && this.originalMessage != null) {
			return this.originalMessage;
		}
		if (this.payload instanceof Throwable) {
			return (Message<T>) new ErrorMessage((Throwable) this.payload, this.headers);
		}
		return new GenericMessage<T>(this.payload, this.headers);
	}

	private void verifyType(String headerName, Object headerValue) {
		if (headerName != null && headerValue != null) {
			if (MessageHeaders.ID.equals(headerName)) {
				Assert.isTrue(headerValue instanceof UUID, "The '" + headerName + "' header value must be a UUID.");
			}
			else if (MessageHeaders.TIMESTAMP.equals(headerName)) {
				Assert.isTrue(headerValue instanceof Long, "The '" + headerName + "' header value must be a Long.");
			}
			else if (MessageHeaders.EXPIRATION_DATE.equals(headerName)) {
				Assert.isTrue(headerValue instanceof Date || headerValue instanceof Long, "The '" + headerName
						+ "' header value must be a Date or Long.");
			}
			else if (MessageHeaders.ERROR_CHANNEL.equals(headerName)
					|| MessageHeaders.REPLY_CHANNEL.endsWith(headerName)) {
				Assert.isTrue(headerValue instanceof MessageChannel || headerValue instanceof String, "The '"
						+ headerName + "' header value must be a MessageChannel or String.");
			}
			else if (MessageHeaders.SEQUENCE_NUMBER.equals(headerName)
					|| MessageHeaders.SEQUENCE_SIZE.equals(headerName)) {
				Assert.isTrue(Integer.class.isAssignableFrom(headerValue.getClass()), "The '" + headerName
						+ "' header value must be an Integer.");
			}
		}
	}


}
