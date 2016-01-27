package org.abimon.caa;

import java.util.LinkedList;

public class UserList extends LinkedList<User>
{
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean contains(Object o){
		if(o instanceof User){
			return super.contains(o);
		}
		if(o instanceof String){
			for(User user : this)
				if(user.ip.equalsIgnoreCase((String) o))
					return true;
		}
		return false;
	}
	
	public User addUser(User user){
		super.add(user);
		return user;
	}
	
	public User getByAddress(String addr){
		for(User user : this)
			if(user.ip.equalsIgnoreCase(addr))
				return user;
		return addUser(new User(addr));
	}
	
	public int getIndexByAddress(String addr){
		for(int i = 0; i < size(); i++)
			if(get(i).ip.equalsIgnoreCase(addr))
				return i;
		add(new User(addr));
		return getIndexByAddress(addr);
	}
	
	public int usersSubmittedCards(){
		int counter = 0;
		for(User user : this)
			if((user.cardSelection != null && user.cardSelection.length > 0) || user.isCzar)
				counter++;
		return counter;
	}
	
	public int usersWaitingForCards(){
		return size() - usersSubmittedCards();
	}
}
