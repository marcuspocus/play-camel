import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;

import play.modules.camel.CamelPlugin;
import play.test.UnitTest;


public class FileRouteTest extends UnitTest{

	@Test
	public void testFile(){
		RouteBuilder routes = new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				from("file:///Users/marcus/tmp/msgs").convertBodyTo(String.class).to("file:///Users/marcus/tmp/outbox");
			}
		};
		
		try {
			CamelPlugin.getCamelContext().addRoutes(routes);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
