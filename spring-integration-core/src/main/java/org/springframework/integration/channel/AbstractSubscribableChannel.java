/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.util.Assert;

/**
 * Base implementation of {@link MessageChannel} that invokes the subscribed
 * {@link MessageHandler handler(s)} by delegating to a {@link MessageDispatcher}.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractSubscribableChannel<T> extends AbstractMessageChannel<T> implements SubscribableChannel<T> {

	public boolean subscribe(MessageHandler handler) {
		return this.getRequiredDispatcher().addHandler(handler);
	}

	public boolean unsubscribe(MessageHandler handle) {
		return this.getRequiredDispatcher().removeHandler(handle);
	}

	@Override
	protected boolean doSend(Message<? extends T> message, long timeout) {
		return this.getRequiredDispatcher().dispatch(message);
	}

	private MessageDispatcher getRequiredDispatcher() {
		MessageDispatcher dispatcher = this.getDispatcher();
		Assert.state(dispatcher != null, "'dispatcher' must not be null");
		return dispatcher;
	}

	protected abstract MessageDispatcher getDispatcher();

}
