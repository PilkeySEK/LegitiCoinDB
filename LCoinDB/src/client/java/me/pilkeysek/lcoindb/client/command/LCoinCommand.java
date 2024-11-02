package me.pilkeysek.lcoindb.client.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.pilkeysek.lcoindb.client.LcoindbClient;
import me.pilkeysek.lcoindb.client.MojangApiUtil;
import me.pilkeysek.lcoindb.client.requestbodies.TransactionBody;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;

// BIG TODO: Split this huge aah command with confusing stuff up probably into multiple classes
// TODO: Also make the argument names better
// Maybe also make a custom argument provider
public class LCoinCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("lcoin")
                .then(ClientCommandManager.argument("main-arg", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("ping");
                            builder.suggest("transaction");
                            builder.suggest("balance");
                            if(LcoindbClient.config.authenticationEnabled()) builder.suggest("disableauth");
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            switch(StringArgumentType.getString(context, "main-arg")) {
                                case "ping":
                                    ping(context);
                                    return 1;
                                case "transaction":
                                    context.getSource().sendFeedback(Text.literal("Expected 2 more arguments").formatted(Formatting.RED));
                                    context.getSource().sendFeedback(Text.literal("/lcoin transaction <recevier's username> <amount>").formatted(Formatting.GOLD));
                                    return 0;
                                case "balance":
                                    getCoins(context.getSource().getPlayer().getNameForScoreboard(), context);
                                    return 1;
                                case "disableauth":
                                    LcoindbClient.config.authenticationEnabled(false);
                                    context.getSource().sendFeedback(Text.literal("Disabled automatic authentication").formatted(Formatting.GREEN));
                                    return 1;
                                default:
                                    context.getSource().sendFeedback(Text.literal("Unknown argument").formatted(Formatting.RED));
                                    return 0;
                            }}).then(ClientCommandManager.argument("arg-2", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if(StringArgumentType.getString(context, "main-arg").equals("transaction") || StringArgumentType.getString(context, "main-arg").equals("balance")) {
                                        List<AbstractClientPlayerEntity> players = context.getSource().getWorld().getPlayers();
                                        for(AbstractClientPlayerEntity player : players) {
                                            builder.suggest(player.getNameForScoreboard());
                                        }
                                        return builder.buildFuture();
                                    }
                                    return builder.buildFuture();
                                }).executes(context -> {
                                    String mainArg = StringArgumentType.getString(context, "main-arg");
                                    switch(mainArg) {
                                        case "transaction":
                                            context.getSource().sendFeedback(Text.literal("Expected 1 more argument").formatted(Formatting.RED));
                                            context.getSource().sendFeedback(Text.literal("/lcoin transaction <recevier's username> <amount>").formatted(Formatting.GOLD));
                                            return 0;
                                        case "balance":
                                            getCoins(StringArgumentType.getString(context, "arg-2"), context);
                                            return 1;
                                        default:
                                            context.getSource().sendFeedback(Text.literal("Unknown argument").formatted(Formatting.RED));
                                            return 0;
                                    }
                                }).then(ClientCommandManager.argument("arg-3", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            String mainArg = StringArgumentType.getString(context, "main-arg");
                                            String arg2 = StringArgumentType.getString(context, "arg-2");
                                            switch(mainArg) {
                                                case "transaction":
                                                    int resCode = makeTransaction(context.getSource().getPlayer().getUuid(), StringArgumentType.getString(context, "arg-2"), IntegerArgumentType.getInteger(context, "arg-3"), context);
                                                    if(resCode == 200) {
                                                        context.getSource().sendFeedback(Text.literal("The operation succeeded!").formatted(Formatting.GREEN));
                                                        return 1;
                                                    }
                                                    context.getSource().sendFeedback(Text.literal("The operation failed with code " + resCode).formatted(Formatting.RED));
                                                    return 0;
                                                default:
                                                    context.getSource().sendFeedback(Text.literal("Unknown argument").formatted(Formatting.RED));
                                                    return 0;
                                            }
                                        }))
                        )

                ));
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
            if(status == 200 && res.get("status").getAsString().equals("ok")) {
                context.getSource().sendFeedback(Text.literal("All systems are likely operational :D").formatted(Formatting.DARK_GREEN));
            }
        } catch (IOException e) {
            context.getSource().sendFeedback(Text.literal(e.getMessage()).formatted(Formatting.RED));
        }
    }

    private static int makeTransaction(UUID sender, String receiverName, int amount, CommandContext<FabricClientCommandSource> context) {
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
            return response.getStatusLine().getStatusCode();
        } catch (Exception e) {
            context.getSource().sendFeedback(Text.literal(e.getMessage()).formatted(Formatting.RED));
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return 500;
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
}
