require("dotenv").config();
const express = require("express");
const https = require('https');
const fs = require('fs');
const { MongoClient, MongoInvalidArgumentError } = require("mongodb");

const key = fs.readFileSync(__dirname + '/certs/key.key');
const cert = fs.readFileSync(__dirname + '/certs/cert.crt');
const options = {
  key: key,
  cert: cert
};

const MONGO_URI = process.env.MONGO_URI;
const PORT = process.env.PORT;
const DB = process.env.DB;
const mongoclient = new MongoClient(MONGO_URI);
const app = express();
app.use(express.json());

mongoclient.connect();

const players = mongoclient.db(DB).collection("players");
const transactions = mongoclient.db(DB).collection("transactions");
const authplayers = mongoclient.db(DB).collection("authplayers");

app.get("/", async (req, res) => {
  res.json({ status: "ok" });
});

app.use((req, res, next) => {
  mongoclient.connect(); // This seems to actually be the proper way of ensuring a connection
  next();
});

app.get("/player/:uuid", async (req, res) => {
  const uuid = req.params.uuid;
  const player = await players.findOne({ uuid: uuid });
  if (player) {
    res.json(player);
  } else {
    res.json({});
  }
});

// Put this AFTER all listeners that dont require credentials (mostly GET)
app.use((req, res, next) => {
  console.log(req.rawHeaders);
  console.log(req.body);
  if (!req.headers.authorization) {
    return res.status(403).json({ message: "No authorization sent" });
  }
  next();
});

app.post("/transaction", async (req, res) => {
  const body = req.body;
  if (!(body.sender && body.receiver && body.amount)) {
    res
      .status(400)
      .json({ message: "No sender, reciever or amount was provided" });
    return;
  }
  const sender = await players.findOne({ uuid: body.sender });
  if (!sender) {
    res.status(400).json({ message: "Sender is not registered in database" });
    return;
  }
  const receiver = await players.findOne({ uuid: body.receiver });
  if (!receiver) {
    res.status(400).json({ message: "Receiver is not registered in database" });
    return;
  }
  const authorization = req.headers.authorization;
  const dbAuthorization = await authplayers.findOne({ uuid: body.sender });
  if (!dbAuthorization) {
    res.status(400).json({ message: "No authorized player with that uuid" });
    return;
  }
  if (authorization !== dbAuthorization.secret) {
    res.status(403).json({ message: "The autorization failed" });
    return;
  }
  if (body.amount <= 0) {
    res.status(400).json({ message: "Amount must be 1 or higher" });
    return;
  }
  if (body.sender === body.receiver) {
    res.status(400).json({ message: "Cannot send money to yourself" });
    return;
  }

  const amount = body.amount;
  let senderlcoins = sender.lcoins;
  senderlcoins -= amount;
  let receiverlcoins = receiver.lcoins;
  receiverlcoins += amount;
  if (senderlcoins < 0) {
    res.status(400).json({
      message: "Bad Transaction: The sender does not have enough lcoins",
    });
    return;
  }
  if (isNaN(senderlcoins) || isNaN(receiverlcoins)) {
    res
      .status(400)
      .json({ message: "Bad Transaction: Resulting amounts are NaN" });
    return;
  }
  await players.updateOne(
    { uuid: body.sender },
    { $set: { lcoins: senderlcoins } }
  );
  await players.updateOne(
    { uuid: body.receiver },
    { $set: { lcoins: receiverlcoins } }
  );
  res.json({ message: "Transaction Success" });
});

async function dropRandomLCoins() {
  const randomDocument = await players.aggregate([{ $sample: { size: 1 } }]).toArray();
  await players.updateOne(
    { uuid: randomDocument.at(0).uuid },
    { $set: { lcoins: randomDocument.at(0).lcoins + 1 } }
  );
}

setInterval(dropRandomLCoins, 1000 * 60 * 3); // Every 3 minutes

const server = https.createServer(options, app);


// TODO: HTTPS instead of HTTP
server.listen(PORT, "0.0.0.0", () => {
  console.log("Port ", PORT);
});
