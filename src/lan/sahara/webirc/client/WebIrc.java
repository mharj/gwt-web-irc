package lan.sahara.webirc.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import lan.sahara.webirc.events.LoginRequiredEvent;
import lan.sahara.webirc.shared.IrcContainer;
import lan.sahara.webirc.shared.IrcEntry;
import lan.sahara.webirc.shared.IrcUser;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebIrc implements EntryPoint,LoginRequiredEvent.LoginRequiredHandler {
	final long ONE_YEAR = 1000 * 60 * 60 * 24 * 31 * 12; // year
	HorizontalPanel cookiePanel = new HorizontalPanel();
	private Button disconnect = new Button("Disconnect");
	CheckBox cookieCheck = new CheckBox();	
	private Long timestamp=0L; 
	private HandlerManager eventBus = new HandlerManager(this);
	FlexTable user_list = new FlexTable();
	ScrollPanel user_list_scroll = new ScrollPanel(user_list);
	DockLayoutPanel p = new DockLayoutPanel(Unit.PX);
	TabLayoutPanel tabPanel = new TabLayoutPanel(2.5, Unit.EM);
	Map<String,IrcChatTable> channel_list = new HashMap<String,IrcChatTable>();
	Panel title = new SimplePanel();
	private DateTimeFormat dateFormat = DateTimeFormat.getFormat("HH:mm:ss");
	private Boolean pulseRunning = false;
	private final DialogBox login = new DialogBox();
	private final FlexTable loginTable = new FlexTable();
	private final TextBox  raw = new TextBox ();
	private final TextBox  nick = new TextBox ();
	private final TextBox  server = new TextBox ();
	private final TextBox  port = new TextBox();
	private final TextBox  password = new TextBox ();
	private final Button  loginButton = new Button ("Connect");
	private static final String SERVER_ERROR = "An error occurred while attempting to contact the server. Please check your network connection and try again.";
	private final IrcServiceAsync ircService = GWT.create(IrcService.class);
	public void onModuleLoad() {
		title.setStyleName("WhiteText");
		HorizontalPanel title_panel = new HorizontalPanel();
		title_panel.add(title);
		title_panel.add(disconnect);
		title_panel.setCellWidth(title, "100%");
		title_panel.setStyleName("title_panel");
		tabPanel.addStyleName("tab_panel");
		login.hide();
		disconnect.setStyleName("disconnect");
		raw.setStyleName("raw");
		// extract cookies
		nick.setValue(Cookies.getCookie("nick"));
		server.setValue(Cookies.getCookie("server"));
		port.setValue(Cookies.getCookie("port"));
		password.setValue(Cookies.getCookie("password"));
		eventBus.addHandler(LoginRequiredEvent.TYPE, this);
		channel_list.put("SERVER", new IrcChatTable("SERVER"));
		tabPanel.setAnimationDuration(250);
		tabPanel.setWidth("100%");
		tabPanel.setHeight("100%");
		raw.setWidth("100%");
		loginButton.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent event) {
				loginButton.setEnabled(false);
				pulseRunning=false;
				ircService.login(nick.getValue(), server.getValue(),port.getValue(),password.getValue(),new AsyncCallback<Boolean>() {
					public void onFailure(Throwable caught) {
						Window.alert(SERVER_ERROR);
					}
					public void onSuccess(Boolean result) {
						if ( result == true ) {
							pulseRunning=true;
							login.hide();
							loginButton.setEnabled(true);
							runPulse();
							if ( cookieCheck.getValue() == true ) {
								Date expires = new Date(System.currentTimeMillis()+ONE_YEAR);
								Cookies.setCookie("nick", nick.getValue(),expires, null, "/", false);
								Cookies.setCookie("server", server.getValue(),expires, null, "/", false);
								Cookies.setCookie("port", port.getValue(),expires, null, "/", false);
								Cookies.setCookie("password", password.getValue(),expires, null, "/", false);
							}							
						}
					}
				});
			}
		});	
		disconnect.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent event) {
				ircService.logout(new AsyncCallback<Boolean>() {
					public void onFailure(Throwable caught) {}
					public void onSuccess(Boolean result) {
						pulseRunning=false;
						tabPanel.clear();
						eventBus.fireEvent(new LoginRequiredEvent());
					}
				});
			}
		});
		tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
			public void onSelection(SelectionEvent<Integer> event) {
				final IrcChatTable current = (IrcChatTable) tabPanel.getWidget(event.getSelectedItem());
				current.scrollToBottom();
				Timer t = new Timer() {
					public void run() {
						current.scrollToBottom();
					}
				};
				t.schedule(100);
				user_list.clear();
				Integer count = 0;
				for (Entry<String, IrcUser> c : current.user_list.entrySet()) {
					StringBuffer id = new StringBuffer();
					if (c.getValue().oper == true)
						id.append("&#9679;");
					if (c.getValue().voice == true)
						id.append("&#9675;");
					HTML h_id =  new HTML(id.toString());
					Label h_uid = new Label(c.getKey());
					h_id.setStyleName("WhiteText");
					h_uid.setStyleName("WhiteText");
					user_list.setWidget(count, 0,h_id);
					user_list.setWidget(count, 1,h_uid);
					count++;
				}
				// update title
				title.clear();
				title.add(current.topic);
				Window.setTitle("[" + current.getName() + "] " + current.topic.getText());
			}
		});
		port.setValue("6667");
		HTML nickHtml = new HTML("Nick:");
		nickHtml.setStyleName("back");
		HTML serverHtml = new HTML("Server:");
		serverHtml.setStyleName("back");
		HTML passwordHtml = new HTML("Password:");
		passwordHtml.setStyleName("back");
		HTML cookieHtml = new HTML("Remember:");
		cookieHtml.setStyleName("back");
		cookiePanel.add(cookieHtml);
		cookiePanel.add(cookieCheck);
		loginTable.setWidget(0, 0, nickHtml);
		loginTable.setWidget(0, 1, nick);
		loginTable.setWidget(1, 0, serverHtml);
		loginTable.setWidget(1, 1, server);
		loginTable.setWidget(1, 2, port);
		loginTable.setWidget(2, 0, passwordHtml);
		loginTable.setWidget(2, 1, password);
		loginTable.setWidget(3, 0, cookiePanel);
		loginTable.setWidget(3, 1, loginButton);		
		login.setHTML("IRC Connect:");
		login.setWidget(loginTable);
		p.addNorth(title_panel, 25);
		p.addSouth(raw, 25);
		p.addEast(user_list_scroll, 140);
		p.add(tabPanel);
		RootLayoutPanel.get().add(p);
		raw.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && raw.getValue().length() > 0 ) {
					IrcChatTable target=(IrcChatTable)tabPanel.getWidget(tabPanel.getSelectedIndex());
					ircService.send(raw.getValue(),target.getName(),new AsyncCallback<Boolean>() {
						public void onFailure(Throwable caught) {}
						public void onSuccess(Boolean result) {
							raw.setValue("");
							raw.setFocus(true);
						}
					});
				}
			}
		});
		ircService.reload(new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {
			}
			public void onSuccess(Boolean result) {
				if ( result == true ) {
					pulseRunning=true;
					runPulse();
				}else 
					eventBus.fireEvent(new LoginRequiredEvent());
			}
		});
	}
	public void runPulse() {
		if ( pulseRunning == true ) {
			ircService.getPulse(timestamp,new AsyncCallback<IrcContainer>() {
				public void onFailure(Throwable caught) {
					pulseRunning=false;
				}
				public void onSuccess(IrcContainer result) {
					if ( result != null ) {
						// get messages to UI
						if ( result.msg_buffer != null ) {
							Iterator<IrcEntry> i=result.msg_buffer.iterator();
							while ( i.hasNext() ) {
								IrcEntry c = i.next();
								Date date = new Date(c.timestamp);
								if ( ! channel_list.containsKey(c.channel) ) {
									channel_list.put(c.channel,new IrcChatTable(c.channel));
									tabPanel.add(channel_list.get(c.channel),c.channel);	
								}
								Label uu=new Label(c.user);
								uu.setStyleName("WhiteText");
								uu.setTitle(c.host);
								Integer idx = channel_list.get(c.channel).chat_table.getRowCount();
								channel_list.get(c.channel).chat_table.setWidget(idx,0,new HTML("["+dateFormat.format(date)+"]") );
								channel_list.get(c.channel).chat_table.setWidget(idx,1,uu);
								channel_list.get(c.channel).chat_table.getFlexCellFormatter().setHorizontalAlignment(idx, 1, HasHorizontalAlignment.ALIGN_RIGHT);
								channel_list.get(c.channel).chat_table.setWidget(idx,2,new HTML(c.msg));
								channel_list.get(c.channel).limitChatCount(256);
								channel_list.get(c.channel).scrollToBottom();
							}
							// lets update timestamp
							if ( result.timestamp != null )
								timestamp=result.timestamp;
						}
						// topic
						if ( result.topic != null ) {
							for ( Entry<String, String> c : result.topic.entrySet() ) {
								if ( channel_list.containsKey(c.getKey())) 
									channel_list.get(c.getKey()).topic.setText(c.getValue());
							}
						}
						// update full user lists
						if ( result.channel_user_lists != null ) {
							for ( Entry<String, Map<String,IrcUser>> c : result.channel_user_lists.entrySet() ) {
								if ( channel_list.containsKey(c.getKey())) 
									channel_list.get(c.getKey()).user_list = new TreeMap<String,IrcUser>(c.getValue()) ;
							}
						}						
						// close tabs
						if ( result.close_channels != null ) {
							Iterator<String> i=result.close_channels.iterator();
							while ( i.hasNext() ) {
								String channel = i.next();
								System.err.println("remove:"+channel);
								if ( tabPanel.getWidget(tabPanel.getSelectedIndex()).equals(channel_list.get(channel)) ) 
									tabPanel.selectTab(0);
								tabPanel.remove(channel_list.get(channel));
								channel_list.remove(channel);
							}
						}
					}
					runPulse(); // self running
				}
			});
		}		
	}
	@Override
	public void onLoginRequired(LoginRequiredEvent event) {
		login.center();
	}
}
