package lan.sahara.webirc.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lan.sahara.webirc.shared.IrcEntry;
import org.jibble.pircbot.*;

public class ClientIrcObject extends PircBot {
	public List<IrcEntry> msg_buffer = Collections.synchronizedList(new LinkedList<IrcEntry>());
	public List<IrcEntry> msg_buffer_cache = Collections.synchronizedList(new LinkedList<IrcEntry>());
	public List<String> close_channels = Collections.synchronizedList(new LinkedList<String>());
	public Map<String,List<String>> channel_user_lists = Collections.synchronizedMap(new HashMap<String,List<String>>());
	public Map<String,String> topic = Collections.synchronizedMap(new HashMap<String,String>());
	public Map<String,String> topic_cache = Collections.synchronizedMap(new HashMap<String,String>());
	public long seen;
	/*
	 * 1024 row buffer
	 */
	public void addMsg(IrcEntry e) {
		msg_buffer.add(e);
		msg_buffer_cache.add(e);
		if ( msg_buffer_cache.size() > 1024 ) {
			Map<Long,Integer> idx = new TreeMap<Long,Integer>();
			Iterator<IrcEntry> i=msg_buffer_cache.iterator();
			int c=0;
			while ( i.hasNext() ) 
				idx.put(i.next().timestamp, c++);
			Iterator<Integer> mi = idx.values().iterator();
			while (  msg_buffer_cache.size() > 1024 ) {
				msg_buffer_cache.remove(mi.next());
			}
		}
		
	}
	public Boolean checkStatus() {
		Boolean ret=false;
		if ( msg_buffer != null && msg_buffer.size() > 0 )
				ret=true;
		if (close_channels != null && close_channels.size() > 0 )
				ret=true;
		if ( channel_user_lists != null && channel_user_lists.size() > 0 )
				ret=true;
		if ( topic != null && topic.size() > 0 )
				ret=true;		
		return ret;
	}
	public ClientIrcObject(String nick,String server,String port,String password) {
		this.setLogin("webirc");
		this.setName(nick);
		this.setVerbose(false);
		try {
			this.setEncoding("UTF-8");
		} catch (UnsupportedEncodingException e1) {
			System.err.println(e1.getMessage());
		}
		try {
			this.connect(server,Integer.parseInt(port),password);
		} catch (NickAlreadyInUseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IrcException e) {
			this.disconnect();
		}
	}
	public void runReConnect (String nick,String server,String port,String password) {
		this.setName(nick);
		this.setVerbose(true);
		try {
			this.connect(server,Integer.parseInt(port),password);
		} catch (NickAlreadyInUseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IrcException e) {
			System.err.println(e.getMessage());
			this.disconnect();
		}
	}
	public void MyJoin(String channel) {
		this.joinChannel(channel);
	}
	public void MyLeave(String channel) {
		this.partChannel(channel);
	}	
	public void onServerResponse(int code, String response) {
		synchronized(this){
			if ( code != 353 && code != 366 )	 // skip userlist
				addMsg(new IrcEntry(System.currentTimeMillis(),code,"Server","Server","",response));
		}
	}
	public void onDisconnect(int code, String response) {
		addMsg(new IrcEntry(System.currentTimeMillis(),code,"Server","Server","",response));
		System.err.println("SERVER:"+response);
	}	
	public void onMessage(String channel, String sender,String login, String hostname, String message) {
		addMsg(new IrcEntry(System.currentTimeMillis(),0,channel,sender,login+"@"+hostname,message));
//		msg_buffer.add(new IrcEntry(System.currentTimeMillis(),0,channel,sender,login+"@"+hostname,message));
	}
	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		addMsg(new IrcEntry(System.currentTimeMillis(),0,sender,sender,login+"@"+hostname,message));
	}
	public void onServerPing(String response) {
		if ( (seen+60000) > System.currentTimeMillis() ) {  // we are still alive (<60sec) .. else let irc server disconnect
			this.sendRawLine("PONG " + response);
		}
	}
	public void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
		addMsg(new IrcEntry(System.currentTimeMillis(),0,"Server",sourceNick,sourceLogin+"@"+sourceHostname,"NOTICE: "+target+" "+notice));
	}
	public void onJoin(String channel, String sender, String login, String hostname) {
		addMsg(new IrcEntry(System.currentTimeMillis(),0,channel,sender,login+"@"+hostname,"Joined channel "+channel));	
	}
	// TODO: solve channels (from memory?) where "nick" was
	public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
/*		msg_buffer.add(new IrcEntry(System.currentTimeMillis(),0,channel,sourceNick,sourceLogin+"@"+sourceHostname,"Quit "+reason)); */
	}
	public void onPart(String channel, String sender, String login, String hostname) {
		addMsg(new IrcEntry(System.currentTimeMillis(),0,channel,sender,login+"@"+hostname,"Leave channel "+channel));
		if ( this.getNick().equals(sender) ) {
			close_channels.add(channel);
		}
	}
	public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
		msg_buffer.add(new IrcEntry(System.currentTimeMillis(),0,channel,sourceNick,sourceLogin+"@"+sourceHostname,"MODE: "+mode));
	}
	public void onUserList(String channel, User[] users) {
		for (User s : users ) {
			if ( ! channel_user_lists.containsKey(channel) ) 
				channel_user_lists.put(channel, new LinkedList<String>());
			channel_user_lists.get(channel).add(s.getNick());
		}
	}
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
		this.topic.put(channel,topic);
		this.topic_cache.put(channel,topic);
	}
}
