# LegitiCoinDB
This is the source code for the LegitiCoinDB (Minecraft Client Mod, API, Minecraft Paper Server Plugin)

## Where can I get the mod?
Go to the releases and select the latest release, download the `LCoinDB.jar` from there
[Get the latest release here](https://github.com/PilkeySEK/LegitiCoinDB/releases/latest)

### One thing to do before you can use it
You should join the server `lcauth.skye.host` to authenticate (else you won't be able to use the mod). You might need to rejoin one time if it gives an error on the first join.

## Directories
`LCoinDB` -> Minecraft Client Mod for Fabric

`LcoinDB-Server` -> Minecraft Paper Server Plugin for Authenticating

`LegitiCoinDB-server` -> node.js API for the Minecraft Client Mod

(I know the directories are badly named)

## TODO List
- Rename directories/projects to be more descriptive
- Make some way to get the "top 10" players
- Clean up code for the `/lcoin` command (split into multiple files maybe?)
- Make error messages on the client better (not just sending the raw json response into the chat if somehthing goes wrong)
- Rate limit the API
- Make translations for the Mod instead of hardcoding the texts
- Add information on how to build and run the project yourself
- Fix the bug where you need to rejoin one time to actually authenticate