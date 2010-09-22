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

package org.springframework.integration.http;

import org.junit.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpRequestHandlingMessagingGatewayTests {

	@Test
	@SuppressWarnings("unchecked")
	public void getRequestGeneratesMapPayload() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setRequestChannel(requestChannel);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(LinkedMultiValueMap.class, message.getPayload().getClass());
		LinkedMultiValueMap<String, String> map = (LinkedMultiValueMap<String, String>) message.getPayload();
		assertEquals(1, map.get("foo").size());
		assertEquals("bar", map.getFirst("foo"));
	}

	@Test
	public void stringExpectedWithoutReply() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(false);
		gateway.setRequestPayloadType(String.class);
		gateway.setRequestChannel(requestChannel);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("text/plain");
		request.setContent("hello".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		Message<?> message = requestChannel.receive(0);
		assertNotNull(message);
		assertEquals(String.class, message.getPayload().getClass());
		assertEquals("hello", message.getPayload());
	}

	@Test
	public void stringExpectedWithReply() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		});
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setRequestPayloadType(String.class);
		gateway.setRequestChannel(requestChannel);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.addHeader("Accept", "x-application/octet-stream");
		request.setContentType("text/plain");
		request.setContent("hello".getBytes());
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		assertEquals("HELLO", response.getContentAsString());
	}

	@Test
	public void testExceptionConversion() throws Exception {
		QueueChannel requestChannel = new QueueChannel() {
			@Override
			protected boolean doSend(Message message, long timeout) {
				throw new RuntimeException("Planned");
			}
		};
		HttpRequestHandlingMessagingGateway gateway = new HttpRequestHandlingMessagingGateway(true);
		gateway.setRequestChannel(requestChannel);
		gateway.setConvertExceptions(true);
		gateway.setMessageConverters(Arrays.<HttpMessageConverter<?>>asList(new DumbHttpMessageConverter()));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Accept", "application/x-java-serialized-object");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		String content = response.getContentAsString();
		assertEquals("Planned", content);
	}

	private static class DumbHttpMessageConverter extends AbstractHttpMessageConverter<Exception> {
		
		public DumbHttpMessageConverter() {
			setSupportedMediaTypes(Arrays.asList(MediaType.ALL));
		}

		@Override
		protected Exception readInternal(Class<? extends Exception> clazz, HttpInputMessage inputMessage) throws IOException,
				HttpMessageNotReadableException {
			return null;
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		protected void writeInternal(Exception t, HttpOutputMessage outputMessage) throws IOException,
				HttpMessageNotWritableException {
			new PrintWriter(outputMessage.getBody()).append(t.getCause().getMessage()).flush();
		}

	}

}
