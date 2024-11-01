package me.pilkeysek.lcoindb.client.requestbodies;

public class TransactionBody {
    public TransactionBody(String sender, String receiver, int amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
    }
    String sender;
    String receiver;
    int amount;
}
