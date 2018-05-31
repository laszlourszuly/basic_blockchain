package com.jayway.blockchain;

import java.lang.IllegalArgumentException;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.System;

public class TransactionHelper {

    public static Transaction create(final String sender,
                                     final String receiver,
                                     final String data,
                                     final long timestamp) {

        validateSender(sender);
        validateReceiver(receiver);
        validateData(data);
        
        long t = timestamp < 0 ?
                System.currentTimeMillis() :
                timestamp;
                
        String hash = HashHelper.hash(new StringBuilder()
                .append(sender)
                .append(receiver)
                .append(data)
                .append(t)
                .toString());

        return new Transaction(hash, sender, receiver, data, t);
    }

    private static void validateSender(final String sender) {
        if (sender == null || sender.isEmpty())
            throw new IllegalArgumentException("Invalid sender");
    }
    
    private static void validateReceiver(final String receiver) {
        if (receiver == null || receiver.isEmpty())
            throw new IllegalArgumentException("Invalid receiver");

    }
    
    private static void validateData(final String data) {
        if (data == null || data.isEmpty())
            throw new IllegalArgumentException("Invalid data");

    }
    
}

