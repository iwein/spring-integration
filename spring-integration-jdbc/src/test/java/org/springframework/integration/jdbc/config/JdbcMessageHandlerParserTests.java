package org.springframework.integration.jdbc.config;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JdbcMessageHandlerParserTests {

	private SimpleJdbcTemplate jdbcTemplate;
	
	private MessageChannel channel;
	
	private ConfigurableApplicationContext context;
	
	@Test
	@SuppressWarnings("unchecked")
	public void testSimpleOutboundChannelAdapter(){
		setUp("handlingWithJdbcOperationsJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("business.key", "FOO").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", "FOO", map.get("ID"));
		assertEquals("Wrong id", "foo", map.get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDollarHeaderOutboundChannelAdapter(){
		setUp("handlingDollarHeaderJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong id", "foo", map.get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapPayloadOutboundChannelAdapter(){
		setUp("handlingMapPayloadJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapPayloadNestedQueryOutboundChannelAdapter(){
		setUp("handlingMapPayloadNestedQueryJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload(Collections.singletonMap("foo", "bar")).build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testParameterSourceOutboundChannelAdapter(){
		setUp("handlingParameterSourceJdbcOutboundChannelAdapterTest.xml", getClass());
		Message<?> message = MessageBuilder.withPayload("foo").build();
		channel.send(message);
		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * from FOOS");
		assertEquals("Wrong id", message.getHeaders().getId().toString(), map.get("ID"));
		assertEquals("Wrong name", "bar", map.get("name"));
	}

	@After
	public void tearDown(){
		if(context != null){
			context.close();
		}
	}
	
	public void setUp(String name, Class<?> cls){
		context = new ClassPathXmlApplicationContext(name, cls);
		jdbcTemplate = new SimpleJdbcTemplate(this.context.getBean("dataSource",DataSource.class));
		channel = this.context.getBean("target", MessageChannel.class);
	}
	
}
