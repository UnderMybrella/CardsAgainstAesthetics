package org.abimon.caa;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class Webserver 
{

	PrintStream log;

	static BlackCard currentCard = Game.currentBlackCard;

	static volatile String userPickedCzar = "";
	static HashMap<Integer, String> posToUser = new HashMap<Integer, String>();
	static String[] cards = new String[Game.users.size()];

	static boolean waitingOnCzar = false;

	public Webserver(){
		ServerSocket server = null;
		try{
			if(!new File("Logs" + File.separatorChar).exists())
				new File("Logs" + File.separatorChar).mkdir();
			File logFile = new File("Logs" + File.separatorChar + new Date() + ".txt");
			logFile.createNewFile();
			log = new PrintStream(logFile);

			log.println("Initialising Webserver...");

			server = new ServerSocket(80);

			HashMap<String, byte[]> cache = new HashMap<String, byte[]>();

			while(true)
			{
				try{
					Socket client = server.accept();
					log.println("Client: " + client);
					while(client.getInputStream().available() == 0)
						Thread.sleep(100);
					byte[] data = new byte[client.getInputStream().available()];
					client.getInputStream().read(data);
					String request = new String(data);
					for(String s : request.split("\n"))
						if(s.contains("GET /")){
							String url = s.replace("GET /", "").replace("HTTP/1.1", "").trim();
							String[] baseParams = url.split("\\?");
							String[] params = baseParams[baseParams.length - 1].split("\\&");
							for(int i = 0; i < params.length; i++){
								params[i] = params[i].replace("+", " ");
								String param = "";
								String encoded = "";
								for(char c : params[i].toCharArray())
									if(c == '%')
										encoded += c;
									else if(!encoded.equals("") && encoded.length() < 3)
										encoded += c;
									else if(encoded.length() == 3)
									{
										int hex = (Integer.parseInt(encoded.substring(1), 16));
										char hexCode = (char) hex;
										param += hexCode + "" + c;
										encoded = "";
									}
									else
										param += c;
								params[i] = param;
							}
							File file = new File(baseParams[0]);

							if(file.getAbsolutePath().equals(new File("").getAbsolutePath()))
							{
								log.print(client + " is requesting the central directory. Redirecting to login");
								file = new File("login-redirect.html");
							}
							if(file.getName().equals("join.html") && baseParams.length > 1 && params[0].startsWith("username"))
							{
								User user = Game.users.getByAddress(client.getInetAddress().getHostAddress());
								user.username = params[0].split("=", 2)[1];
								if(Game.gameStarted)
								{
									while(user.cards.size() < Game.CARDS_IN_HAND)
										user.cards.add(Game.whiteDeck.poll());
								}

								if(!waitingOnCzar)
									cards = new String[Game.users.size()];

								file = new File("lobby-redirect.html");
							}

							if((file.getName().equals("lobby.html") || file.getName().equals("lobby-redirect.html")) && Game.gameStarted && Game.users.contains(client.getInetAddress().getHostAddress()))
								file = new File("game-redirect.html");

							if(file.getName().equals("game.html") || file.getName().equals("game-redirect.html"))
								if(Game.users.getByAddress(client.getInetAddress().getHostAddress()).isCzar && Game.gameStarted)
									file = new File("czar-redirect.html");

							log.println(client + " is requesting " + file.getAbsolutePath());

							if(file.getName().equals("game-lobby.html")){
								if(Game.users.getByAddress(client.getInetAddress().getHostAddress()).cardSelection == null)
									file = new File("game-redirect.html");
								else{
									String lobbyString = "<html><head><meta http-equiv=\"refresh\" content=\"1\"/></head><body>";
									if(!waitingOnCzar)
										lobbyString += "<h2>Waiting for everyone to pick their cards</h2>";
									else{
										if(cards[0] == null || cards[0].equals(""))
											for(User user : Game.users)
												if(user.username.equals(""));
												else{
													int pos = new Random().nextInt(cards.length);
													int counter = 0;
													while(cards[pos] != null && !cards[pos].equals("") && counter < 100)
													{
														pos = new Random().nextInt(cards.length);
														counter++;
													}
													if(cards[pos] == null || cards[pos].equals(""))
													{
														cards[pos] = Webserver.currentCard.getNicelyFormatted(user.cardSelection);
														Webserver.posToUser.put(pos, user.username);
													}
												}
										for(String str : cards)
											lobbyString += "<p>" + str + "</p>";

										if(!lobbyString.contains("<br>"))
											lobbyString += "<br>";
									}
									lobbyString += "<form><input type=\"submit\" value=\"Refresh\"></form>";
									lobbyString += "</body></html>";
									cache.put(file.getName(), lobbyString.getBytes());
								}
							}

							if(file.getName().equals("czar.html")){
								if(params.length == 1 && params[0].equalsIgnoreCase("czar.html")){
									String lobbyString = "<html>" + (!waitingOnCzar ? "<head><meta http-equiv=\"refresh\" content=\"1\"/></head>" : "") + "<body>";
									if(!Webserver.userPickedCzar.equals("") || !User.winner.equals(""))
										lobbyString += "<h3>" + (Webserver.userPickedCzar.equals("") ? User.winner : Webserver.userPickedCzar) + " won that round</h3>";
									if(!waitingOnCzar)
										lobbyString += "<h2>Waiting for everyone to pick their cards</h2>";
									else{
										if(cards[0] == null || cards[0].equals(""))
											for(User user : Game.users)
												if(user.username.equals(""));
												else{
													int pos = new Random().nextInt(cards.length);
													int counter = 0;
													while(cards[pos] != null && !cards[pos].equals("") && counter < 100)
													{
														pos = new Random().nextInt(cards.length);
														counter++;
													}
													if(cards[pos] == null || cards[pos].equals(""))
													{
														cards[pos] = (pos + 1) + ") " + Webserver.currentCard.getNicelyFormatted(user.cardSelection);
														Webserver.posToUser.put(pos, user.username);
													}
												}
										for(String str : cards)
											lobbyString += "<p>" + str + "</p>";

										if(!lobbyString.contains("<br>"))
											lobbyString += "<br>";
									}
									lobbyString += "<form><input type=\"text\" name=\"cardSet\"><input type=\"submit\" value=\"Select\"></form>";
									lobbyString += "</body></html>";
									cache.put(file.getName(), lobbyString.getBytes());
								}
								else{
									try{
										int set = Integer.parseInt(params[0].split("=")[1])-1;
										User.winner = Webserver.posToUser.get(set);
										if(User.winner == null){
											User.winner = "";
											throw new Exception();
										}
										userPickedCzar = User.winner;
										file = new File("game-redirect.html");
									}
									catch(Throwable th){
											String lobbyString = "<html>" + (!waitingOnCzar ? "<head><meta http-equiv=\"refresh\" content=\"1\"/></head>" : "") + "<body>";
											if(!Webserver.userPickedCzar.equals("") || !User.winner.equals(""))
												lobbyString += "<h3>" + (Webserver.userPickedCzar.equals("") ? User.winner : Webserver.userPickedCzar) + " won that round</h3>";
											lobbyString += "<h3>Invalid Card Selection (" + params[0].split("=")[1] + ")" + "</h3>";
											if(!waitingOnCzar)
												lobbyString += "<h2>Waiting for everyone to pick their cards</h2>";
											else{
												if(cards[0] == null || cards[0].equals(""))
													for(User user : Game.users)
														if(user.username.equals(""));
														else{
															int pos = new Random().nextInt(cards.length);
															int counter = 0;
															while(cards[pos] != null && !cards[pos].equals("") && counter < 100)
															{
																pos = new Random().nextInt(cards.length);
																counter++;
															}
															if(cards[pos] == null || cards[pos].equals(""))
															{
																cards[pos] = (pos + 1) + ") " + Webserver.currentCard.getNicelyFormatted(user.cardSelection);
																Webserver.posToUser.put(pos, user.username);
															}
														}
												for(String str : cards)
													lobbyString += "<p>" + str + "</p>";

												if(!lobbyString.contains("<br>"))
													lobbyString += "<br>";
											}
											lobbyString += "<form><input type=\"text\" name=\"cardSet\"><input type=\"submit\" value=\"Select\"></form>";
											lobbyString += "</body></html>";
											cache.put(file.getName(), lobbyString.getBytes());
									}
								}
							}

							if(file.getName().equals("lobby.html"))
							{
								String lobbyString = "<html><head><meta http-equiv=\"refresh\" content=\"2\"/></head><body>";
								for(User user : Game.users)
									if(user.username.equals(""));
									else
										lobbyString += "<p>" + user.username + "</p>";
								if(!lobbyString.contains("<br>"))
									lobbyString += "<br>";
								lobbyString += "<form><input type=\"submit\" value=\"Refresh\"></form>";
								lobbyString += "</body></html>";
								cache.put(file.getName(), lobbyString.getBytes());
							}

							if(file.getName().equals("game.html")){
								if(baseParams.length == 1)
								{
									String gameString = "<html><body>";
									int counter = 1;
									//								while(Game.currentBlackCard == null || (prevCard != null && prevCard.hashCode() == Game.currentBlackCard.hashCode()))
									//								{
									//									Thread.sleep(100);
									//									System.out.println("Waiting...");
									//								}
									gameString += "<h1>" + currentCard != null ? currentCard.getText() : "For whatever reason the black card is null" + "</h1>";


									System.out.println(Webserver.userPickedCzar);

									for(WhiteCard card : Game.users.getByAddress(client.getInetAddress().getHostAddress()).cards){
										if(card != null)
											gameString += "<p>" + counter++ + ") " + card.getName() + "</p>";
									}

									gameString += "<form>";
									for(int i = 0; i < currentCard.getPick(); i++)
										gameString += "<input type=\"text\" name=\"card_" + i + "\">";
									gameString += "<input type=\"submit\" value=\"Submit Card(s)\"></form>";
									if(!Webserver.userPickedCzar.equals("") || !User.winner.equals(""))
										gameString += "<h3>" + (Webserver.userPickedCzar.equals("") ? User.winner : Webserver.userPickedCzar) + " won that round</h3>";
									gameString += "</body></html>";
									client.getOutputStream().write(gameString.getBytes());
									client.getOutputStream().flush();
									client.close();
									continue;
								}
								else if(params.length < currentCard.pick)
								{
									System.out.println("Params: " + (params.length));
									System.out.println("Needed: " + currentCard.pick);
									for(String str : params)
										System.out.println(str);
								}
								else{
									User user = Game.users.getByAddress(client.getInetAddress().getHostAddress());

									int[] taken = new int[user.cards.size()];
									String[] text = new String[params.length];
									LinkedList<String> problems = new LinkedList<String>();

									for(int i = 0; i < params.length; i++)
									{
										String token = params[i].split("\\=", 2)[1];
										if(token.matches("\\d+"))
										{
											int loc = Integer.parseInt(token)-1;
											if(loc < 0)
												problems.add("Card Choice " + (i) + " must be greater than 1!");
											else if(loc > taken.length)
												problems.add("Card Choice " + (i) + " cannot be greater than " + taken.length + "!");
											else if(taken[loc] == 192168)
												problems.add("You've already used that card!");
											else
											{
												taken[loc] = 192168;
												text[i] = user.cards.get(loc).getName();
											}
										}
										else if(token.matches("\\d+\\)"))
										{
											int loc = Integer.parseInt(token.substring(0, token.length() - 1))-1;
											if(loc < 0)
												problems.add("Card Choice " + (i) + " must be greater than 1!");
											else if(loc > taken.length)
												problems.add("Card Choice " + (i) + " cannot be greater than " + taken.length + "!");
											else if(taken[loc] == 192168)
												problems.add("You've already used that card!");
											else
											{
												taken[loc] = 192168;
												text[i] = user.cards.get(loc).getName();
											}
										}
										else if(token.matches("\\d+\\).*")) //Blank card
										{
											int loc = Integer.parseInt(token.split("\\)", 2)[0])-1;
											if(loc < 0)
												problems.add("Card Choice " + (i) + " must be greater than 1!");
											else if(loc > taken.length)
												problems.add("Card Choice " + (i) + " cannot be greater than " + taken.length + "!");
											else if(taken[loc] == 192168)
												problems.add("You've already used that card!");
											else if(user.cards.get(loc).getName().equals("____"))
											{
												taken[loc] = 192168;
												text[i] = token.split("\\)", 2)[1];
											}
											else
											{
												taken[loc] = 192168;
												text[i] = user.cards.get(loc).getName();
											}
										}
										else{ //Here comes the error checking. Change to lower case, check if it matches one of the cards. If so, use it. If it doesn't, check if we have a blank. If so, use the blank. If not, error.
											for(int loc = 0; loc < user.cards.size(); loc++)
												if(user.cards.get(loc).getName().equalsIgnoreCase(token))
												{
													taken[loc] = 192168;
													text[i] = user.cards.get(loc).getName();
													break;
												}
											if(text[i] == null || text[i].equals("")){
												for(int loc = 0; loc < user.cards.size(); loc++)
													if(user.cards.get(loc).getName().equalsIgnoreCase("____"))
													{
														taken[loc] = 192168;
														text[i] = token;
														break;
													}
												if(text[i] == null || text[i].equals(""))
													problems.add("Invalid card specifier");
											}
										}
									}

									if(problems.isEmpty()){
										Game.users.getByAddress(client.getInetAddress().getHostAddress()).cardSelection = text;
										for(int i = 0; i < taken.length; i++)
											if(taken[i] == 192168)
												Game.users.getByAddress(client.getInetAddress().getHostAddress()).cards.set(i, null);
										file = new File("game-lobby-redirect.html");
									}
									else
										System.out.println("Many a problemo\n" + problems);
								}
							}

							if(!file.exists() && !cache.containsKey(file.getName()))
								file = new File("404.html");

							if(file.isDirectory())
							{
								client.getOutputStream().write(Arrays.toString(file.listFiles()).getBytes());
								client.getOutputStream().flush();
								client.getOutputStream().close();
							}
							else{
								if(!cache.containsKey(file.getName()))
								{
									FileInputStream in = new FileInputStream(file);
									byte[] fData = new byte[in.available()];
									in.read(fData);
									in.close();
									cache.put(file.getName(), fData);
								}
								client.getOutputStream().write(cache.get(file.getName()));
								client.getOutputStream().flush();
								client.close();
							}
						}
				}
				catch(Throwable th){
					try{Thread.sleep(1000);}catch(Throwable the){}
					th.printStackTrace(log);}
			}
		}
		catch(Throwable th){
			try{Thread.sleep(1000);}catch(Throwable the){}
			th.printStackTrace(log);
		}

		try{
			if(server != null && !server.isClosed())
				server.close();
		}
		catch(Throwable th){}
	}
}
