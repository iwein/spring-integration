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

package org.springframework.integration.ip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.ip.udp.DatagramPacketMessageMapper;
import org.springframework.integration.ip.udp.MulticastSendingMessageHandler;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class DatagramPacketSendingHandlerTests {

	@Test
	public void verifySend() throws Exception {
		final int testPort = 27816;
		byte[] buffer = new byte[8];
		final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					DatagramSocket socket = new DatagramSocket(testPort);
					socket.receive(receivedPacket);
					latch.countDown();
					socket.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread.sleep(1000);
		UnicastSendingMessageHandler handler = 
				new UnicastSendingMessageHandler("localhost", testPort);
		String payload = "foo";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
		byte[] src = receivedPacket.getData();
		int length = receivedPacket.getLength();
		int offset = receivedPacket.getOffset();
		byte[] dest = new byte[length];
		System.arraycopy(src, offset, dest, 0, length);
		assertEquals(payload, new String(dest));
		handler.shutDown();
	}

	@Test
	public void verifySendWithAck() throws Exception {
		final int testPort = 27816;
		final int ackPort = 17816;
		byte[] buffer = new byte[1000];
		final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		final CountDownLatch latch = new CountDownLatch(1);
		UnicastSendingMessageHandler handler = 
				new UnicastSendingMessageHandler("localhost", testPort, true, 
						true, "localhost", ackPort, 5000);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					DatagramSocket socket = new DatagramSocket(testPort);
					socket.receive(receivedPacket);
					socket.close();
					DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
					mapper.setAcknowledge(true);
					mapper.setLengthCheck(true);
					Message<byte[]> message = mapper.toMessage(receivedPacket);
					Object id = message.getHeaders().getId();
					byte[] ack = id.toString().getBytes();
					DatagramPacket ackPack = new DatagramPacket(ack, ack.length, 
							                        new InetSocketAddress("localHost", ackPort));
					DatagramSocket out = new DatagramSocket();
					out.send(ackPack);
					out.close();
					latch.countDown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Thread.sleep(1000);
		String payload = "foobar";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
		byte[] src = receivedPacket.getData();
		int length = receivedPacket.getLength();
		int offset = receivedPacket.getOffset();
		byte[] dest = new byte[6];
		System.arraycopy(src, offset+length-6, dest, 0, 6);
		assertEquals(payload, new String(dest));
		handler.shutDown();
	}

	@Test
	@Ignore
	public void verifySendMulticast() throws Exception {
		final int testPort = 27816;
		final String multicastAddress = "225.6.7.8";
		final String payload = "foo";
		final CountDownLatch latch = new CountDownLatch(2);
		Runnable catcher = new Runnable() {
			public void run() {
				try {
					byte[] buffer = new byte[8];
					DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
					MulticastSocket socket = new MulticastSocket(testPort);
					InetAddress group = InetAddress.getByName(multicastAddress);
					socket.joinGroup(group);
					LogFactory.getLog(getClass())
						.debug(Thread.currentThread().getName() + " waiting for packet");
					socket.receive(receivedPacket);
					socket.close();
					byte[] src = receivedPacket.getData();
					int length = receivedPacket.getLength();
					int offset = receivedPacket.getOffset();
					byte[] dest = new byte[length];
					System.arraycopy(src, offset, dest, 0, length);
					assertEquals(payload, new String(dest));
					LogFactory.getLog(getClass())
						.debug(Thread.currentThread().getName() + " received packet");
					latch.countDown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Executor executor = Executors.newFixedThreadPool(2);
		executor.execute(catcher);
		executor.execute(catcher);
		Thread.sleep(1000);
		MulticastSendingMessageHandler handler = new MulticastSendingMessageHandler(multicastAddress, testPort);
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
		handler.shutDown();
	}

	@Test
	@Ignore
	public void verifySendMulticastWithAcks() throws Exception {
		final int testPort = 27816;
		final int ackPort = 17817;
		final String multicastAddress = "225.6.7.8";
		final String payload = "foobar";
		final CountDownLatch latch = new CountDownLatch(2);
		Runnable catcher = new Runnable() {
			public void run() {
				try {
					byte[] buffer = new byte[1000];
					DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
					MulticastSocket socket = new MulticastSocket(testPort);
					InetAddress group = InetAddress.getByName(multicastAddress);
					socket.joinGroup(group);
					LogFactory.getLog(getClass()).debug(Thread.currentThread().getName() + " waiting for packet");
					socket.receive(receivedPacket);
					socket.close();
					byte[] src = receivedPacket.getData();
					int length = receivedPacket.getLength();
					int offset = receivedPacket.getOffset();
					byte[] dest = new byte[6];
					System.arraycopy(src, offset+length-6, dest, 0, 6);
					assertEquals(payload, new String(dest));
					LogFactory.getLog(getClass()).debug(Thread.currentThread().getName() + " received packet");
					DatagramPacketMessageMapper mapper = new DatagramPacketMessageMapper();
					mapper.setAcknowledge(true);
					mapper.setLengthCheck(true);
					Message<byte[]> message = mapper.toMessage(receivedPacket);
					Object id = message.getHeaders().getId();
					byte[] ack = id.toString().getBytes();
					DatagramPacket ackPack = new DatagramPacket(ack, ack.length, 
							                        new InetSocketAddress("localHost", ackPort));
					DatagramSocket out = new DatagramSocket();
					out.send(ackPack);
					out.close();
					latch.countDown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Executor executor = Executors.newFixedThreadPool(2);
		executor.execute(catcher);
		executor.execute(catcher);
		Thread.sleep(1000);
		MulticastSendingMessageHandler handler = 
			new MulticastSendingMessageHandler(multicastAddress, testPort, true, 
                    							true, "localhost", ackPort, 500000);;
		handler.setMinAcksForSuccess(2);
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
		handler.shutDown();
	}
	
}