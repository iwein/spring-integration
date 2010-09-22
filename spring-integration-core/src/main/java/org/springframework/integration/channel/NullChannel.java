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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;

/**
 * A channel implementation that essentially behaves like "/dev/null".
 * All receive() calls will return <em>null</em>, and all send() calls
 * will return <em>true</em> although no action is performed.
 * Note however that the invocations are logged at debug-level.
 * 
 * @author Mark Fisher
 */
public  class NullChannel<T> implements PollableChannel<T> {

	private final Log logger = LogFactory.getLog(this.getClass());

	public boolean send(Message<? extends T> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("message sent to null channel: " + message);
		}
		return true;
	}

	public boolean send(Message<? extends T> message, long timeout) {
		return this.send(message);
	}

	public Message<T> receive() {
		if (logger.isDebugEnabled()) {
			logger.debug("receive called on null channel");
		}		
		return null;
	}

	public Message<T> receive(long timeout) {
		return this.receive();
	}

}
