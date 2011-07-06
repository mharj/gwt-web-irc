package lan.sahara.webirc.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class IrcUser implements IsSerializable {
	public String host;
	public IrcUser() {}
	public IrcUser(String _host ) {
		this.host = _host;
	}
}
