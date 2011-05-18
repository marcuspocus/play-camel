package play.modules.camel;

import javax.jms.ConnectionFactory;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;
import play.inject.Injector;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;


public class CamelPlugin extends PlayPlugin implements BeanSource {

	private static DefaultCamelContext ctx;
	private static ActiveMQComponent amqc;
	
	@Override
	public void onApplicationStart() {
		try {
			if (ctx == null) {
				// Camel
				ctx = new DefaultCamelContext();
				ctx.setName(Play.configuration.getProperty("camel.name", "play-camel"));
				amqc = new ActiveMQComponent(ctx);
				amqc.setBrokerURL(Play.configuration.getProperty("broker.url", "vm:localhost"));
				ctx.start();
				Logger.info("Camel & ActiveMQ Services are now started...");
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
			}
			Logger.info("Camel & ActiveMQ Services are now stopped\n");
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
		} 
		else if (clazz.equals(ConnectionFactory.class)) {
			Logger.info("%s Injection...OK", clazz.getName());
			return (T) amqc.getConfiguration().getConnectionFactory();
		}

		Logger.info("%s Injection...KO", clazz.getName());
		return null;
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
	
	public static CamelContext getCamelContext(){
		return ctx;
	}

}
