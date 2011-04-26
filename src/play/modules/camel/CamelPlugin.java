package play.modules.camel;

import java.net.InetSocketAddress;
import java.net.URI;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerContext;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.command.BrokerInfo;
import org.apache.activemq.transport.TransportAcceptListener;
import org.apache.activemq.transport.TransportServer;
import org.apache.activemq.transport.TransportServerSupport;
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
				broker.addConnector(Play.configuration.getProperty("broker.connector", "nio://localhost:61616"));
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
			throw new ExceptionInInitializerError(e);
		}
		Injector.inject(this);
	}

	@Override
	public void onApplicationStop() {
		try {
			ctx.shutdown();
			while(!ctx.isStopped()){
				Thread.sleep(100);
			}
			Logger.info("Camel & ActiveMQ Services are now stopped");
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
		if (clazz.equals(CamelContext.class)) {
			Logger.info("%s Injection...OK", clazz.getName());
			return (T) ctx;
		}else if (clazz.equals(BrokerService.class)) {
			Logger.info("%s Injection...OK", clazz.getName());
			return (T) broker;
		}

		Logger.info("%s Injection...KO", clazz.getName());
		return null;
	}
	
	public static BrokerService getBroker(){
		return broker;
	}
	
	public static CamelContext getCamelContext(){
		return ctx;
	}
	
}
