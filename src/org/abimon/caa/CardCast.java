package org.abimon.caa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class CardCast {
	
	public static void findAndDownloadDeck(String code) {
		try{
			URL url = new URL("https://api.cardcastgame.com/v1/decks/" + code);
			HttpsURLConnection http = (HttpsURLConnection) url.openConnection(Game.proxy);
			InputStream in = http.getInputStream();
			byte[] data = new byte[in.available()];
			in.read(data);

			String s = new String(data);
			JsonObject json = new JsonParser().parse(s).getAsJsonObject();
			String name = json.get("name").getAsString();
			name = name.replace("/", "-");
			System.out.println("Deck located:");
			System.out.println("Name: " + name);
			System.out.println("Desc: " + json.get("description"));
			System.out.println("Download and add deck?");
			System.out.print("> ");
			String response = Game.in.nextLine().toLowerCase();
			if(response.equals("yes"))
			{
				URL cards = new URL("https://api.cardcastgame.com/v1/decks/" + code + "/cards");

				String cardsString = getUrlContent(cards);
				JsonObject cardsObject = new JsonParser().parse(cardsString).getAsJsonObject();
				
				JsonArray blackCardsArray = cardsObject.get("calls").getAsJsonArray();
				JsonArray whiteCardsArray = cardsObject.get("responses").getAsJsonArray();
				
				File deckOut = new File(Game.decks.getAbsolutePath() + File.separator + code + ".json");
				File deckBlackCards = new File(Game.decks.getAbsolutePath() + File.separator + "black_cards" + File.separator + name + ".txt");
				File deckWhiteCards = new File(Game.decks.getAbsolutePath() + File.separator + "white_cards" + File.separator + name + ".txt");
				
				PrintStream deckOutput = new PrintStream(deckOut);
				JsonObject deckJson = new JsonObject();
				
				deckJson.add("name", new JsonPrimitive(name));
				deckJson.add("desc", new JsonPrimitive(json.get("description").getAsString()));
				deckJson.add("black_cards", new JsonPrimitive(deckBlackCards.getAbsolutePath()));
				deckJson.add("white_cards", new JsonPrimitive(deckWhiteCards.getAbsolutePath()));
				deckOutput.println(Game.gson.toJson(deckJson));
				deckOutput.close();
				
				PrintStream blackCardsOut = new PrintStream(deckBlackCards);
				for(JsonElement blackCard : blackCardsArray)
				{
					JsonArray text = blackCard.getAsJsonObject().get("text").getAsJsonArray();
		            final ArrayList<String> strs = new ArrayList<String>(text.size());
		            String finalText = "";
		            for (JsonElement elem : text) {
		              strs.add(elem.getAsString());
		              finalText += elem.getAsString() + "____";
		            }
		            finalText = finalText.substring(0, finalText.length() - 4);
		            final int pick = strs.size() - 1;
		            final int draw = (pick >= 3 ? pick - 1 : 0);
		            
		            Game.blackCards.add(new BlackCard(finalText, json.get("name").getAsString(), code, pick, draw));
		            blackCardsOut.println(finalText);
				}
				blackCardsOut.close();
				
				PrintStream whiteCardsOut = new PrintStream(deckWhiteCards);
				for(JsonElement whiteCard : whiteCardsArray)
				{
					JsonArray text = whiteCard.getAsJsonObject().get("text").getAsJsonArray();
		            final ArrayList<String> strs = new ArrayList<String>(text.size());
		            String finalText = "";
		            for (JsonElement elem : text) {
		              strs.add(elem.getAsString());
		              finalText += elem.getAsString() + "____";
		            }
		            finalText = finalText.substring(0, finalText.length() - 4);
		            
		            Game.whiteCards.add(new WhiteCard(finalText, json.get("name").getAsString(), code));
		            whiteCardsOut.println(finalText);
				}
				whiteCardsOut.close();
				
				JsonObject decksJson = new JsonParser().parse(new FileReader(Game.deckUsage)).getAsJsonObject();
				if(decksJson.has(code))
					decksJson.add(code, new JsonPrimitive(decksJson.get(code).getAsInt() + 1));
				else
					decksJson.add(code, new JsonPrimitive(1));
				PrintStream decksOut = new PrintStream(Game.deckUsage);
				decksOut.println(Game.gson.toJson(deckJson));
				decksOut.close();
			}
		}
		catch(Throwable th){
			th.printStackTrace();
		}
	}
	
	  private static String getUrlContent(final URL url) throws IOException {
		    final HttpURLConnection conn = (HttpURLConnection) url.openConnection(Game.proxy);
		    conn.setDoInput(true);
		    conn.setDoOutput(false);
		    conn.setRequestMethod("GET");
		    conn.setInstanceFollowRedirects(true);
		    conn.setReadTimeout(3000);
		    conn.setConnectTimeout(3000);

		    final int code = conn.getResponseCode();
		    if (HttpURLConnection.HTTP_OK != code) {
		      System.out.println(String.format("Got HTTP response code %d from Cardcast for %s", code, url));
		      return null;
		    }
		    final String contentType = conn.getContentType();
		    if (!"application/json".equals(contentType)) {
		      System.out.println(String.format("Got content-type %s from Cardcast for %s", contentType, url));
		      return null;
		    }

		    final InputStream is = conn.getInputStream();
		    final InputStreamReader isr = new InputStreamReader(is);
		    final BufferedReader reader = new BufferedReader(isr);
		    final StringBuilder builder = new StringBuilder(4096);
		    String line;
		    while ((line = reader.readLine()) != null) {
		      builder.append(line);
		      builder.append('\n');
		    }
		    reader.close();
		    isr.close();
		    is.close();

		    return builder.toString();
		  }

}
