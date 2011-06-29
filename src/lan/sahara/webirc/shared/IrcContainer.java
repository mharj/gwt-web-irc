package lan.sahara.webirc.shared;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class IrcContainer implements IsSerializable {
	public IrcContainer() {}
	public List<IrcEntry> msg_buffer = null;
	public List<String> close_channels = null;
	public Map<String,List<String>> channel_user_lists = null;
	public Map<String,String> topic = null;

}
