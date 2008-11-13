/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.integration.endpoint;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.ErrorMessage;

/**
 * 
 * @author Jonas Partner
 *
 */
public class PollingEndpointErrorHandlingTests {
	
	@SuppressWarnings("unchecked")
	@Test
	public void checkExcpetionPlacedOnErrorChannel() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"pollingEndpointErrorHandlingTests.xml", this.getClass());
		PollableChannel errorChannel = (PollableChannel) context.getBean("errorChannel");
		Message errorMessage = errorChannel.receive(5000);
		assertNotNull("No error message received", errorMessage);
		assertEquals("Message recevied was not an ErrorMessage" ,ErrorMessage.class,errorMessage.getClass());
	}

}