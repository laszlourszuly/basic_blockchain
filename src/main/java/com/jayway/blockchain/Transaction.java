package com.jayway.blockchain;

import java.lang.String;

/**
 *  This class represents a single transaction in the blockchain.
 */
public class Transaction {
    public final String hash;
    public final String sender;
    public final String receiver;
    public final String data;
    public final long timestamp;


    public Transaction(final String hash,
                       final String sender,
                       final String receiver,
                       final String data,
                       final long timestamp) {

        this.hash = hash;
        this.sender = sender;
        this.receiver = receiver;
        this.data = data;
        this.timestamp = timestamp;
    }

}

