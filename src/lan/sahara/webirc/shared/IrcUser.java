package lan.sahara.webirc.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class IrcUser implements IsSerializable {
	public String host;
	public Boolean oper=false;
	public Boolean voice=false;
	public IrcUser() {}
	public IrcUser(String _host,Boolean _oper,Boolean _voice) {
		this.host = _host;
		this.oper = _oper;
		this.voice = _voice;
	}
}
