package me.pilkeysek.lCoinDBServer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class LCoinDBServer extends JavaPlugin {
    FileConfiguration config = getConfig();
    MongoClient mongoClient;
    MongoCollection<Document> authplayersDocument;

    @Override
    public void onEnable() {
        config.addDefault("mongoUri", "mongodb://skye.host:27017/?directConnection=true&serverSelectionTimeoutMS=2000");
        config.options().copyDefaults(true);
        saveConfig();
        mongoClient = MongoClients.create(Objects.requireNonNull(config.getString("mongoUri")));

        this.getCommand("authme").setExecutor(new AuthMeCommand(mongoClient, getLogger()));
    }

    @Override
    public void onDisable() {
        getLogger().info("Closing MongoDB connection");
        mongoClient.close();
    }
}
