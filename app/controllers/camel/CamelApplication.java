package controllers.camel;

import java.util.List;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import play.Logger;
import play.modules.camel.CamelPlugin;
import play.mvc.Controller;

public class CamelApplication extends Controller {
	
	public static void index(){
		List<Route> routes = CamelPlugin.getCamelContext().getRoutes();
		render(routes);
	}
	
}
