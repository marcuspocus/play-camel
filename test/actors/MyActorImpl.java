package actors;

import akka.actor.UntypedActor;
import akka.camel.Message;

public class MyActorImpl extends UntypedActor {

	@Override
	public void onReceive(Object o) {
		Message m = (Message) o;
		String body = m.getBodyAs(String.class);
		getContext().replySafe(String.format("result: %s", body));
	}

}
