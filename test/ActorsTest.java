import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.ProducerCallback;

import play.modules.camel.CamelPlugin;
import play.test.UnitTest;
import actors.MyActorImpl;
import akka.actor.ActorRef;
import akka.actor.Actors;
import akka.camel.Message;

public class ActorsTest extends UnitTest{
	
	private CamelContext ctx;
	
	@Before
	public void before(){
		ctx = CamelPlugin.getCamelContext();
	}
	
	@After
	public void after(){
		Actors.registry().shutdownAll();
		try {
			ctx.stopRoute("test");
		} catch (Exception e) {
		}
		try {
			ctx.removeRoute("test");
		} catch (Exception e) {
		}
		try {
			ctx.stopRoute("jms");
		} catch (Exception e) {
		}
		try {
			ctx.removeRoute("jms");
		} catch (Exception e) {
		}
		try {
			ctx.stopRoute("testReply");
		} catch (Exception e) {
		}
		try {
			ctx.removeRoute("testReply");
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
	public void testActorSendRequestReply(){
		try {
			ActorRef actor = Actors.actorOf(MyActorImpl.class);
			actor.start();
			
			assertEquals("result: test", actor.sendRequestReply(new Message("test")));
			
			actor.stop();
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testCamelRouteToActor(){
		try {
			ActorRef actor = Actors.actorOf(MyActorImpl.class);
			actor.start();

			final String actorUri = String.format("actor:uuid:%s", actor.getUuid());
			RouteBuilder rb = new RouteBuilder(ctx) {
				@Override
				public void configure() throws Exception {
					from("direct:test").id("test").to(actorUri);
				}
			};
			ctx.addRoutes(rb);
			Endpoint endpoint = ctx.getEndpoint("direct:test");
			String response = (String) ctx.createProducerTemplate().requestBody(endpoint, "test");
			assertEquals("result: test", response);
			
			actor.stop();

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test 
	public void testActorCustomId(){
		try {
			ActorRef actor = Actors.actorOf(MyActorImpl.class);
			actor.setId("myActor");
			actor.start();
			final String actorUri = String.format("actor:id:%s", actor.getId());
			RouteBuilder rb = new RouteBuilder(ctx) {
				@Override
				public void configure() throws Exception {
					from("direct:test").id("test").to(actorUri).end();
				}
			};
			ctx.addRoutes(rb);
			Endpoint endpoint = ctx.getEndpoint("direct:test");
			String response = (String) ctx.createProducerTemplate().requestBody(endpoint, "test");
			assertEquals("result: test", response);
			
			actor.stop();

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testJmsToActor(){
		try {
			ActorRef actor = Actors.actorOf(MyActorImpl.class);
			actor.setId("myActor");
			actor.start();
			final String actorUri = String.format("actor:id:%s", actor.getId());
			RouteBuilder rb = new RouteBuilder(ctx) {
				@Override
				public void configure() throws Exception {
					from("direct:test").id("jms").to("activemq:test");
					from("activemq:test").id("test").to(actorUri);
				}
			};
			ctx.addRoutes(rb);

			JmsTemplate jms = CamelPlugin.getJmsTemplate();
			jms.send("activemq:test", new MessageCreator() {
				public javax.jms.Message createMessage(Session session) throws JMSException {
					return session.createTextMessage("test");
				}
			});

			actor.stop();

		}catch(Exception e){
			fail(e.getMessage());
		}
		
	}
}