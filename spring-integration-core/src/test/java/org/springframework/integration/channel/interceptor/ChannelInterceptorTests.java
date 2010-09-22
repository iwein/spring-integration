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

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ChannelInterceptorTests {

	private final QueueChannel channel = new QueueChannel();


	@Test
	public void testPreSendInterceptorReturnsMessage() {
		channel.addInterceptor(new PreSendReturnsMessageInterceptor());
		channel.send(new GenericMessage<String>("test"));
		Message<?> result = channel.receive(0);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
		assertEquals(1, result.getHeaders().get(PreSendReturnsMessageInterceptor.class.getSimpleName()));
	}

	@Test
	public void testPreSendInterceptorReturnsNull() {
		PreSendReturnsNullInterceptor interceptor = new PreSendReturnsNullInterceptor();
		channel.addInterceptor(interceptor);
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		assertEquals(1, interceptor.getCount());
		Message<?> result = channel.receive(0);
		assertNull(result);
	}

	@Test
	public void testPostSendInterceptorWithSentMessage() {
		final AtomicBoolean invoked = new AtomicBoolean(false);
		channel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public  void postSend(Message message, MessageChannel channel, boolean sent) {
				assertNotNull(message);
				assertNotNull(channel);
				assertSame(ChannelInterceptorTests.this.channel, channel);
				assertTrue(sent);
				invoked.set(true);
			}
		});
		channel.send(new GenericMessage<String>("test"));
		assertTrue(invoked.get());
	}

	@Test
	public void testPostSendInterceptorWithUnsentMessage() {
		final AtomicInteger invokedCounter = new AtomicInteger(0);
		final AtomicInteger sentCounter = new AtomicInteger(0);
		final QueueChannel singleItemChannel = new QueueChannel(1);
		singleItemChannel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public void postSend(Message message, MessageChannel channel, boolean sent) {
				assertNotNull(message);
				assertNotNull(channel);
				assertSame(singleItemChannel, channel);
				if (sent) {
					sentCounter.incrementAndGet();
				}
				invokedCounter.incrementAndGet();
			}
		});
		assertEquals(0, invokedCounter.get());
		assertEquals(0, sentCounter.get());
		singleItemChannel.send(new GenericMessage<String>("test1"));
		assertEquals(1, invokedCounter.get());
		assertEquals(1, sentCounter.get());
		singleItemChannel.send(new GenericMessage<String>("test2"), 0);
		assertEquals(2, invokedCounter.get());
		assertEquals(1, sentCounter.get());
	}

	@Test
	public void testPreReceiveInterceptorReturnsTrue() {
		channel.addInterceptor(new PreReceiveReturnsTrueInterceptor());
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		Message<?> result = channel.receive(0);
		assertEquals(1, PreReceiveReturnsTrueInterceptor.counter.get());
		assertNotNull(result);		
	}

	@Test
	public void testPreReceiveInterceptorReturnsFalse() {
		channel.addInterceptor(new PreReceiveReturnsFalseInterceptor());
		Message<?> message = new GenericMessage<String>("test");
		channel.send(message);
		Message<?> result = channel.receive(0);
		assertEquals(1, PreReceiveReturnsFalseInterceptor.counter.get());
		assertNull(result);		
	}

	@Test
	public void testPostReceiveInterceptor() {
		final AtomicInteger invokedCount = new AtomicInteger();
		final AtomicInteger messageCount = new AtomicInteger();
		channel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public Message postReceive(Message message, MessageChannel channel) {
				assertNotNull(channel);
				assertSame(ChannelInterceptorTests.this.channel, channel);
				if (message != null) {
					messageCount.incrementAndGet();
				}
				invokedCount.incrementAndGet();
				return message;
			}
		});
		channel.receive(0);
		assertEquals(1, invokedCount.get());
		assertEquals(0, messageCount.get());
		channel.send(new GenericMessage<String>("test"));
		Message<?> result = channel.receive(0);
		assertNotNull(result);		
		assertEquals(2, invokedCount.get());
		assertEquals(1, messageCount.get());
	}
	@Test
	public void testInterceptorBeanWithPnamespace(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("ChannelInterceptorTests-context.xml", ChannelInterceptorTests.class);
		AbstractMessageChannel channel = ac.getBean("input", AbstractMessageChannel.class);
		DirectFieldAccessor cAccessor = new DirectFieldAccessor(channel);
		Object iList = cAccessor.getPropertyValue("interceptors");
		DirectFieldAccessor iAccessor = new DirectFieldAccessor(iList);
		@SuppressWarnings("unchecked")
		List<PreSendReturnsMessageInterceptor> interceptorList =
					(List<PreSendReturnsMessageInterceptor>) iAccessor.getPropertyValue("interceptors");
		String foo = interceptorList.get(0).getFoo();
		assertTrue(StringUtils.hasText(foo));
		assertEquals("foo", foo);
	}


	public static class PreSendReturnsMessageInterceptor extends ChannelInterceptorAdapter { 
		private String foo;

		private static AtomicInteger counter = new AtomicInteger();

		@Override
		public <T> Message<T> preSend(Message<T> message, MessageChannel<T> channel) {
			assertNotNull(message);
			Message<T> reply = MessageBuilder.fromMessage(message)
					.setHeader(this.getClass().getSimpleName(), counter.incrementAndGet()).build();
			return reply;
		}
		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}


	private static class PreSendReturnsNullInterceptor extends ChannelInterceptorAdapter { 

		private static AtomicInteger counter = new AtomicInteger();

		protected int getCount() {
			return counter.get();
		}

		@Override
		public Message preSend(Message message, MessageChannel channel) {
			assertNotNull(message);
			counter.incrementAndGet();
			return null;
		}
	}


	private static class PreReceiveReturnsTrueInterceptor extends ChannelInterceptorAdapter { 

		private static AtomicInteger counter = new AtomicInteger();

		@Override
		public boolean preReceive(MessageChannel channel) {
			counter.incrementAndGet();
			return true;
		}
	}


	private static class PreReceiveReturnsFalseInterceptor extends ChannelInterceptorAdapter { 

		private static AtomicInteger counter = new AtomicInteger();

		@Override
		public boolean preReceive(MessageChannel channel) {
			counter.incrementAndGet();
			return false;
		}
	}

}
