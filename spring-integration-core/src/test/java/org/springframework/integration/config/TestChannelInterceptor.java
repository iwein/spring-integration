/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;

/**
 * @author Mark Fisher
 */
public class TestChannelInterceptor extends ChannelInterceptorAdapter {

	private final AtomicInteger sendCount = new AtomicInteger();

	private final AtomicInteger receiveCount = new AtomicInteger();


	@Override
	public Message preSend(Message message, MessageChannel channel) {
		sendCount.incrementAndGet();
		return message;
	}

	@Override
	public Message postReceive(Message message, MessageChannel channel) {
		receiveCount.incrementAndGet();
		return message;
	}

	public int getSendCount() {
		return this.sendCount.get();
	}

	public int getReceiveCount() {
		return this.receiveCount.get();
	}

}
