package lan.sahara.webirc.client;

import lan.sahara.webirc.shared.IrcContainer;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("irc")
public interface IrcService  extends RemoteService {
	public IrcContainer getPulse() throws IllegalArgumentException;
	public Boolean reload() throws IllegalArgumentException;
	public Boolean login(String nick,String server,String port,String password) throws IllegalArgumentException;
	public Boolean logout() throws IllegalArgumentException;
	public Boolean send(String msg,String target) throws IllegalArgumentException;

}
