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

package org.springframework.integration.transformer;

import org.springframework.integration.Message;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

import java.util.UUID;

/**
 * Transformer that accepts a Message whose payload is a UUID and retrieves the Message associated
 * with that id from a MessageStore if available. An Exception will be thrown if no Message with
 * that ID can be retrieved from the given MessageStore.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ClaimCheckOutTransformer extends AbstractTransformer {

	private final MessageStore messageStore;


	/**
	 * Create a claim check-out transformer that will delegate to the provided MessageStore.
	 */
	public ClaimCheckOutTransformer(MessageStore messageStore) {
		Assert.notNull(messageStore, "MessageStore must not be null");
		this.messageStore = messageStore;
	}


	@Override
	protected Object doTransform(Message message) throws Exception {
		Assert.notNull(message, "message must not be null");
		Assert.isTrue(message.getPayload() instanceof UUID, "payload must be a UUID");
		UUID id = (UUID) message.getPayload();
		Message<?> retrievedMessage = this.messageStore.getMessage(id);
		Assert.notNull(retrievedMessage, "unable to locate Message for ID: " + id
				+ " within MessageStore [" + this.messageStore + "]");
		MessageBuilder<?> responseBuilder = MessageBuilder.fromMessage(retrievedMessage);
		// headers on the 'current' message take precedence
		responseBuilder.copyHeaders(message.getHeaders());
		return responseBuilder.build();
	}

}
