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
package org.springframework.integration.ip.tcp.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.springframework.commons.serializer.java.JavaStreamingConverter;
import org.springframework.integration.ip.util.SocketUtils;

/**
 * @author Gary Russell
 *
 */
public class InputConverterTests {

	@Test
	public void testReadLength() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendLength(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		byte[] out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(out));
		out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
				 new String(out));
		server.close();
	}
	
	@Test
	public void testReadStxEtx() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendStxEtx(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxConverter converter = new ByteArrayStxEtxConverter();
		byte[] out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(out));
		out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
				 new String(out));
		server.close();
	}

	@Test
	public void testReadCrLf() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendCrLf(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		byte[] out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
								 new String(out));
		out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
				 new String(out));
		server.close();
	}

	@Test
	public void testReadSerialized() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendSerialized(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		JavaStreamingConverter converter = new JavaStreamingConverter();
		Object out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING, out);
		out = converter.convert(socket.getInputStream());
		assertEquals("Data", SocketUtils.TEST_STRING, out);
		server.close();
	}

	@Test
	public void testReadLengthOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendLengthOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderConverter converter = new ByteArrayLengthHeaderConverter();
		try {
			converter.convert(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Message length")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadStxEtxTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayStxEtxConverter converter = new ByteArrayStxEtxConverter();
		try {
			converter.convert(socket.getInputStream());
	    	fail("Expected timeout exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Read timed out")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadStxEtxOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxConverter converter = new ByteArrayStxEtxConverter();
		converter.setMaxMessageSize(1024);
		try {
			converter.convert(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("ETX not found")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadCrLfTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendCrLfOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		try {
			converter.convert(socket.getInputStream());
	    	fail("Expected timout exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Read timed out")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}

	@Test
	public void testReadCrLfOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketUtils.testSendCrLfOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayCrLfConverter converter = new ByteArrayCrLfConverter();
		converter.setMaxMessageSize(1024);
		try {
			converter.convert(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("CRLF not found")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
	}
	
}
