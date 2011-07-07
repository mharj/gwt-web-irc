package lan.sahara.webirc.client;

import java.util.Map;
import java.util.TreeMap;

import lan.sahara.webirc.shared.IrcUser;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;

public class IrcChatTable extends ScrollPanel {
	private String name;
	public Map<String,IrcUser> user_list = new TreeMap<String,IrcUser>();
	public FlexTable chat_table = new FlexTable();
	public Label topic = new Label("");
	public IrcChatTable() {
		super();
		this.setWidget(this.chat_table);
		this.setStyleName("scroll_list");
		topic.setStyleName("WhiteText");
	}
	public IrcChatTable(String _name) {
		super();
		this.setWidget(this.chat_table);
		this.setStyleName("scroll_list");
		this.name = _name;
	}	
	public void setName(String _name){
		this.name = _name;
	}
	public String getName(){
		return this.name;
	}
	public void limitChatCount(Integer max) {
		while (chat_table.getRowCount() > max  )  
			chat_table.removeRow(0);
	}
}
