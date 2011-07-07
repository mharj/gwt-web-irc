package lan.sahara.webirc.server;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.http.HttpSession;
import org.jibble.pircbot.User;
import lan.sahara.webirc.client.IrcService;
import lan.sahara.webirc.shared.IrcContainer;
import lan.sahara.webirc.shared.IrcEntry;
import lan.sahara.webirc.shared.IrcUser;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class IrcServiceImpl extends RemoteServiceServlet implements IrcService {
	Map <String,ClientIrcObject> ircConnections = Collections.synchronizedMap(new HashMap<String,ClientIrcObject>());
	public IrcServiceImpl() {
		System.err.println("Starting IrcServiceImpl");
		// session check timer
		Timer sessionTimer = new Timer();
		sessionTimer.schedule(new TimerTask(){
			public void run() {
				synchronized(ircConnections) {
					for ( Entry<String, ClientIrcObject> c : ircConnections.entrySet() ) {
						if ( (c.getValue().seen + 120000) < Calendar.getInstance().getTimeInMillis() ) {
							c.getValue().quitServer("Timeout");
							c.getValue().disconnect();
							System.err.println("removed(time):"+c.getKey());
							ircConnections.remove(c.getKey());
						}
					}
				}
			}
		}, 0, 10000);
	}
	
	/*
	 * TODO: use timestamp to get missing
	 * @see lan.sahara.webirc.client.IrcService#getPulse(java.lang.Long)
	 */
	public IrcContainer getPulse(Long timestamp) throws IllegalArgumentException {
		HttpSession session=this.getThreadLocalRequest().getSession();
		String sid=session.getId();		
		if ( ! ircConnections.containsKey(sid) ) {
			throw new IllegalArgumentException("relogin");
		}
		ircConnections.get(sid).seen =  System.currentTimeMillis();
		long wait=Calendar.getInstance().getTimeInMillis()+30000; // 30sec pulse
		while ( ircConnections.containsKey(sid) && ircConnections.get(sid).checkStatus(timestamp) != true  && wait > Calendar.getInstance().getTimeInMillis() ) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new IllegalArgumentException("restart"); // disable spamming
			}
		}
		if ( ! ircConnections.containsKey(sid) ) // we did quit (button)
			return null;
		if ( (Boolean)session.getAttribute("reload") != null ) { // we have reload going
			session.removeAttribute("reload");
			return null;
		}
		if ( ircConnections.get(sid).checkStatus(timestamp) == true ) {
			IrcContainer ret = new IrcContainer();
			synchronized(ircConnections.get(sid)){
				if ( ircConnections.get(sid).close_channels.size() > 0 ) {
					ret.close_channels = new LinkedList<String>(ircConnections.get(sid).close_channels);
					ircConnections.get(sid).close_channels.clear();
				}
				if ( ircConnections.get(sid).channel_user_lists.size() > 0 ) {
					ret.channel_user_lists = new HashMap<String,Map<String,IrcUser>>(ircConnections.get(sid).channel_user_lists);
					ircConnections.get(sid).channel_user_lists.clear();
				}
				if ( ircConnections.get(sid).topic.size() > 0 ) {
					ret.topic = new HashMap<String,String>(ircConnections.get(sid).topic);
					ircConnections.get(sid).topic.clear();
				}
				// read message buffer
				for ( Entry<Long, IrcEntry> c : ircConnections.get(sid).msg_buffer_cache.entrySet() ) {
					if ( timestamp < c.getKey()  ) {
						if ( ret.msg_buffer == null )
							ret.msg_buffer = new LinkedList<IrcEntry>();
						ret.msg_buffer.add(c.getValue());
						// let's limit max lines with one pulse
						if ( ret.msg_buffer.size() > 100 ) 
							break;
					}
				}
				ret.timestamp=Calendar.getInstance().getTimeInMillis();
			}
			return ret;
		}
		return null;
	}
	
	public Boolean reload() throws IllegalArgumentException {
		HttpSession session=this.getThreadLocalRequest().getSession();
		String sid=session.getId();
		if ( ircConnections.containsKey(sid) ) {
			session.setAttribute("reload", true);
			synchronized(ircConnections.get(sid)){
				ircConnections.get(sid).seen =  System.currentTimeMillis();
				String channels[] = ircConnections.get(sid).getChannels();
				for ( String chan : channels ) {
					Map<String,IrcUser> users = new TreeMap<String,IrcUser>();
					for ( User u : ircConnections.get(sid).getUsers(chan) ) 
						users.put(u.getNick(),new IrcUser(u.getPrefix(),u.isOp(),u.hasVoice()) );
					ircConnections.get(sid).channel_user_lists.put(chan,users);
					// load topics from topic_cache
					for ( Entry<String, String> c : ircConnections.get(sid).topic_cache.entrySet() ) 
						ircConnections.get(sid).topic.put(c.getKey(),c.getValue());
				}
			}
			return true;
		}
		return false;
	}
	
	public Boolean login(String nick, String server,String port, String password) throws IllegalArgumentException {
		HttpSession session=this.getThreadLocalRequest().getSession();
		String sid=session.getId();
		if ( ! ircConnections.containsKey(sid) ) {
			ircConnections.put(sid, new ClientIrcObject(nick,server,port,password));
			ircConnections.get(sid).seen =  System.currentTimeMillis();
			System.err.println("connected:"+sid);
		} else {
			ircConnections.get(sid).runReConnect(nick,server,port,password);
		}
		return true;
	}
	
	public Boolean logout() throws IllegalArgumentException {
		HttpSession session=this.getThreadLocalRequest().getSession();
		if ( session != null ) {
			String sid=session.getId();
			if ( ircConnections.containsKey(sid) ) {
				ircConnections.get(sid).quitServer("Bye!");
				ircConnections.get(sid).disconnect();
				ircConnections.remove(sid);
				System.err.println("removed(button):"+sid);
			}
			session.invalidate();
		}
		return null;
	}

	public Boolean send(String msg,String target) throws IllegalArgumentException {
		HttpSession session=this.getThreadLocalRequest().getSession();
		String sid=session.getId();		
		if ( ircConnections.containsKey(sid) ) {
			// join channel
			if ( msg.matches("^/join .*?")) {
				String p[]=msg.split(" ");
				if ( p.length > 1 )
					ircConnections.get(sid).MyJoin(p[1]);
				return true;
			}
			// leave channel
			if ( msg.matches("^/leave .*?")) {
				String p[]=msg.split(" ");
				if ( p.length > 1 )
					ircConnections.get(sid).MyLeave(p[1]);
				return true;
			}
			// set topic
			if ( msg.matches("^/topic .*?")) {
				String p[]=msg.split(" ");
				if ( p.length > 1 ) {
					StringBuffer real_msg = new StringBuffer();
					for ( int i=1;i<p.length;i++) {
						if ( real_msg.length() == 0 )
							real_msg.append(p[i]);
						else
							real_msg.append(" "+p[i]);
					}
					ircConnections.get(sid).setTopic(target, real_msg.toString() );
					ircConnections.get(sid).addMsg(new IrcEntry(System.currentTimeMillis(),0,target,ircConnections.get(sid).getNick(),ircConnections.get(sid).getLogin()+"@"+ircConnections.get(sid).getInetAddress(),"sets topic: "+real_msg.toString()));
				}
				return true;
			}
			// set nick
			if ( msg.matches("^/nick .*?")) {
				String p[]=msg.split(" ");
				if ( p.length > 1 ) 
					ircConnections.get(sid).changeNick(p[1]);
				return true;
			}
			// priv msg (open tab for output with msg_buffer)
			if ( msg.matches("^/msg .*?")) {
				String p[]=msg.split(" ");
				if ( p.length > 1 ) {
					StringBuffer real_msg = new StringBuffer();
					for ( int i=2;i<p.length;i++) {
						if ( real_msg.length() == 0 )
							real_msg.append(p[i]);
						else
							real_msg.append(" "+p[i]);
					}
					ircConnections.get(sid).sendMessage(p[1], real_msg.toString());
					ircConnections.get(sid).addMsg(new IrcEntry(System.currentTimeMillis(),0,p[1],ircConnections.get(sid).getNick(),ircConnections.get(sid).getLogin()+"@"+ircConnections.get(sid).getInetAddress(),real_msg.toString()));
				}
				return true;
			}
			// we are sending to channel or priv
			if( ! target.matches("Server")  ) {
				ircConnections.get(sid).sendMessage(target, msg);
				ircConnections.get(sid).addMsg(new IrcEntry(System.currentTimeMillis(),0,target,ircConnections.get(sid).getNick(),ircConnections.get(sid).getLogin()+"@"+ircConnections.get(sid).getInetAddress(),msg));
			}
		}
		return false;
	}
}
