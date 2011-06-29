package lan.sahara.webirc.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class LoginRequiredEvent extends GwtEvent<LoginRequiredEvent.LoginRequiredHandler> {
	public interface LoginRequiredHandler extends EventHandler {
		void onLoginRequired(LoginRequiredEvent event);
	}
	public static final Type<LoginRequiredEvent.LoginRequiredHandler> TYPE = new Type<LoginRequiredEvent.LoginRequiredHandler>();
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<LoginRequiredEvent.LoginRequiredHandler> getAssociatedType() {
		return TYPE;
	}
	@Override
	protected void dispatch(LoginRequiredEvent.LoginRequiredHandler handler) {
		handler.onLoginRequired(this);
	}
}
