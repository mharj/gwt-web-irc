package lan.sahara.webirc.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lan.sahara.webirc.events.LoginRequiredEvent;
import lan.sahara.webirc.shared.IrcContainer;
import lan.sahara.webirc.shared.IrcEntry;
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
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebIrc implements EntryPoint,LoginRequiredEvent.LoginRequiredHandler {
	private HandlerManager eventBus = new HandlerManager(this);
	VerticalPanel user_list = new VerticalPanel();
	ScrollPanel user_list_scroll = new ScrollPanel(user_list);
	DockLayoutPanel p = new DockLayoutPanel(Unit.EM);
	TabLayoutPanel tabPanel = new TabLayoutPanel(2.5, Unit.EM);
	Map<String,ChatTable> channels = new HashMap<String,ChatTable>();
	Map<String,ScrollPanel> chan_scroll = new HashMap<String,ScrollPanel>();
	Map<String,List<String>> channel_user_lists = new HashMap<String,List<String>>();
	Map<String,String> topic = new HashMap<String,String>();
	HTML title = new HTML("MENU");
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
		login.hide();
		eventBus.addHandler(LoginRequiredEvent.TYPE, this);
		
		channels.put("SERVER", new ChatTable());
		tabPanel.setAnimationDuration(250);
		tabPanel.setWidth("100%");
		tabPanel.setHeight("100%");
		raw.setWidth("100%");
		tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
		      public void onSelection(SelectionEvent<Integer> event) {
		    	  final ScrollPanel current = (ScrollPanel) tabPanel.getWidget(event.getSelectedItem());
		    	  current.scrollToBottom();
		    	  Timer t = new Timer() {
		  	    	public void run() {
		  	    		current.scrollToBottom();	
		  	    	}
		    	  };
		    	  t.schedule(100);
		    	  String channel = current.getTitle();
		    	  user_list.clear();
		    	  if ( channel_user_lists.containsKey(channel) ) {
		    		  Iterator<String> i = channel_user_lists.get(channel).iterator();
		    		  while ( i.hasNext()) {
		    			  user_list.add(new HTML(""+i.next()));
		    		  }
		    	  }
		    	  if ( topic.containsKey(channel)) {
		    		  title.setHTML(topic.get(channel));
		    		  Window.setTitle("["+channel+"] "+topic.get(channel));
		    	  } else { 
		    		  title.setHTML("");
		    		  Window.setTitle("["+channel+"]");
		    	  }
		      }
		});
		port.setValue("6667");
		loginTable.setWidget(0, 0, new HTML("Nick:"));
		loginTable.setWidget(0, 1, nick);
		loginTable.setWidget(1, 0, new HTML("Server:"));
		loginTable.setWidget(1, 1, server);
		loginTable.setWidget(1, 2, port);
		loginTable.setWidget(2, 0, new HTML("Password:"));
		loginTable.setWidget(2, 1, password);
		loginTable.setWidget(3, 1, loginButton);		
		login.setHTML("IRC Connect:");
		login.setWidget(loginTable);
		p.addNorth(title, 3);
		p.addSouth(raw, 3);
		p.addEast(user_list_scroll, 10);
		p.add(tabPanel);
		RootLayoutPanel.get().add(p);
		raw.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && raw.getValue().length() > 0 ) {
					String target=tabPanel.getWidget(tabPanel.getSelectedIndex()).getTitle();
					ircService.send(raw.getValue(),target,new AsyncCallback<Boolean>() {
						public void onFailure(Throwable caught) {
						}
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
				// TODO Auto-generated method stub
				
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
			ircService.getPulse(new AsyncCallback<IrcContainer>() {
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
								if (! channels.containsKey(c.channel)  ) {
									channels.put(c.channel, new ChatTable());
									chan_scroll.put(c.channel, new ScrollPanel(channels.get(c.channel)));
									chan_scroll.get(c.channel).setTitle(c.channel);
									tabPanel.add(chan_scroll.get(c.channel),c.channel);
								}
								Integer idx = channels.get(c.channel).getRowCount();
								Label uu=new Label(c.user);
								uu.setTitle(c.host);
								channels.get(c.channel).setWidget(idx,0,new HTML(dateFormat.format(date) ));
								channels.get(c.channel).setWidget(idx,1,uu);
								channels.get(c.channel).setWidget(idx,2,new HTML(c.msg));
								
								// max row size
								while (channels.get(c.channel).getRowCount() > 256  )  {
									channels.get(c.channel).removeRow(0);
								}
								chan_scroll.get(c.channel).scrollToBottom();
							}
						}
						// topic
						if ( result.topic != null ) {
							for ( Entry<String, String> c : result.topic.entrySet() ) 
								topic.put(c.getKey(),c.getValue());
						}
						// update full user lists
						if ( result.channel_user_lists != null ) {
							for ( Entry<String, List<String>> c : result.channel_user_lists.entrySet() ) {
								channel_user_lists.put(c.getKey(), new LinkedList<String>(c.getValue()));
							}
						}						
						// close tabs
						if ( result.close_channels != null ) {
							Iterator<String> i=result.close_channels.iterator();
							while ( i.hasNext() ) {
								String channel = i.next();
								if ( tabPanel.getWidget(tabPanel.getSelectedIndex()).equals(chan_scroll.get(channel)) ) 
									tabPanel.selectTab(0);
								tabPanel.remove(chan_scroll.get(channel));
								channels.remove(channel);
								chan_scroll.remove(channel);
								channel_user_lists.remove(channel);
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
		loginButton.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent event) {
				ircService.login(nick.getValue(), server.getValue(),port.getValue(),password.getValue(),new AsyncCallback<Boolean>() {
					public void onFailure(Throwable caught) {
						Window.alert(SERVER_ERROR);
					}
					public void onSuccess(Boolean result) {
						if ( result == true ) {
							pulseRunning=true;
							login.hide();
							runPulse();
						}
					}
				});
			}
		});		
	}
}
