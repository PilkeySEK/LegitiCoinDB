package me.pilkeysek.lcoindb.client.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.pilkeysek.lcoindb.client.LcoindbClient;
import me.pilkeysek.lcoindb.client.MojangApiUtil;
import me.pilkeysek.lcoindb.client.command.argumenttype.PlayerArgumentType;
import me.pilkeysek.lcoindb.client.requestbodies.TransactionBody;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

public class LCoinCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("lcoin")
                .then(ClientCommandManager.literal("ping")
                        .executes(context -> {
                            new Thread(() -> {
                                ping(context);
                            }).start();
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(ClientCommandManager.literal("balance")
                        .executes(context -> {
                            String playername = context.getSource().getPlayer().getNameForScoreboard();
                            new Thread(() -> {
                                getCoins(playername, context);
                            }).start();
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                .executes(context -> {
                                    String playername = StringArgumentType.getString(context, "player");
                                    new Thread(() -> {
                                        getCoins(playername, context);
                                    }).start();
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(ClientCommandManager.literal("pay")
                        .then(ClientCommandManager.argument("receiver", PlayerArgumentType.player())
                                .then(ClientCommandManager.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            UUID senderUUID = context.getSource().getPlayer().getUuid();
                                            String receiverName = StringArgumentType.getString(context, "receiver");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            new Thread(() -> {
                                                makeTransaction(senderUUID, receiverName, amount, context);
                                            }).start();
                                            return Command.SINGLE_SUCCESS;
                                        })))
                )
        );
    }

    private static void ping(CommandContext<FabricClientCommandSource> context) {
        try {
            URL apiUrl = URI.create(LcoindbClient.config.apiUrl()).toURL();
            HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
            con.setRequestMethod("GET");

            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JsonObject res = JsonParser.parseString(content.toString()).getAsJsonObject();
            context.getSource().sendFeedback(Text.literal("API responded with status \"" + res.get("status").getAsString() + "\" and code " + status).formatted(Formatting.AQUA));
            if (status == 200 && res.get("status").getAsString().equals("ok")) {
                context.getSource().sendFeedback(Text.literal("All systems are likely operational :D").formatted(Formatting.DARK_GREEN));
            }
        } catch (IOException e) {
            context.getSource().sendFeedback(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
    }
    private static void getCoins(String playername, CommandContext<FabricClientCommandSource> context) {
        String uuidString = MojangApiUtil.usernameToUUIDString(playername);
        try {
            URL apiUrl = URI.create(LcoindbClient.config.apiUrl() + "/player/" + uuidString).toURL();
            HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            int status = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            JsonObject res = JsonParser.parseString(content.toString()).getAsJsonObject();
            if(status == 200) {
                context.getSource().sendFeedback(
                        Text.literal("The player ").formatted(Formatting.GREEN).append(
                                Text.literal(playername).formatted(Formatting.AQUA).append(
                                        Text.literal(" has ").formatted(Formatting.GREEN).append(
                                                Text.literal(String.valueOf(res.get("lcoins").getAsInt())).formatted(Formatting.DARK_AQUA).append(
                                                        Text.literal(" LegitiCoins").formatted(Formatting.GREEN))))));
                return;
            }
            context.getSource().sendFeedback(Text.literal("An error ocurred! API status code: " + status + "\nResponse Body: " + res.getAsString()));
        } catch (Exception e) {
            context.getSource().sendFeedback(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
    }
    private static void makeTransaction(UUID sender, String receiverName, int amount, CommandContext<FabricClientCommandSource> context) {
        String receiverUUIDString = MojangApiUtil.usernameToUUIDString(receiverName);
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPost request = new HttpPost(LcoindbClient.config.apiUrl() + "/transaction");
            Gson gson = new Gson();
            StringEntity params = new StringEntity(gson.toJson(new TransactionBody(sender.toString().replace("-",""),receiverUUIDString, amount)));
            request.addHeader("Authorization", LcoindbClient.config.secret());
            request.setHeader("Content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            if(response.getStatusLine().getStatusCode() != 200) context.getSource().sendFeedback(Text.literal(EntityUtils.toString(response.getEntity())).formatted(Formatting.GOLD));
            else context.getSource().sendFeedback(Text.literal("Success!").formatted(Formatting.GREEN));
        } catch (Exception e) {
            context.getSource().sendFeedback(Text.literal(e.getMessage()).formatted(Formatting.RED));
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
