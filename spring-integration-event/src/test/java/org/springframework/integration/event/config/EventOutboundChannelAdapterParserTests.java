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

package org.springframework.integration.event.config;

import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.event.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EventOutboundChannelAdapterParserTests {
	@Autowired
	private ConfigurableApplicationContext context;
	
	private boolean recievedEvent;
	
	@Test
	public void validateEventParser(){
		EventDrivenConsumer adapter = context.getBean("eventAdapter", EventDrivenConsumer.class);
		Assert.assertNotNull(adapter);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		MessageHandler handler = (MessageHandler) adapterAccessor.getPropertyValue("handler");
		Assert.assertTrue(handler instanceof ApplicationEventPublishingMessageHandler);
		Assert.assertEquals(context.getBean("input"), adapterAccessor.getPropertyValue("inputChannel"));
	}
	@Test
	public void validateUsage(){
		
		ApplicationListener listener = new ApplicationListener<ApplicationEvent>() {
			public void onApplicationEvent(ApplicationEvent event) {
				Object source = event.getSource();
				if (source instanceof Message){
					String payload = (String) ((Message)source).getPayload();
					if (payload.equals("hello")){
						recievedEvent = true;
					}
				}
			}
		};
		context.addApplicationListener(listener);
		DirectChannel channel = context.getBean("input", DirectChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		Assert.assertTrue(recievedEvent);
	}
	
	@Test(timeout=2000)
	public void validateUsageWithPollableChannel() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("EventOutboundChannelAdapterParserTestsWithPollable-context.xml", EventOutboundChannelAdapterParserTests.class);
		final CyclicBarrier barier = new CyclicBarrier(2);
		ApplicationListener listener = new ApplicationListener<ApplicationEvent>() {
			public void onApplicationEvent(ApplicationEvent event) {
				Object source = event.getSource();
				if (source instanceof Message){
					String payload = (String) ((Message)source).getPayload();
					if (payload.equals("hello")){
						recievedEvent = true;
						try {
							barier.await();
						} catch (Exception e) {
							e.printStackTrace();
						} 
					}
				}
			}
		};
		ac.addApplicationListener(listener);
		QueueChannel channel = ac.getBean("input", QueueChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		barier.await();
		Assert.assertTrue(recievedEvent);
	}
	
}
