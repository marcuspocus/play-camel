import java.util.Date;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	}
	
	@Test
	public void testSend(){
		Logger.info("testRoutes");
		ctx.createProducerTemplate().sendBody(ctx.getEndpoint("direct:start"), "test:testRoutes");
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			// TODO: handle exception
		}
		MockEndpoint me = ctx.getEndpoint("mock:inbox", MockEndpoint.class);
		me.assertExchangeReceived(1);
	}

}
