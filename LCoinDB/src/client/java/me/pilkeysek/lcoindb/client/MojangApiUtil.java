package me.pilkeysek.lcoindb.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class MojangApiUtil {
    public static String usernameToUUIDString(String username) {
        try {
            URL apiUrl = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username).toURL();

            HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
            con.setRequestMethod("GET");

            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JsonObject res = JsonParser.parseString(content.toString()).getAsJsonObject();
            return res.get("id").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO Use Mod's logger instead of this
        }
    }
}
