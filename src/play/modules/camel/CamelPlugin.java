package play.modules.camel;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.component.hazelcast.HazelcastComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.jms.core.JmsTemplate;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import akka.camel.CamelContextManager;
import akka.camel.CamelServiceManager;

import com.google.gson.JsonObject;

public class CamelPlugin extends PlayPlugin implements BeanSource {

	private static DefaultCamelContext ctx;
	private static BrokerService broker;

	@Override
	public void onApplicationStart() {
		try {
			if(broker == null && Play.configuration.containsKey("broker.connector")){
				Logger.info("Starting Broker Service...");
				broker = new BrokerService();
				broker.setAdvisorySupport(false);
				broker.setUseJmx(true);
				broker.setBrokerName("play-activemq");
				broker.addConnector(Play.configuration.getProperty("broker.connector", "nio://localhost:61616"));
				broker.setEnableStatistics(true);
				broker.start();
				Logger.info("Starting Broker Service...OK");
			}
			if (ctx == null) {
				Logger.info("Starting Camel Service...");
				ctx = new DefaultCamelContext();
				ctx.setName("play-camel");

				Logger.info("Starting ActiveMQComponent...");
				ActiveMQComponent amqc = new ActiveMQComponent(ctx);
				String brokerURL = Play.configuration.getProperty("broker.url", "vm://localhost");
				amqc.setBrokerURL(brokerURL);
				amqc.setUsePooledConnection(true);
				amqc.setMessageIdEnabled(true);
				amqc.setMessageTimestampEnabled(true);
				amqc.setTestConnectionOnStartup(true);
				amqc.setTransacted(true);
				amqc.setAutoStartup(true);
				amqc.start();
				ctx.addComponent("activemq", amqc);
				Logger.info("Starting ActiveMQComponent...OK");
				
				Logger.info("Starting HazelcastComponent...");
				HazelcastComponent hazel = new HazelcastComponent(ctx);
				hazel.start();
				ctx.addComponent("hazelcast", hazel);
				Logger.info("Starting HazelcastComponent...OK");

				ctx.start();
				Logger.info("Starting Camel Service...OK");
			}

		} catch (Exception e) {
			Logger.info("Starting Camel Service...KO");
			throw new ExceptionInInitializerError(e);
		}
		
		try {
			Logger.info("Starting AKKA Camel Service...");
			CamelContextManager.init(ctx);
			CamelContextManager.start();
			CamelServiceManager.startCamelService();
			Logger.info("Starting AKKA Camel Service is...OK");
		} catch (Exception e) {
			Logger.info("Starting AKKA Camel Service is...KO");
			throw new ExceptionInInitializerError(e);
		}
		Injector.inject(this);
	}

	@Override
	public void onApplicationStop() {
		Logger.info("Stopping Camel Services...");
		try {
			CamelServiceManager.stopCamelService();
		} catch (Exception e) {
		}
		try {
			if(CamelContextManager.started()){
				CamelContextManager.stop();
			}
		} catch (Exception e) {
		}
		try {
			broker.stop();
			while(broker.isStarted()){
				Thread.sleep(1000);
			}
			broker = null;
		} catch (Exception e) {
		}
		try {
			ctx.shutdown();
			while (!ctx.isStopped()) {
				Thread.sleep(100);
			}
			ctx = null;
		} catch (Exception e) {
		}
		Logger.info("Stopping Camel Services...OK");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see play.inject.BeanSource#getBeanOfType(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T> T getBeanOfType(Class<T> clazz) {
		if (clazz.equals(CamelContext.class)) {
			Logger.info("%s Injection...OK", clazz.getName());
			return (T) ctx;
		} else if (clazz.equals(JmsTemplate.class)) {
			Logger.info("%s Injection...OK", clazz.getName());
			return (T) getJmsTemplate();
		} else {
			return null;
		}
	}

	private  static ActiveMQComponent getActiveMQComponent() {
		return ctx.getComponent("activemq", ActiveMQComponent.class);
	}

	@Override
	public boolean rawInvocation(Request request, Response response) throws Exception {
		if ("/@camel".equals(request.path)) {
			response.status = 302;
			response.setHeader("Location", "/@camel/");
			return true;
		}
		return false;
	}

	@Override
	public void onRoutesLoaded() {
		Router.prependRoute("GET", "/@camel/?", "camel.CamelApplication.index");
	}

	public static CamelContext getCamelContext() {
		return ctx;
	}

	public static BrokerService getBroker() {
		return broker;
	}

	public static JmsTemplate getJmsTemplate() {
		return new JmsTemplate(getActiveMQComponent().getConfiguration().getConnectionFactory());
	}

	public String getStatus() {
		return "Camel Status: " + ctx.getStatus().toString();
	}

	public JsonObject getJsonStatus() {
		JsonObject o = new JsonObject();
		o.addProperty("started", ctx.getStatus().toString());
		return o;
	}

}
