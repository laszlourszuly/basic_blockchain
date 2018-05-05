package com.jayway.blockchain;

import java.lang.String;
import java.util.List;
import java.util.ArrayList;


/**
 *  This class represents a block in the block chain.
 */
public class Block {
    public final int index;
    public final long nonce;
    public final long timestamp;
    public final String previousHashString;
    public final List<Transaction> transactions;

    
    public Block(final int index,
                 final long nonce,
                 final long timestamp,
                 final String previousHashString,
                 final List<Transaction> transactions) {
                 
        this.index = index;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.previousHashString = previousHashString;
        this.transactions = new ArrayList<>();

        if (transactions != null && !transactions.isEmpty())
            this.transactions.addAll(transactions);
    }

}































