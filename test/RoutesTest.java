import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Logger;
import play.modules.camel.CamelPlugin;
import play.test.UnitTest;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


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
				// from("activemq:test").id("jms").to("direct:start");
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
		}
		
		try {
			ctx.removeRoute("inbox");
		} catch (Exception e) {
		}
		try {
			for(Endpoint e : ctx.getEndpoints()){
				e.stop();
			}
		} catch (Exception e) {
		}
	}
	
	@Test
	public void testSendWithCamelEndpoint(){
		ctx.createProducerTemplate().sendBody(ctx.getEndpoint("direct:start"), "test:testDirectRoutes");
	}

	/*
	public void testSendWithJmsTemplate(){
		JmsTemplate jms = CamelPlugin.getJmsTemplate();
		jms.send("activemq:test", new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("test:testJmsRoutes");
			}
		});
	}
	*/
	
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
