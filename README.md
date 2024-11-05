# LegitiCoinDB
This is the source code for the LegitiCoinDB (Minecraft Client Mod, API, Minecraft Paper Server Plugin)

## Where can I get the mod?
**Make sure to read all of this section, else you might run into problems**

Go to the releases and select the latest release, download the `LCoinDB.jar` from there
[Get the latest release here](https://github.com/PilkeySEK/LegitiCoinDB/releases/latest)
The first thing you will need to do after installing the mod and starting minecraft is to join `lcauth.skye.host`, the mod will automatically authenticate you with a randomly generated key. If it doesnt work for some reason on the first join (It should tell you that it successfully authenticated you in the chat), rejoin, then it should work. After you're authenticated, you can turn automatic authentication off with the command `/lcoin disableauth` (you can also do this via the config).

## Directories
`LCoinDB` -> Minecraft Client Mod for Fabric

`LcoinDB-Server` -> Minecraft Paper Server Plugin for Authenticating

`LegitiCoinDB-server` -> node.js API for the Minecraft Client Mod

`LCoinDB-Bot` -> Discord bot

(I know the directories are badly named)

## Setting it up on your local machine
To set this up on your local machine (likely for development purposes), follow this step-by-step guide:
1. Clone the repository in a directory of your choosing
2. Install mongodb for your OS
3. Set up a paper server in a directory of your choosing
4. Download the Paper server .jar from the releases or compile it yourself, then put the .jar into the `plugins` folder of the paper server, then restart the server
5. Go into `plugins/LcoinDB-Server/` and open the `config.yml` file with a text editor. Change the `mongoUri` to the connection string of the mongodb that was installed in step 2
6. Restart the server once again (ikr)
7. Go to `<cloned repo>/LegitiCoinDB-server` and create a new file `.env` with the following contents. Replace `<mongodb connection string>` with your mongodb connection string:
master
```
PORT=3000
MONGO_URI=<mongodb connection string>
DB=lcoindb
```
8. Install node.js and npm
9. Run `npm i` in the `LegitiCoinDB-server` directory, this will download all the required node modules
10. Run the node.js app with `node .`
11. Download the fabric mod and put it in the mod folder of your fabric 1.21.1 instance, then start it
12. Adjust the config values of the mod in the `<instance>/config/lcoindb-config.json5` file (change the api url to localhost:3000 (or whatever port you configured the node.js app))
13. Relaunch minecraft and join your local Paper server (you can also put the Paper server behind a proxy like Velocity, make sure to enable online mode on your proxy though). It should be running on `localhost`.
14. If everything went well the Paper plugin should now authenticate you! (This might require a rejoin for some reason)
15. You can now use the mod like normal, except it will now make the api calls to your local node.js app, so meaning it will also write the changes to your local mongodb database. You can now start developing things for the mod, the Paper plugin or the node.js app :)

### Optional: setup discord bot
1. Create a [discord bot](https://discord.com/developers/applications)
2. Go to `<cloned repo>/LCoinDB-Bot and create a new file called .env with your bot token, application client id and mongodb connection string:
```
DISCORD_TOKEN=<discord token>
MONGO_URI=<mongodb connection string>
CLIENTID=<client id>
DB=lcoindb
```
3. Run node deploy-commands.js to update the bot's slash commands
4. [add the bot to your server](https://discordjs.guide/preparations/adding-your-bot-to-servers.html) and run node . to run the bot

## TODO List
- Rename directories/projects to be more descriptive
- Make some way to get the "top 10" players
- Clean up code for the `/lcoin` command (split into multiple files maybe?)
- Make error messages on the client better (not just sending the raw json response into the chat if somehthing goes wrong)
- Rate limit the API
- Make translations for the Mod instead of hardcoding the texts
- Fix the bug where you need to rejoin one time to actually authenticate
- Use the minecraft authentication instead of a custom one (hard)
