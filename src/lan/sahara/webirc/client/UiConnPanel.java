package lan.sahara.webirc.client;

import java.util.Date;
import lan.sahara.webirc.events.LoginRequiredEvent;
import lan.sahara.webirc.events.PulseEvent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class UiConnPanel extends DialogBox implements ClickHandler,LoginRequiredEvent.LoginRequiredHandler  {
	final long ONE_YEAR = 1000 * 60 * 60 * 24 * 31 * 12; // year
	private HandlerManager eventBus = null;
	private static final String SERVER_ERROR = "An error occurred while attempting to contact the server. Please check your network connection and try again.";
	private final IrcServiceAsync ircService = GWT.create(IrcService.class);
	private static UiConnectionUiBinder uiBinder = GWT.create(UiConnectionUiBinder.class);
	interface UiConnectionUiBinder extends UiBinder<Widget, UiConnPanel> {}
	@UiField TextBox nickName;
	@UiField TextBox serverName;
	@UiField TextBox serverPort;
	@UiField PasswordTextBox serverPassword;
	@UiField Button connect;
	@UiField CheckBox cookieCheck;
	public UiConnPanel(HandlerManager _eventBus) {
		this.eventBus=_eventBus;
		this.eventBus.addHandler(LoginRequiredEvent.TYPE,this);
		setWidget(uiBinder.createAndBindUi(this));
		connect.addClickHandler(this);
		this.setText("Irc connection:");
		// extract cookies
		nickName.setValue(Cookies.getCookie("nick"));
		serverName.setValue(Cookies.getCookie("server"));
		serverPort.setValue(Cookies.getCookie("port"));
		serverPassword.setValue(Cookies.getCookie("password"));		
	}
	public void onClick(ClickEvent event) {
		if ( nickName.getValue().length() > 0 && serverName.getValue().length() > 0 ) {
			UiConnPanel.this.connect.setEnabled(false);
			ircService.login(nickName.getValue(), serverName.getValue(),serverPort.getValue(),serverPassword.getValue(),new AsyncCallback<Boolean>() {
				public void onFailure(Throwable caught) {
					Window.alert(SERVER_ERROR);
				}
				public void onSuccess(Boolean result) {
					if ( result == true ) {
						eventBus.fireEvent(new PulseEvent(true));
						UiConnPanel.this.hide();
						UiConnPanel.this.connect.setEnabled(true);
						if ( cookieCheck.getValue() == true ) {
							Date expires = new Date(System.currentTimeMillis()+ONE_YEAR);
							Cookies.setCookie("nick", nickName.getValue(),expires, null, "/", false);
							Cookies.setCookie("server", serverName.getValue(),expires, null, "/", false);
							Cookies.setCookie("port", serverPort.getValue(),expires, null, "/", false);
							Cookies.setCookie("password", serverPassword.getValue(),expires, null, "/", false);
						}							
					}
				}
			});
		}
	}
	@Override
	public void onLoginRequired(LoginRequiredEvent event) {
		UiConnPanel.this.center();
	}
}
