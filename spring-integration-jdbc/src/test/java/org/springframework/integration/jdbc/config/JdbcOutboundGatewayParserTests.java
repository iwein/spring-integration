package org.springframework.integration.jdbc.config;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JdbcOutboundGatewayParserTests {

	private SimpleJdbcTemplate jdbcTemplate;

	private MessageChannel<Map<String, String>> channel;

	private ConfigurableApplicationContext context;

	private MessagingTemplate messagingTemplate;

	@Test
	public void testMapPayloadMapReply() {
		setUp("handlingMapPayloadJdbcOutboundGatewayTest.xml", getClass());
		Message<Map<String, String>> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
		Message<?> reply = messagingTemplate.receive();
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertEquals("bar", payload.get("name"));
	}

	@Test
	public void testKeyGeneration() {
		setUp("handlingKeyGenerationJdbcOutboundGatewayTest.xml", getClass());
		Message<Map<String, String>> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Message<?> reply = messagingTemplate.receive();
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		Object id = payload.get("SCOPE_IDENTITY()");
		assertNotNull(id);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from BARS");
		assertEquals("Wrong id", id, map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@Test
	public void testCountUpdates() {
		setUp("handlingCountUpdatesJdbcOutboundGatewayTest.xml", getClass());
		Message<Map<String, String>> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Message<?> reply = messagingTemplate.receive();
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		Map<String, ?> payload = (Map<String, ?>) reply.getPayload();
		assertEquals(1, payload.get("updated"));
	}

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	protected void setupMessagingTemplate() {
		PollableChannel pollableChannel = this.context.getBean("output", PollableChannel.class);
		this.messagingTemplate = new MessagingTemplate(pollableChannel);
		this.messagingTemplate.setReceiveTimeout(500);
	}

	@SuppressWarnings("unchecked")
	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
		channel = this.context.getBean("target", MessageChannel.class);
		jdbcTemplate = new SimpleJdbcTemplate(this.context.getBean("dataSource", DataSource.class));
		setupMessagingTemplate();
	}

}
