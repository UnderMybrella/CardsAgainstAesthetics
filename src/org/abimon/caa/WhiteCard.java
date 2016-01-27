package org.abimon.caa;

public class WhiteCard implements Cloneable{
	
	String name;
	String set;
	String setID;
	boolean cardCast = false;
	
	public WhiteCard(String name, String set, String setID){
		this.name = name;
		this.set = set;
		this.setID = setID;
	}
	
	public String getName(){
		return name;
	}
	
	public String getSet(){
		return set;
	}
	
	public String getSetID(){
		return setID;
	}
	
	public String toString(){
		return set + " (" + setID + "): " + name;
	}
	
	@Override
	public WhiteCard clone(){
		return new WhiteCard(name, set, setID);
	}
	
	//public void handle();
}
