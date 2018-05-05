package com.jayway.blockchain;

import java.lang.String;
import java.lang.StringBuilder;
import java.lang.System;

/**
 *  This class represents a single transaction in the blockchain.
 */
public class Transaction {
    public final String id;
    public final String sender;
    public final String receiver;
    public final String data;
    public final long timestamp;
    
    public Transaction(final String sender,
                       final String receiver,
                       final String data) {

        this.sender = sender;
        this.receiver = receiver;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        
        byte[] idHash = HashUtils.hash(new StringBuilder()
                .append(sender)
                .append(receiver)
                .append(data)
                .append(timestamp)
                .toString());

        this.id = HashUtils.convertToHexString(idHash);
    }

}
