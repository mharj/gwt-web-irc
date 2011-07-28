package lan.sahara.webirc.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import lan.sahara.webirc.shared.IrcEntry;
import lan.sahara.webirc.shared.IrcUser;
import lan.sahara.webirc.shared.IrcUserListEntry;

import org.jibble.pircbot.*;

public class ClientIrcObject extends PircBot {
	public TreeMap<Long,IrcEntry> msg_buffer_cache = (TreeMap<Long, IrcEntry>) Collections.synchronizedMap(new TreeMap<Long,IrcEntry>());
	public Map<Long,IrcUserListEntry> userlist_buffer_cache = Collections.synchronizedMap(new TreeMap<Long,IrcUserListEntry>());
	public List<String> close_channels = Collections.synchronizedList(new LinkedList<String>());
	public Map<String, TreeMap<String, IrcUser>> channel_user_lists = Collections.synchronizedMap(new HashMap<String,TreeMap<String,IrcUser>>());
	public Map<String,String> topic = Collections.synchronizedMap(new HashMap<String,String>());
	public Map<String,String> topic_cache = Collections.synchronizedMap(new HashMap<String,String>());
	public long seen;
	/*
	 * 1024 row buffer
	 */
	public void addUserEvent(IrcUserListEntry e) {
		synchronized(userlist_buffer_cache) {
			try {
				userlist_buffer_cache.put(Calendar.getInstance().getTimeInMillis(), e);
				Thread.sleep(10);
			} catch (InterruptedException te) {
				te.printStackTrace();
			}
			Long ct=Calendar.getInstance().getTimeInMillis();
			for ( Entry<Long, IrcUserListEntry> c : userlist_buffer_cache.entrySet() ) {
				if ( (c.getKey()+60000) < ct ) // remove entries older than 60sec
					userlist_buffer_cache.remove(c.getKey());
				
			}
		}
	}
	
	public void addMsg(IrcEntry e) {
		synchronized(msg_buffer_cache){
			try {
				msg_buffer_cache.put(Calendar.getInstance().getTimeInMillis(), e);
				Thread.sleep(10);
			} catch (InterruptedException te) {
				te.printStackTrace();
			}
			while ( msg_buffer_cache.size() > 1024 ) 
				msg_buffer_cache.remove(msg_buffer_cache.firstKey());			
		}
	}
	public Boolean checkStatus(Long timestamp) {
		Boolean ret=false;
		// check new msg from buffer_cache
		synchronized(msg_buffer_cache){
			Iterator<Long> i=msg_buffer_cache.keySet().iterator();
			while ( i.hasNext()) {
				if ( timestamp < i.next() )
					ret=true;
			}
		}
		synchronized(userlist_buffer_cache) {
			Iterator<Long> i=userlist_buffer_cache.keySet().iterator();
			while ( i.hasNext()) {
				if ( timestamp < i.next() )
					ret=true;
			}			
		}
		if ( close_channels != null && close_channels.size() > 0 )
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
		this.setAutoNickChange(true);
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
		for ( User s : this.getUsers(channel) ) {
			if ( s.getNick().equals(sender) ) 
				addUserEvent(new IrcUserListEntry(IrcUserListEntry.E_JOIN,channel,sender,new IrcUser(s.getPrefix(),s.isOp(),s.hasVoice())));
		}
		addMsg(new IrcEntry(System.currentTimeMillis(),0,channel,sender,login+"@"+hostname,"Joined channel "+channel));	
	}
	public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
		addUserEvent(new IrcUserListEntry(IrcUserListEntry.E_QUIT,null,sourceNick,null));
		addMsg(new IrcEntry(System.currentTimeMillis(),0,"Server",sourceNick,sourceLogin+"@"+sourceHostname,"Quit "+reason)); 
	}
	public void onPart(String channel, String sender, String login, String hostname) {
		addUserEvent(new IrcUserListEntry(IrcUserListEntry.E_PART,channel,sender,null));
		addMsg(new IrcEntry(System.currentTimeMillis(),0,channel,sender,login+"@"+hostname,"Leave channel "+channel));
		if ( this.getNick().equals(sender) ) 
			close_channels.add(channel);
	}
	public void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
		addMsg(new IrcEntry(System.currentTimeMillis(),0,channel,sourceNick,sourceLogin+"@"+sourceHostname,"MODE: "+mode));
	}

	public void onNickChange(String oldNick, String login, String hostname, String newNick) {
		if ( this.getNick().equals(newNick) ) 
			addMsg(new IrcEntry(System.currentTimeMillis(),0,"Server",oldNick,login+"@"+hostname,"Nick changed to "+newNick));
		for ( String chan : this.getChannels() ) {
			for ( User u : this.getUsers(chan) ) {
				if ( u.getNick() == newNick ) {
					addUserEvent(new IrcUserListEntry(IrcUserListEntry.E_JOIN,chan,newNick,new IrcUser(u.getPrefix(),u.isOp(),u.hasVoice())));
					addUserEvent(new IrcUserListEntry(IrcUserListEntry.E_PART,chan,oldNick,null));
					addMsg(new IrcEntry(System.currentTimeMillis(),0,chan,oldNick,login+"@"+hostname,"Nick changed to "+newNick));
				}
			}
		}
	}
	public void onUserList(String channel, User[] users) {
		for (User s : users ) {
			if ( ! channel_user_lists.containsKey(channel) ) 
				channel_user_lists.put(channel, new TreeMap<String,IrcUser>());
			channel_user_lists.get(channel).put(s.getNick(),new IrcUser(s.getPrefix(),s.isOp(),s.hasVoice()));
		}
	}
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
		this.topic.put(channel,topic);
		this.topic_cache.put(channel,topic);
	}
}
