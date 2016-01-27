package org.abimon.caa;

public class BlackCard implements Cloneable{

	String text;
	String set;
	String setID;

	int pick;
	int draw;

	public BlackCard(String text, String set, String setID, int pick, int draw){
		this.text = text;
		this.set = set;
		this.setID = setID;

		this.pick = pick;
		this.draw = draw;
	}

	public String getText(){
		return text;
	}

	public String getSet(){
		return set;
	}

	public String getSetID(){
		return setID;
	}

	public int getPick(){
		return pick;
	}

	public int getDraw(){
		return draw;
	}

	public String getNicelyFormatted(WhiteCard[] cards){
		String formatted = text;
		for(WhiteCard c : cards)
			formatted = formatted.replaceFirst("____", c.getName());

		return formatted;
	}

	public String getNicelyFormatted(String[] cards){
		if(cards == null)
			return text;
		String formatted = text;
		if(formatted.contains("____"))
			for(String c : cards)
				if(c != null)
					if(c.endsWith("."))
						formatted = formatted.replaceFirst("____", c.substring(0, c.length() - 1));
					else
						formatted = formatted.replaceFirst("____", c);
				else;
		else
			formatted += " " + cards[0];


		return formatted;
	}

	public String toString(){
		return set + " (" + setID + "): " + text;
	}

	@Override
	public int hashCode(){
		return (text + ":" + setID).hashCode();
	}

	@Override
	public BlackCard clone(){
		return new BlackCard(text, set, setID, pick, draw);
	}
}
