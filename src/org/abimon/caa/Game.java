package org.abimon.caa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class Game {

	private static final boolean DUMP = true;

	public static Scanner in;

	public static int CARDS_IN_HAND = 9;

	public static UserList users = new UserList();

	public static File decks = new File("decks");
	public static File deckUsage = new File("deck_usage.json");

	public static Proxy proxy = Proxy.NO_PROXY;

	public static LinkedList<BlackCard> blackCards = new LinkedList<BlackCard>();
	public static LinkedList<WhiteCard> whiteCards = new LinkedList<WhiteCard>();

	public static Gson gson = new Gson();

	public static boolean gameStarted = false;

	public static void main(String[] args) throws Throwable{
		Game.hackSslVerifier();

		if(!decks.exists())
			decks.mkdirs();

		if(!deckUsage.exists())
		{
			deckUsage.createNewFile();
			JsonObject json = new JsonObject();
			for(File deck : decks.listFiles())
				if(deck.getName().endsWith(".json"))
					json.add(deck.getName().substring(0, 5), new JsonPrimitive(0));
			PrintStream deckOut = new PrintStream(deckUsage);
			deckOut.println(gson.toJson(json));
			deckOut.close();
		}

		if(DUMP)
			dump();

		in = new Scanner(System.in);
		System.out.println("First of all, what is the address of the proxy to use? Leave blank for no proxy");
		System.out.print("> ");
		String proxyAddr = in.nextLine();
		if(!proxyAddr.equals(""))
		{
			System.out.println("Next, what is the port of the proxy?");
			System.out.print("> ");
			int port = Integer.parseInt(in.nextLine());
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddr, port));
			System.setProperty("http.proxyHost", proxyAddr);
			System.setProperty("http.proxyPort", port + "");
			System.setProperty("https.proxyHost", proxyAddr);
			System.setProperty("https.proxyPort", port + "");
			System.out.println("Next question, what is the username required for authentication? Leave blank for no username");
			System.out.print("> ");
			final String username = in.nextLine();
			if(!username.equals(""))
			{
				System.out.println("Finally, what is the password used by this account?");
				System.out.print("> ");
				final char[] pass = System.console().readPassword();

				Authenticator authenticator = new Authenticator() {

					public PasswordAuthentication getPasswordAuthentication() {
						return (new PasswordAuthentication(username, pass));
					}
				};
				Authenticator.setDefault(authenticator);
			}
		}
		new Thread(){
			public void run(){
				new Webserver();
			}
		}.start();
		System.out.println("How many cards are to be held by each player? (Default: 10)");
		System.out.print("> ");
		CARDS_IN_HAND = Integer.parseInt(in.nextLine());
		while(true){
			try{
				System.out.println("Type 'start' to start the game.");
				System.out.println("Type 'add <deck>' to add a deck.");
				System.out.println("Type 'remove <deck>' to add a deck.");
				System.out.println("Type 'cards' to list all available cards.");
				System.out.println("Type 'decks' to list available offline decks.");
				System.out.println("'add <deck>' works with Card cast decks by the way.");
				System.out.println("Type 'update <deck>' to redownload a cardcast deck.");
				System.out.print("> ");
				String line = in.nextLine().toLowerCase();
				if(line.startsWith("add ")){
					String[] codes = line.substring(4).split("\\s+");
					for(String code : codes){
						code = code.toUpperCase().trim();
						File deckLoc = new File(decks.getAbsolutePath() + File.separator + code + ".json");
						if(deckLoc.exists())
						{
							System.out.println("Deck exists!");
							JsonObject json = new JsonParser().parse(new FileReader(deckLoc)).getAsJsonObject();
							System.out.println("Name: " + json.get("name").getAsString());
							System.out.println("Desc: " + json.get("desc").getAsString());
							System.out.println("Load deck?");
							System.out.print("> ");
							String response = in.next().toLowerCase();
							if(response.equals("yes"))
							{
								File blackCardFile = new File(json.get("black_cards").getAsString());
								File whiteCardFile = new File(json.get("white_cards").getAsString());

								BufferedReader blackReader = new BufferedReader(new FileReader(blackCardFile));
								String blackCard = null;
								while((blackCard = blackReader.readLine()) != null){
									int pick = instancesOf(blackCard, "____");
									pick = Math.max(1, pick); 
									Game.blackCards.add(new BlackCard(blackCard, json.get("name").getAsString(), code, pick, (pick >= 3 ? pick - 1 : 0)));
								}

								BufferedReader whiteReader = new BufferedReader(new FileReader(whiteCardFile));
								String whiteCard = null;
								while((whiteCard = whiteReader.readLine()) != null){
									Game.whiteCards.add(new WhiteCard(whiteCard, json.get("name").getAsString(), code));
								}

								blackReader.close();
								whiteReader.close();

								JsonObject deckJson = new JsonParser().parse(new FileReader(deckUsage)).getAsJsonObject();
								if(deckJson.has(code))
									deckJson.add(code, new JsonPrimitive(deckJson.get(code).getAsInt() + 1));
								else
									deckJson.add(code, new JsonPrimitive(1));
								PrintStream deckOut = new PrintStream(deckUsage);
								deckOut.println(gson.toJson(deckJson));
								deckOut.close();
							}
						}
						else
						{
							String filter = code.toLowerCase().replace("_", " ");
							LinkedList<String> deckNames = new LinkedList<String>();
							LinkedList<String> deckDescriptions = new LinkedList<String>();
							LinkedList<String> deckCodes = new LinkedList<String>();
							for(File deck : decks.listFiles())
								if(deck.isFile() && deck.getName().endsWith(".json")){
									JsonObject json = new JsonParser().parse(new FileReader(deck)).getAsJsonObject();
									String name = json.get("name").getAsString();
									if(name.toLowerCase().matches(filter) || name.toLowerCase().startsWith(filter) || name.toLowerCase().contains(filter))
									{
										deckNames.add(name + " (" + deck.getName().substring(0, 5) + ")");
										deckDescriptions.add(json.get("desc").getAsString());
										deckCodes.add(deck.getName());
									}
								}
							if(deckNames.isEmpty()){
								System.out.println("Deck does not exist! Attempting to locate deck on Cardcast...");
								CardCast.findAndDownloadDeck(code);
							}
							else
							{
								System.out.println("Decks found for that filter: ");
								for(int i = 0; i < deckNames.size(); i++){
									System.out.println("\t" + deckNames.get(i));
									System.out.println("\t\t" + deckDescriptions.get(i));
								}

								System.out.println("Add all?");
								System.out.print("> ");
								String response = in.next().toLowerCase();
								if(response.equals("yes"))
									for(String deckCode : deckCodes)
									{
										JsonObject json = new JsonParser().parse(new FileReader(new File(decks + File.separator + deckCode))).getAsJsonObject();
										File blackCardFile = new File(json.get("black_cards").getAsString());
										File whiteCardFile = new File(json.get("white_cards").getAsString());

										BufferedReader blackReader = new BufferedReader(new FileReader(blackCardFile));
										String blackCard = null;
										while((blackCard = blackReader.readLine()) != null){
											int pick = instancesOf(blackCard, "____");
											pick = Math.max(1, pick); 
											Game.blackCards.add(new BlackCard(blackCard, json.get("name").getAsString(), deckCode.toUpperCase().substring(0, 5), pick, (pick >= 3 ? pick - 1 : 0)));
										}

										BufferedReader whiteReader = new BufferedReader(new FileReader(whiteCardFile));
										String whiteCard = null;
										while((whiteCard = whiteReader.readLine()) != null){
											Game.whiteCards.add(new WhiteCard(whiteCard, json.get("name").getAsString(), deckCode.toUpperCase().substring(0, 5)));
										}

										blackReader.close();
										whiteReader.close();

										JsonObject deckJson = new JsonParser().parse(new FileReader(deckUsage)).getAsJsonObject();
										if(deckJson.has(code))
											deckJson.add(code, new JsonPrimitive(deckJson.get(code).getAsInt() + 1));
										else
											deckJson.add(code, new JsonPrimitive(1));
										PrintStream deckOut = new PrintStream(deckUsage);
										deckOut.println(gson.toJson(deckJson));
										deckOut.close();
									}
							}
						}
					}
				}
				else if(line.equals("cards")){
					for(BlackCard card : blackCards)
						System.out.println(card);
					for(WhiteCard card : whiteCards)
						System.out.println(card);
				}
				else if(line.equals("decks")){
					System.out.println("Decks: ");
					LinkedList<String> deckNames = new LinkedList<String>();
					LinkedList<String> deckCodes = new LinkedList<String>();
					for(File deck : decks.listFiles())
						if(deck.isFile() && deck.getName().endsWith(".json")){
							JsonObject json = new JsonParser().parse(new FileReader(deck)).getAsJsonObject();
							deckNames.add(json.get("name").getAsString() + " (" + deck.getName().substring(0, 5) + ")");
							deckCodes.add(deck.getName().substring(0, 5));
						}

					JsonObject deckJson = new JsonParser().parse(new FileReader(deckUsage)).getAsJsonObject();

					int loops = 0;
					while(loops < 5)
					{
						loops++;
						for(int i = 1; i < deckNames.size(); i++)
						{
							String code = deckCodes.get(i);
							String codeBefore = deckCodes.get(i-1);

							String name = deckNames.get(i);
							String nameBefore = deckNames.get(i-1);

							if(deckJson.get(code).getAsInt() > deckJson.get(codeBefore).getAsInt())
							{
								deckCodes.set(i, codeBefore);
								deckCodes.set(i-1, code);

								deckNames.set(i, nameBefore);
								deckNames.set(i-1, name);

								loops = 0;
							}
						}
					}

					for(String s : deckNames)
						System.out.println("\t" + s);
				}
				else if(line.startsWith("decks")){
					String filter = line.split("\\s+", 2)[1];
					System.out.println();
					System.out.println("Decks: ");
					LinkedList<String> deckNames = new LinkedList<String>();
					LinkedList<String> deckCodes = new LinkedList<String>();
					for(File deck : decks.listFiles())
						if(deck.isFile() && deck.getName().endsWith(".json")){
							JsonObject json = new JsonParser().parse(new FileReader(deck)).getAsJsonObject();
							String name = json.get("name").getAsString();
							if(name.toLowerCase().matches(filter) || name.toLowerCase().startsWith(filter) || name.toLowerCase().contains(filter))
							{
								deckNames.add(name + " (" + deck.getName().substring(0, 5) + ")");
								deckCodes.add(deck.getName().substring(0, 5));
							}
						}

					JsonObject deckJson = new JsonParser().parse(new FileReader(deckUsage)).getAsJsonObject();

					int loops = 0;
					while(loops < 5)
					{
						loops++;
						for(int i = 1; i < deckNames.size(); i++)
						{
							String code = deckCodes.get(i);
							String codeBefore = deckCodes.get(i-1);

							String name = deckNames.get(i);
							String nameBefore = deckNames.get(i-1);

							if(deckJson.get(code).getAsInt() > deckJson.get(codeBefore).getAsInt())
							{
								deckCodes.set(i, codeBefore);
								deckCodes.set(i-1, code);

								deckNames.set(i, nameBefore);
								deckNames.set(i-1, name);

								loops = 0;
							}
						}
					}

					for(String s : deckNames)
						System.out.println("\t" + s);
					System.out.println();
				}
				else if(line.equals("start"))
					playGame();
				else
					System.out.println("Invalid Input");
			}
			catch(Throwable th){
				th.printStackTrace();
			}
		}

	}

	public static int instancesOf(String line, String regex){
		return (line + "abcdefg").split(regex).length - 1;
	}

	public static LinkedList<? extends Object> shuffle(LinkedList<? extends Object> shuffling){
		LinkedList<Object> shuffled = new LinkedList<Object>();

		while(!shuffling.isEmpty())
			shuffled.add(shuffling.remove(new Random().nextInt(shuffling.size())));

		return shuffled;
	}

	public static LinkedList<? extends Object> shuffleAndClone(LinkedList<? extends Cloneable> shuffling){
		LinkedList<Object> shuffled = new LinkedList<Object>();
		LinkedList<Object> cloned = new LinkedList<Object>();

		for(Object clone : shuffling){
			try{
				Method cloneMethod = clone.getClass().getDeclaredMethod("clone");
				cloned.add(cloneMethod.invoke(clone));
			}
			catch(Throwable th){
				th.printStackTrace();
			}
		}

		while(!cloned.isEmpty())
			shuffled.add(cloned.remove(new Random().nextInt(cloned.size())));

		return shuffled;
	}

	public static LinkedList<WhiteCard> whiteDeck;
	public static LinkedList<BlackCard> blackDeck;
	public static BlackCard currentBlackCard = new BlackCard("Why did this card appear?", "Default", "DECAF", 1, 0);

	@SuppressWarnings("unchecked")
	public static void playGame(){
		whiteDeck = (LinkedList<WhiteCard>) shuffleAndClone(whiteCards);
		blackDeck = (LinkedList<BlackCard>) shuffleAndClone(blackCards);

		users.getFirst().isCzar = true;

		for(User user : users)
			while(user.cards.size() < Game.CARDS_IN_HAND)
				user.cards.add(whiteDeck.poll());
		System.out.println(blackDeck.peek());
		currentBlackCard = blackDeck.poll();
		Webserver.currentCard = currentBlackCard;
		System.out.println(currentBlackCard);
		Game.gameStarted = true;
		while(true){
			Webserver.cards = new String[Game.users.size()];
			Webserver.waitingOnCzar = false;
			Webserver.userPickedCzar = "";
			while(users.size() == 0 || users.usersWaitingForCards() > 0){
				try{
					Thread.sleep(1000);
					if(System.currentTimeMillis() % 10000 == 0)
						System.out.println("Waiting on " + users.usersWaitingForCards());
				}
				catch(Throwable th){
					th.printStackTrace();
				}
			}
			Webserver.waitingOnCzar = true;
			//For now, the console will be the Czar
			while(Webserver.userPickedCzar.equals(""))
			{
				try{
					Thread.sleep(1000);
				}catch(Throwable th){}
			}

			currentBlackCard = blackDeck.poll();
			Webserver.currentCard = currentBlackCard;
			System.out.println(currentBlackCard);
			boolean nextCzar = false;
			boolean checkCzar = false;
			for(User user : users)
			{
				if(nextCzar){
					user.isCzar = true;
					nextCzar = false;
				}
				if(user.isCzar && !checkCzar){
					nextCzar = true;
					checkCzar = true;
				}
				user.cardSelection = null;

				while(user.cards.contains(null))
					user.cards.remove(null);

				while(user.cards.size() < Game.CARDS_IN_HAND){
					user.cards.add(whiteDeck.poll());
				}
			}
			Webserver.waitingOnCzar = false;
		}
		//Game.gameStarted = false;
	}

	private static void dump() throws Throwable{
		File f = new File("cah_cards.sql");

		File setDirsBlack = new File(decks.getAbsolutePath() + File.separator + "black_cards");
		if(!setDirsBlack.exists())
			setDirsBlack.mkdir();

		File setDirsWhite = new File(decks.getAbsolutePath() + File.separator + "white_cards");
		if(!setDirsWhite.exists())
			setDirsWhite.mkdir();

		HashMap<String, PrintStream> writeOutBlack = new HashMap<String, PrintStream>();
		HashMap<String, PrintStream> writeOutWhite = new HashMap<String, PrintStream>();

		FileInputStream in = new FileInputStream(f);
		byte[] data = new byte[in.available()];
		in.read(data);
		in.close();

		String[] lines = new String(data).split("\n");

		for(String line : lines){
			if(line.startsWith("INSERT INTO card_set VALUES"))
			{
				String[] params = line.replace("INSERT INTO card_set VALUES (", "").replace(");", "").split(", ");
				String name = params[2].replace("'", "").replace("/", "-");
				File cardSetBlack = new File(setDirsBlack.getAbsolutePath() + File.separator + name + ".txt");
				if(!cardSetBlack.exists())
					cardSetBlack.createNewFile();
				File cardSetWhite = new File(setDirsWhite.getAbsolutePath() + File.separator + name + ".txt");
				if(!cardSetWhite.exists())
					cardSetWhite.createNewFile();

				JsonObject json = new JsonObject();
				json.add("black_cards", new JsonPrimitive(cardSetBlack.getAbsolutePath()));
				json.add("white_cards", new JsonPrimitive(cardSetWhite.getAbsolutePath()));
				json.add("name", new JsonPrimitive(params[2].replace("'", "")));
				json.add("desc", new JsonPrimitive(params[4]));

				char[] set = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();

				String fileName = "";
				for(int i = 0; i < 5; i++)
					fileName += set[new Random((name + (i ^ name.length())).hashCode()).nextInt(set.length)];
				File deck = new File(decks.getAbsolutePath() + File.separator + fileName + ".json");
				if(!deck.exists())
				{
					System.out.println(fileName + " is now in use thanks to " + name);
					deck.createNewFile();
				}
				else
					System.out.println("How dare they! " + fileName + " is already in use. Thanks " + name);

				PrintStream out = new PrintStream(deck);
				out.println(gson.toJson(json));
				out.close();

				PrintStream outBlack = new PrintStream(cardSetBlack);
				writeOutBlack.put(params[0], outBlack);
				PrintStream outWhite = new PrintStream(cardSetWhite);
				writeOutWhite.put(params[0], outWhite);
			}
		}

		HashMap<Integer, String> blackCards = new HashMap<Integer, String>();
		HashMap<Integer, String> whiteCards = new HashMap<Integer, String>();

		for(String line : lines){
			if(line.startsWith("INSERT INTO black_cards VALUES"))
			{
				String[] params = line.replace("INSERT INTO black_cards VALUES (", "").replace(");", "").split(",(?=([^\']*\'[^\']*\')*[^\']*$) ", -1);
				blackCards.put(Integer.parseInt(params[0]), params[1].substring(1, params[1].length() - 1).replace("''", "'"));
			}
			if(line.startsWith("INSERT INTO white_cards VALUES"))
			{
				String[] params = line.replace("INSERT INTO white_cards VALUES (", "").replace(");", "").split(",(?=([^\']*\'[^\']*\')*[^\']*$) ", -1);
				whiteCards.put(Integer.parseInt(params[0]), params[1].substring(1, params[1].length() - 1).replace("''", "'"));
			}
		}

		for(String line : lines){
			if(line.startsWith("INSERT INTO card_set_black_card VALUES"))
			{
				String[] params = line.replace("INSERT INTO card_set_black_card VALUES (", "").replace(");", "").split(",(?=([^\']*\'[^\']*\')*[^\']*$) ", -1);
				if(writeOutBlack.containsKey(params[0]))
					writeOutBlack.get(params[0]).println(blackCards.get(Integer.parseInt(params[1].trim())));
			}
			if(line.startsWith("INSERT INTO card_set_white_card VALUES"))
			{
				String[] params = line.replace("INSERT INTO card_set_white_card VALUES (", "").replace(");", "").split(",(?=([^\']*\'[^\']*\')*[^\']*$) ", -1);
				if(writeOutWhite.containsKey(params[0]))
					writeOutWhite.get(params[0]).println(whiteCards.get(Integer.parseInt(params[1].trim())));
			}
		}

		for(PrintStream out : writeOutBlack.values())
			out.close();
		for(PrintStream out : writeOutWhite.values())
			out.close();
	}


	public static void hackSslVerifier() {
		// FIXME: My JVM doesn't like the certificate. I should go add StartSSL's root certificate to
		// its trust store, and document steps. For now, I'm going to disable SSL certificate checking.

		// Create a trust manager that does not validate certificate chains
		final TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
					}

					@Override
					public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
					}
				}
		};

		try {
			final SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// Create host name verifier that only trusts cardcast
		final HostnameVerifier allHostsValid = new HostnameVerifier() {

			public boolean verify(String hostname, SSLSession session) {
				return"api.cardcastgame.com".equals(hostname);
			}
		};

		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}
}
