package lan.sahara.webirc.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class PulseEvent extends GwtEvent<PulseEvent.PulseHandler> {
	private Boolean action=null;
	public interface PulseHandler extends EventHandler {
		void onPulseAction(PulseEvent event);
	}
	public static final Type<PulseEvent.PulseHandler> TYPE = new Type<PulseEvent.PulseHandler>();
	public PulseEvent(Boolean _action) {
		this.action=_action;
	}
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<PulseHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(PulseHandler handler) {
		handler.onPulseAction(this);
	}
	public Boolean getAction() {
		return this.action;
	}
}
