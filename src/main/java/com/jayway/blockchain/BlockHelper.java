package com.jayway.blockchain;

import com.google.gson.Gson;

import java.lang.String;
import java.lang.StringBuilder;

import java.util.List;


/**
 * This class offers convenince methods for operations on {@link Block}
 * objects.
 */
public class BlockHelper {

    /**
     * Parses the provided JSON string into a single Block object.
     *
     * @param json The JSON string to parse.
     * @return The corresponding Block object.
     */
    public static Block parseBlock(final String json) {
        return new Gson().fromJson(json, Block.class);
    }

    /**
     * Parses the provided JSON string into an array of Block objects.
     *
     * @param json The JSON string to parse.
     * @return The corresponding Block objects.
     */
    public static Block[] parseBlocks(final String json) {
        return new Gson().fromJson(json, Block[].class);
    }

    /**
     * Builds an unobscured raw header string for the provided block. The
     * produced header isn't in itself enough to uniquely identify the block.
     * For that, a suitable nonce needs to be added before hashing it.
     *
     * @param block The block create the header for.
     * @return The block header string that can be used as input in the
     * hashing proces.
     */
    public static String buildRawBlockHeader(final Block block) {
        return buildRawBlockHeader(block.index,
                block.timestamp,
                block.previousHashString,
                block.transactions);
    }

    /**
     * Builds an unobscured raw header string from the provided data. The
     * produced header isn't in itself enough to uniquely identify the data.
     * For that, a suitable nonce needs to be added before hashing it.
     *
     * @param index         The index of the block this data belongs to.
     * @param timestamp     The start time when the block was mined.
     * @param referenceHash The hash of the previous block in the chain.
     * @param transactions  The list of transactions in the block.
     * @return The block header string that can be used as input in the
     * hashing proces.
     */
    public static String buildRawBlockHeader(final int index,
                                             final long timestamp,
                                             final String referenceHash,
                                             final List<Transaction> transactions) {

        StringBuilder stringBuilder = new StringBuilder()
                .append(index)
                .append(timestamp)
                .append(referenceHash);

        for (Transaction transaction : transactions)
            stringBuilder.append(transaction.hash);

        return stringBuilder.toString();
    }

    /**
     * Generates a unique block hash based on the data found in the provided
     * block.
     * <p>
     * This method will build a block header from scratch every time invoked,
     * hence, it's not optimal to use in the mining process.
     *
     * @param block The block to hash.
     * @return A unique hash based on the provided block data.
     */
    public static String hashBlock(final Block block) {
        String header = buildRawBlockHeader(block);
        return hashBlock(block.nonce, header);
    }

    /**
     * Generates a unique block hash based on a nonce and a raw block header.
     *
     * @param nonce  The nonce to decorate the header with.
     * @param header The raw unobscured header of the block to hash.
     * @return A unique hash based on the provided block data.
     */
    public static String hashBlock(final long nonce, final String header) {
        return HashHelper.hash(nonce + header);
    }

}
