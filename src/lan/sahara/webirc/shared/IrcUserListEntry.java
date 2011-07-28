package lan.sahara.webirc.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class IrcUserListEntry implements IsSerializable {
	public static Integer E_JOIN = 0;
	public static Integer E_PART = 1;
	public static Integer E_QUIT = 2;
	public Integer type;
	public String channel;
	public String nick;
	public IrcUser user;
	public IrcUserListEntry(){}
	public IrcUserListEntry(Integer _type,String _channel,String _nick,IrcUser _user) {
		this.type=_type;
		this.channel=_channel;
		this.nick=_nick;
		this.user=_user;
	}
}

