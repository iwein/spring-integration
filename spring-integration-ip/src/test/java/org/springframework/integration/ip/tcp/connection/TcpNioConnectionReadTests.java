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
package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.ip.tcp.converter.AbstractByteArrayStreamingConverter;
import org.springframework.integration.ip.tcp.converter.ByteArrayCrLfConverter;
import org.springframework.integration.ip.tcp.converter.ByteArrayLengthHeaderConverter;
import org.springframework.integration.ip.tcp.converter.ByteArrayStxEtxConverter;
import org.springframework.integration.ip.util.SocketUtils;

/**
 * @author Gary Russell
 *
 */
public class TcpNioConnectionReadTests {

	private CountDownLatch latch = new CountDownLatch(1);
	
	private AbstractServerConnectionFactory getConnectionFactory(int port,
			AbstractByteArrayStreamingConverter converter, TcpListener listener) throws Exception {
		return getConnectionFactory(port, converter, listener, null);
	}
	
	private AbstractServerConnectionFactory getConnectionFactory(int port,
			AbstractByteArrayStreamingConverter converter, TcpListener listener, TcpSender sender) throws Exception {
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		scf.setInputConverter(converter);
		scf.setOutputConverter(converter);
		scf.registerListener(listener);
		if (sender != null) {
			scf.registerSender(sender);
		}
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 200) {
				fail("Failed to listen");
			}
		}
		return scf;
	}
	
	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testReadLength() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		});
		
		// Fire up the sender.
		
		SocketUtils.testSendLength(port, latch);
		latch.countDown();
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertEquals("Did not receive data", 2, responses.size());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
						         new String(((Message<byte[]>) responses.get(0)).getPayload()));
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
		         new String(((Message<byte[]>) responses.get(1)).getPayload()));
		scf.close();
	}



	@SuppressWarnings("unchecked")
	@Test
	public void testFragmented() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) { }
				semaphore.release();
				return false;
			}
		});
		
		int howMany = 2;
		scf.setPoolSize(howMany + 5);
		// Fire up the sender.
		SocketUtils.testSendFragmented(port, howMany, false);
		assertTrue(semaphore.tryAcquire(howMany, 20000, TimeUnit.MILLISECONDS));
		assertEquals("Expected", howMany, responses.size());
		for (int i = 0; i < howMany; i++) {
			assertEquals("Data", "xx", 
				new String(((Message<byte[]>) responses.get(0)).getPayload()));
		}
		scf.close();
	}
	
	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testReadStxEtx() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayStxEtxConverter converter = new ByteArrayStxEtxConverter();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		});
		
		// Fire up the sender.
		
		SocketUtils.testSendStxEtx(port, latch);
		latch.countDown();
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertEquals("Did not receive data", 2, responses.size());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
						         new String(((Message<byte[]>) responses.get(0)).getPayload()));
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
		         new String(((Message<byte[]>) responses.get(1)).getPayload()));
		scf.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testReadCrLf() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		});
		
		// Fire up the sender.
		
		SocketUtils.testSendCrLf(port, latch);
		latch.countDown();
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertTrue(semaphore.tryAcquire(1, 10000, TimeUnit.MILLISECONDS));
		assertEquals("Did not receive data", 2, responses.size());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
						         new String(((Message<byte[]>) responses.get(0)).getPayload()));
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
		         new String(((Message<byte[]>) responses.get(1)).getPayload()));
		scf.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadLengthOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		
		// Fire up the sender.
		
		SocketUtils.testSendLengthOverflow(port);
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(removed.size() > 0);
		scf.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadStxEtxOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayStxEtxConverter converter = new ByteArrayStxEtxConverter();
		converter.setMaxMessageSize(1024);
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		
		// Fire up the sender.
		
		SocketUtils.testSendStxEtxOverflow(port);
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(removed.size() > 0);
		scf.close();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.NioSocketReader}.
	 */
	@Test
	public void testReadCrLfOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		converter.setMaxMessageSize(1024);
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		
		// Fire up the sender.
		
		SocketUtils.testSendCrLfOverflow(port);
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(removed.size() > 0);
		scf.close();
	}

	/**
	 * Tests socket closure when no data received.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupNoData() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		converter.setMaxMessageSize(1024);
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				semaphore.release();
				return false;
			}
		}, new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.close();
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(removed.size() > 0);
		scf.close();
	}

	/**
	 * Tests socket closure when mid-message
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCloseCleanupCrLf() throws Exception {
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		testClosureMidMessageGuts(converter, "xx");
	}
	
	/**
	 * Tests socket closure when mid-message
	 * 
	 * @throws Exception
	 */

	@Test
	public void testCloseCleanupStxEtx() throws Exception {
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		testClosureMidMessageGuts(converter, ByteArrayStxEtxConverter.STX + "xx");
	}

	/**
	 * Tests socket closure when mid-message
	 * 
	 * @throws Exception
	 */

	@Test
	public void testCloseCleanupLengthHeader() throws Exception {
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		testClosureMidMessageGuts(converter, "\u0000\u0000\u0000\u0003xx");
	}

	private void testClosureMidMessageGuts(AbstractByteArrayStreamingConverter converter, String shortMessage)
			throws Exception, IOException, UnknownHostException,
			InterruptedException {
		final int port = SocketUtils.findAvailableServerSocket();
		final List<Message<?>> responses = new ArrayList<Message<?>>();
		final Semaphore semaphore = new Semaphore(0);
		final List<TcpConnection> added = new ArrayList<TcpConnection>();
		final List<TcpConnection> removed = new ArrayList<TcpConnection>();
		AbstractServerConnectionFactory scf = getConnectionFactory(port, converter,new TcpListener() {
			public boolean onMessage(Message<?> message) {
				responses.add(message);
				return false;
			}
		}, new TcpSender() {
			public void addNewConnection(TcpConnection connection) {
				added.add(connection);
				semaphore.release();
			}
			public void removeDeadConnection(TcpConnection connection) {
				removed.add(connection);
				semaphore.release();
			}
		});
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write(shortMessage.getBytes());
		socket.close();
		whileOpen(semaphore, added);
		assertEquals(1, added.size());
		assertTrue(removed.size() > 0);
		scf.close();
	}

	private void whileOpen(Semaphore semaphore, final List<TcpConnection> added)
			throws InterruptedException {
		int n = 0;
		assertTrue(semaphore.tryAcquire(10000, TimeUnit.MILLISECONDS));
		while (added.get(0).isOpen()) {
			Thread.sleep(50);
			if (n++ > 200) {
				fail("Failed to close socket");
			}
		}
	}

}
