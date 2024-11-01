package me.pilkeysek.lCoinDBServer;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;

public class AuthMeCommand implements CommandExecutor {
    MongoClient client;
    Logger logger;
    public AuthMeCommand(MongoClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must be a player</red>"));
            return false;
        }
        if(strings.length != 1) {
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Usage: </yellow><dark_aqua>/authme <secret></dark_aqua>"));
            return false;
        }
        MongoCollection<Document> authplayers = client.getDatabase("lcoindb").getCollection("authplayers");
        MongoCollection<Document> players = client.getDatabase("lcoindb").getCollection("players");
        String playerUUID = ((Player) commandSender).getUniqueId().toString().replace("-","");
        Document doc = authplayers.find(eq("uuid", playerUUID)).first();
        if(doc != null) {
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already authenticated! To request removal of your authentication, please contact an admin.</red>"));
            return false;
        }
        try {
            InsertOneResult result = authplayers.insertOne(new Document()
                    .append("_id", new ObjectId())
                    .append("uuid", playerUUID)
                    .append("secret", strings[0])
            );
            InsertOneResult result2 = players.insertOne(new Document()
                    .append("_id", new ObjectId())
                    .append("uuid", playerUUID)
                    .append("lcoins", 100)
            );
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize("<green>You have been authenticated! You can now leave this server.</green>"));
        }
        catch (MongoException e) {
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize("An error ocurred trying to insert the document. Please contact an admin."));
            logger.warning(e.getMessage());
        }
        return true;
    }
}
