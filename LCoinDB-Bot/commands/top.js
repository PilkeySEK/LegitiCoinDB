const { SlashCommandBuilder, EmbedBuilder } = require("discord.js");
const { MongoClient } = require("mongodb");
const minecraftPlayer = require("minecraft-player");

const MONGO_URI = process.env.MONGO_URI;
const DB = process.env.DB;
const mongoclient = new MongoClient(MONGO_URI);

mongoclient.connect();

const players = mongoclient.db(DB).collection("players");

module.exports = {
  data: new SlashCommandBuilder()
    .setName("top")
    .setDescription("Get stats about a specific player"),
  async execute(interaction) {
    try {
      const top10 = await players.find().sort({ lcoins: -1 }).toArray();
      var str = "";
      for (var i = 0; i < top10.length; i++) {
        const { username } = await minecraftPlayer(top10.at(i).uuid);
        var lcoins = top10.at(i).lcoins;
        str += i + ". " + username + ": " + lcoins + "\n";
      }
      const topEmbed = new EmbedBuilder()
        .setColor(0xefbf04)
        .setTitle("Top 10")
        .setDescription(`${str}`);
      await interaction.reply({ embeds: [topEmbed] });
    } catch (error) {
      await interaction.reply(`${error}`);
    }
  },
};
