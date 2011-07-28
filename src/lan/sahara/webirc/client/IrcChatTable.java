package lan.sahara.webirc.client;

import java.util.TreeMap;
import java.util.Map.Entry;
import lan.sahara.webirc.shared.IrcUser;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;

public class IrcChatTable extends ScrollPanel {
	private String name;
	public TreeMap<String,IrcUser> user_list = new TreeMap<String,IrcUser>();
	public FlexTable user_list_table = new FlexTable();
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
	public void rebuildUserWidget() {
		user_list_table.clear();
		Integer count = 0;
		for (Entry<String, IrcUser> c : this.user_list.entrySet()) {
			StringBuffer id = new StringBuffer();
			if (c.getValue().oper == true)
				id.append("&#9679;");
			if (c.getValue().voice == true)
				id.append("&#9675;");
			HTML h_id =  new HTML(id.toString());
			Label h_uid = new Label(c.getKey());
			h_id.setStyleName("WhiteText");
			h_uid.setStyleName("WhiteText");
			user_list_table.setWidget(count, 0,h_id);
			user_list_table.setWidget(count, 1,h_uid);
			count++;
		}		
	}
}
