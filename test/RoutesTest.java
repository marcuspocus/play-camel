import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import play.Logger;
import play.modules.camel.CamelPlugin;
import play.test.UnitTest;
import play.utils.Utils;
import play.vfs.VirtualFile;


public class RoutesTest extends UnitTest {

	private CamelContext ctx;
	
	@Before
	public void before(){
		Logger.info("Before");
		ctx = CamelPlugin.getCamelContext();
		RouteBuilder rb = new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				from("direct:start").id("inbox").to("log:inbox").to("mock:inbox");
				from("activemq:test").id("jms").to("direct:start");
			}
		};
		
		try {
			ctx.addRoutes(rb);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public void after(){
		Logger.info("After");
		try {
			ctx.stopRoute("inbox");
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
		try {
			ctx.removeRoute("inbox");
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try {
			ctx.stopRoute("jms");
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
		try {
			ctx.removeRoute("jms");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSendWithCamelEndpoint(){
		ctx.createProducerTemplate().sendBody(ctx.getEndpoint("direct:start"), "test:testDirectRoutes");
	}

	@Test
	public void testSendWithJmsTemplate(){
		JmsTemplate jms = CamelPlugin.getJmsTemplate();
		jms.send("activemq:test", new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("test:testJmsRoutes");
			}
		});
	}
	
	@Test
	public void testSendAsync(){
		Future<Object> result = ctx.createProducerTemplate().asyncSendBody("direct:start", "test:testDirectRoutes");
		try {
			result.get(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}
}
