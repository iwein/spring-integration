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
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpRequestHandlingControllerTests {

	@Test
	public void sendOnly() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(0, modelAndView.getModel().size());
		Message<?> requestMessage = requestChannel.receive(0);
		assertNotNull(requestMessage);
		assertEquals("hello", requestMessage.getPayload());
	}

	@Test
	public void requestReply() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		Object reply = modelAndView.getModel().get("reply");
		assertNotNull(reply);
		assertEquals("HELLO", reply);
	}

	@Test
	public void requestReplyWithCustomReplyKey() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.setReplyKey("myReply");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("howdy".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		assertNull(modelAndView.getModel().get("reply"));
		Object reply = modelAndView.getModel().get("myReply");
		assertEquals("HOWDY", reply);
	}

	@Test
	public void requestReplyWithFullMessageInModel() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.setExtractReplyPayload(false);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("abc".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		Object reply = modelAndView.getModel().get("reply");
		assertNotNull(reply);
		assertTrue(reply instanceof Message<?>);
		assertEquals("ABC", ((Message<?>) reply).getPayload());
	}

	@Test
	public void testSendWithError() throws Exception {
		QueueChannel requestChannel = new QueueChannel() {
			@Override
			protected boolean doSend(Message message, long timeout) {
				throw new RuntimeException("Planned");
			}
		};
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setRequestChannel(requestChannel);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals(1, modelAndView.getModel().size());
		Errors errors = (Errors) modelAndView.getModel().get("errors");
		assertEquals(1, errors.getErrorCount());
		ObjectError error = errors.getAllErrors().get(0);
		assertEquals(3, error.getArguments().length);
		assertTrue("Wrong message: "+error, ((String)error.getArguments()[1]).startsWith("failed to send Message"));
	}


}
