package play.modules.camel;

import org.apache.activemq.broker.BrokerService;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.impl.DefaultCamelContext;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;
import play.inject.Injector;

public class CamelPlugin extends PlayPlugin implements BeanSource {

	private static DefaultCamelContext ctx;
	private static BrokerService broker;

	@Override
	public void onApplicationStart() {
		try {
			if(broker == null){
				// ActiveMQ
				broker = new BrokerService();
				broker.setAdvisorySupport(false);
				broker.setUseJmx(true);
				broker.setBrokerName(Play.configuration.getProperty("broker.name", "play-activemq"));
				broker.addConnector(Play.configuration.getProperty("broker.connector", "tcp://localhost:61616"));
				broker.start();
				Logger.info("ActiveMQ Broker started...");
			}
            if(ctx == null){
				// Camel
				ctx = new DefaultCamelContext();
				ctx.setName(Play.configuration.getProperty("camel.name", "play-camel"));
				ActiveMQComponent amqc = new ActiveMQComponent(ctx);
				amqc.setBrokerURL(Play.configuration.getProperty("broker.url", "vm:localhost"));
				ctx.start();
				Logger.info("Camel EIP started...");
			}

		} catch (Exception e) {
			//Logger.error(e, "Exception on initialization: %s", e.getMessage());
			throw new ExceptionInInitializerError(e);
		}
		Injector.inject(this);
	}

	@Override
	public void onApplicationStop() {
		try {
			ctx.shutdown();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see play.inject.BeanSource#getBeanOfType(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public <T> T getBeanOfType(Class<T> clazz) {
		Logger.info("CamelContext Injection...");
		if (clazz.equals(CamelContext.class)) {
			Logger.info("CamelContext Injection...OK");
			return (T) ctx;
		}
		Logger.info("CamelContext Injection...KO");
		return null;
	}
	
}
