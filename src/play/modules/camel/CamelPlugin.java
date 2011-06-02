package play.modules.camel;

import javax.jms.ConnectionFactory;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
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

import com.google.gson.JsonObject;

public class CamelPlugin extends PlayPlugin implements BeanSource {

	private static DefaultCamelContext ctx;

	@Override
	public void onApplicationStart() {
		try {

			if (ctx == null) {
				ctx = new DefaultCamelContext();
				ctx.setName(Play.configuration.getProperty("camel.name", "play-camel"));

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

				HazelcastComponent hazel = new HazelcastComponent(ctx);
				hazel.start();
				ctx.addComponent("hazelcast", hazel);

				ctx.start();
				Logger.info("Camel Service is now started...\n");
			}

		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
		Injector.inject(this);
	}

	@Override
	public void onApplicationStop() {
		try {
			ctx.shutdown();
			while (!ctx.isStopped()) {
				Thread.sleep(100);
				Logger.info("Stopping %s...", "Camel");
			}
			ctx = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		Logger.info("Camel & ActiveMQ Services are now stopped\n");
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

	private static ActiveMQComponent getActiveMQComponent() {
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
		Router.prependRoute("GET", "/@camel/?", "CamelApplication.index");
	}

	public static CamelContext getCamelContext() {
		return ctx;
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
