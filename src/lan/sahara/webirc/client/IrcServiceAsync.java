package lan.sahara.webirc.client;

import lan.sahara.webirc.shared.IrcContainer;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface IrcServiceAsync {
	void getPulse(AsyncCallback<IrcContainer> callback) throws IllegalArgumentException;
	void reload(AsyncCallback<Boolean> callback) throws IllegalArgumentException;
	void login(String nick,String server,String port,String password,AsyncCallback<Boolean> callback) throws IllegalArgumentException;
	void logout(AsyncCallback<Boolean> callback) throws IllegalArgumentException;
	void send(String msg,String target,AsyncCallback<Boolean> callback) throws IllegalArgumentException;
}
