const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const { MongoClient } = require("mongodb");
const minecraftPlayer = require("minecraft-player");

const MONGO_URI = process.env.MONGO_URI;
const DB = process.env.DB;
const mongoclient = new MongoClient(MONGO_URI);

mongoclient.connect();

const players = mongoclient.db(DB).collection("players");

module.exports = {
	data: new SlashCommandBuilder()
		.setName('profile')
		.setDescription('Get stats about a specific player')
		.addStringOption(option =>
			option.setName('username')
				.setDescription('The username of the player you want to get the profile of')
				.setRequired(true)
		),
	async execute(interaction) {
		const fusername = interaction.options.getString('username');
		const { uuid } = await minecraftPlayer(fusername);
		const uuidnd = uuid.replace(/-/g, "")
		const player = await players.findOne({ uuid: uuidnd });
		const balance = player.lcoins;
		const { username } = await minecraftPlayer(uuid)
		const profileEmbed = new EmbedBuilder()
			.setColor(0xefbf04)
			.setTitle(username)
			.setDescription('Balance: `' + `${balance}` + '`')
			.setThumbnail('https://mc-heads.net/avatar/' + `${uuid}`)
		await interaction.reply({ embeds: [profileEmbed] });
	},
}