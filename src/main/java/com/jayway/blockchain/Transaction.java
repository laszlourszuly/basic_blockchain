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

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;

        if (!(other instanceof Transaction))
            return false;

        Transaction otherTransaction = (Transaction) other;
        return (hash == null) ?
                otherTransaction.hash == null :
                hash.equals(otherTransaction.hash);
    }

    @Override
    public int hashCode() {
        return hash != null ?
                hash.hashCode() :
                0;
    }

}

