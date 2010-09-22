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

import org.springframework.integration.MessageChannel;

/**
 * Interface for any MessageChannel implementation that accepts subscribers.
 * The subscribers must implement the {@link MessageHandler} interface and
 * will be invoked when a Message is available.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public interface SubscribableChannel<T> extends MessageChannel<T> {

	/**
	 * Register a {@link MessageHandler} as a subscriber to this channel.
	 */
	boolean subscribe(MessageHandler handler);

	/**
	 * Remove a {@link MessageHandler} from the subscribers of this channel.
	 */
	boolean unsubscribe(MessageHandler handler);

}
