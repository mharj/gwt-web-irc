package lan.sahara.webirc.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class IrcEntry implements IsSerializable {
	public Long timestamp;
	public Integer code;
	public String channel;
	public String msg;
	public String user;
	public String host;
	public IrcEntry() {}
	public IrcEntry(Long _timestamp,Integer _code,String _channel,String _user,String _host,String _msg) {
		this.timestamp=_timestamp;
		this.channel=_channel;
		this.msg=_msg;
		this.code=_code;
		this.user=_user;
		this.host=_host;
	}
}
