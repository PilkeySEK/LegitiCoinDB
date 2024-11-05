const { SlashCommandBuilder } = require('discord.js');
const { MongoClient } = require("mongodb");
const minecraftPlayer = require("minecraft-player");

const MONGO_URI = process.env.MONGO_URI;
const DB = process.env.DB;
const mongoclient = new MongoClient(MONGO_URI);

mongoclient.connect();

const players = mongoclient.db(DB).collection("players");

module.exports = {
	data: new SlashCommandBuilder()
		.setName('balance')
		.setDescription('Replies with your balance')
		.addStringOption(option =>
			option.setName('username')
				.setDescription('The username of the player')
				.setRequired(true)
		),
	async execute(interaction) {
		const username = interaction.options.getString('username');
		const { uuid } = await minecraftPlayer(username);
		const uuidnd = uuid.replace(/-/g, "")
		const player = await players.findOne({ uuid: uuidnd });
		const balance = player.lcoins;
		// TODO: Make pretty with embeds
		await interaction.reply(username + "'s balance is: " + balance);
	},
}