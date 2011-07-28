package lan.sahara.webirc.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;
import lan.sahara.webirc.events.LoginRequiredEvent;
import lan.sahara.webirc.events.PulseEvent;
import lan.sahara.webirc.shared.IrcContainer;
import lan.sahara.webirc.shared.IrcEntry;
import lan.sahara.webirc.shared.IrcUser;
import lan.sahara.webirc.shared.IrcUserListEntry;
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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
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

public class WebIrc implements EntryPoint,PulseEvent.PulseHandler {
	private static final String SERVER_ERROR = "An error occurred while attempting to contact the server. Please check your network connection and try again.";
	private Button disconnect = new Button("Disconnect");
	private Long timestamp=0L; 
	private HandlerManager eventBus = new HandlerManager(this);
	private ScrollPanel user_list_scroll = new ScrollPanel();
	private DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.PX);
	private TabLayoutPanel tabPanel = new TabLayoutPanel(2.5, Unit.EM);
	private HashMap<String,IrcChatTable> channel_list = new HashMap<String,IrcChatTable>();
	private HorizontalPanel titlePanel = new HorizontalPanel();
	private Panel title = new SimplePanel();
	private DateTimeFormat dateFormat = DateTimeFormat.getFormat("HH:mm:ss");
	private Boolean pulseRunning = false;
	private final TextBox sendString = new TextBox ();
	private final IrcServiceAsync ircService = GWT.create(IrcService.class);
	public void onModuleLoad() {
		this.eventBus.addHandler(PulseEvent.TYPE, this);
		UiConnPanel connPanel = new UiConnPanel(eventBus);
		connPanel.setTitle("Connection");
		title.setStyleName("WhiteText");
		titlePanel.add(title);
		titlePanel.add(disconnect);
		titlePanel.setCellWidth(title, "100%");
		titlePanel.setStyleName("title_panel");
		disconnect.setStyleName("disconnect");
		sendString.setStyleName("raw");
		sendString.setWidth("100%");
		channel_list.put("SERVER", new IrcChatTable("SERVER"));
		tabPanel.addStyleName("tab_panel");
		tabPanel.setAnimationDuration(250);
		tabPanel.setWidth("100%");
		tabPanel.setHeight("100%");
		// main page structure
		mainPanel.addNorth(titlePanel, 25);
		mainPanel.addSouth(sendString, 25);
		mainPanel.addEast(user_list_scroll, 140);
		mainPanel.add(tabPanel);
		RootLayoutPanel.get().add(mainPanel);
		
		// tab selection
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
				user_list_scroll.setWidget(current.user_list_table);
				// update title
				title.clear();
				title.add(current.topic);
				Window.setTitle("[" + current.getName() + "] " + current.topic.getText());
			}
		});		
		// handle string sending
		sendString.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && sendString.getValue().length() > 0 ) {
					IrcChatTable target=(IrcChatTable)tabPanel.getWidget(tabPanel.getSelectedIndex());
					ircService.send(sendString.getValue(),target.getName(),new AsyncCallback<Boolean>() {
						public void onFailure(Throwable caught) {}
						public void onSuccess(Boolean result) {
							sendString.setValue("");
							sendString.setFocus(true);
						}
					});
				}
			}
		});
		// handle page reload
		ircService.reload(new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {
				eventBus.fireEvent(new PulseEvent(false));
				Window.alert(SERVER_ERROR);
			}
			public void onSuccess(Boolean result) {
				if ( result == true ) {
					pulseRunning=true;
					runPulse();
				}else 
					eventBus.fireEvent(new LoginRequiredEvent());
			}
		});
		// disconnect (button) from server
		disconnect.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent event) {
				ircService.logout(new AsyncCallback<Boolean>() {
					public void onFailure(Throwable caught) {
						eventBus.fireEvent(new PulseEvent(false));
						Window.alert(SERVER_ERROR);
					}
					public void onSuccess(Boolean result) {
						pulseRunning=false;
						tabPanel.clear();
//						user_list.clear();
						eventBus.fireEvent(new LoginRequiredEvent());
					}
				});
			}
		});		
	}
	
	/**
	 * self running "pulse" loop 
	 */
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
									tabPanel.selectTab(channel_list.get(c.channel));
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
							for ( Entry<String, TreeMap<String,IrcUser>> c : result.channel_user_lists.entrySet() ) {
								if ( channel_list.containsKey(c.getKey())) {
									channel_list.get(c.getKey()).user_list = new TreeMap<String,IrcUser>(c.getValue()) ;
									channel_list.get(c.getKey()).rebuildUserWidget();
								}
							}
						}
						// user list events
						if ( result.userlist_buffer != null ) {
							for ( Entry<Long, IrcUserListEntry> c : result.userlist_buffer.entrySet() ) {
								if ( c.getValue().type == IrcUserListEntry.E_JOIN ) {
									if ( ! channel_list.get(c.getValue().channel).user_list.containsKey(c.getValue().nick) ) {
										channel_list.get(c.getValue().channel).user_list.put(c.getValue().nick, c.getValue().user);
									}
								}
								if ( c.getValue().type == IrcUserListEntry.E_PART ) {
									if ( channel_list.get(c.getValue().channel).user_list.containsKey(c.getValue().nick) ) {
										channel_list.get(c.getValue().channel).user_list.remove(c.getValue().nick);	
									}
								}
								if ( c.getValue().type == IrcUserListEntry.E_QUIT ) {
									//TODO: check all channels for removal
								}
								channel_list.get(c.getValue().channel).rebuildUserWidget();
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
	/* 
	 * Handles pulse logic
	 * @see lan.sahara.webirc.events.PulseEvent.PulseHandler#onPulseAction(lan.sahara.webirc.events.PulseEvent)
	 */
	public void onPulseAction(PulseEvent event) {
		if ( event.getAction() == true ) {
			pulseRunning=true;
			runPulse();
		}else 
			pulseRunning=false;
	}

}
