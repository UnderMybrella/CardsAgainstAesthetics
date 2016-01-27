package org.abimon.caa;

import java.util.LinkedList;

public class User {
	
	static String winner = "";
	
	String ip = "";
	String username = "";
	
	String[] cardSelection = null;
	
	LinkedList<WhiteCard> cards = new LinkedList<WhiteCard>();
	
	boolean isCzar = false;
	
	public User(String ip){
		this.ip = ip;
	}
	
	public void setUser(String username){
		this.username = username;
	}
	
	@Override
	public int hashCode(){
		return (ip + ":" + username).hashCode();
	}
}
