package lan.sahara.webirc.shared;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;
import com.google.gwt.user.client.rpc.IsSerializable;

public class IrcContainer implements IsSerializable {
	public IrcContainer() {}
	public LinkedList<IrcEntry> msg_buffer = null;
	public TreeMap<Long,IrcUserListEntry> userlist_buffer = null;
	public LinkedList<String> close_channels = null;
	public HashMap<String,TreeMap<String,IrcUser>> channel_user_lists = null;
	public HashMap<String,String> topic = null;
	public Long timestamp = null;
}
