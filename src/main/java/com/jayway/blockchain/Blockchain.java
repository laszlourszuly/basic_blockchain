package com.jayway.blockchain;

import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.lang.InterruptedException;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.System;
import java.lang.Thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class represents our blockchain, exposing means of recording new
 * transactions and generating new blocks.
 */
public class Blockchain {
    private static final String DIFFICULTY = "000";

    private final List<Block> blocks;
    private final List<Transaction> transactions;
    private final ExecutorService workerService;
    private final AtomicInteger nextBlockIndex;

    private Future<Block> miningTask;

    /**
     * Prepares a new instance of a blockchain. Each instance will have its
     * genesis block generated and populated.
     */
    public Blockchain() {
        blocks = new ArrayList<>();
        transactions = Collections.synchronizedList(new ArrayList<>());
        workerService = Executors.newSingleThreadExecutor();
        nextBlockIndex = new AtomicInteger(0);
    }


    /**
     * Stores a new transaction temporarilly in an internal cache if it isn't
     * already cached.
     *
     * @param from The sender of the new transaction.
     * @param to   The receiver of the new transaction.
     * @param data The data to transact.
     * @throws IllegalArgumentException if any of the required data is null
     *                                  or empty.
     */
    public void record(final String from,
                       final String to,
                       final String data,
                       final long timestamp) throws IllegalArgumentException {

        Transaction transaction = TransactionHelper.create(from, to, data, timestamp);
        for (Transaction t : transactions)
            if (transaction.hash.equals(t.hash))
                return;

        transactions.add(transaction);
    }

    /**
     * Tries to mine the next block on the blockchain by finding the next nonce
     * that fulfills the difficulty constraints. This method will enqueue a new
     * job to the worker thread.
     * <p>
     * Since we're running on a worker thread, we'll take a snapshot of the
     * transaction cache and work with that. More transactions may be added
     * while we're mining, those won't be included in this block, however,
     * only the snapshot will.
     */
    public void mine() {
        if (miningTask != null && !miningTask.isDone())
            return;

        miningTask = workerService.submit(() -> {
            if (transactions.isEmpty() && !blocks.isEmpty())
                return null;

            // Collect block header details.
            int index = nextBlockIndex.getAndIncrement();
            Date timestamp = new Date();
            String referenceHash = blocks.isEmpty() ? null : BlockHelper.hashBlock(blocks.get(index - 1));
            List<Transaction> content = new ArrayList<>(transactions);

            // Build the block header.
            String rawHeader = BlockHelper.buildRawBlockHeader(
                    index,
                    date.getTime(),
                    referenceHash,
                    content);

            long nonce = 0;
            String hashString = "";
            System.out.printf("Started mining at %s UTC\n", date.toString());

            // Start looking for a nonce that, when added to the block
            // header, gives a hash hex string starting with "000".
            while (!hashString.startsWith(DIFFICULTY)) {
                hashString = BlockHelper.hashBlock(++nonce, rawHeader);

                // Abort mining if cancelled.
                if (Thread.currentThread().isInterrupted())
                    return null;
            }

            System.out.printf("Found new block!\n\tDuration: %d\n\tIterations: %d\n",
                    System.currentTimeMillis() - date.getTime(),
                    nonce);

            transactions.removeAll(content);
            Block newBlock = new Block(index,
                    nonce,
                    date.getTime(),
                    referenceHash,
                    content);
            blocks.add(newBlock);
            return newBlock;
        });
    }

    /**
     * Aborts any ongoing mining.
     */
    public void stopMining() {
        if (miningTask != null && !miningTask.isDone())
            miningTask.cancel(true);
    }

    /**
     * Returns an unmodifiable version of the entire blockchain as it looks
     * right now.
     */
    public List<Block> getBlocks() {
        return Collections.unmodifiableList(
                new ArrayList<Block>(blocks));
    }

    /**
     * Returns an unmodifiable part of the entire blockchain as it looks
     * right now. The "part" will be the blocks [index..size] (exclusive end).
     *
     * @param fromIndex The index of the first block to return.
     * @return A list of blocks. May be empty but never null.
     */
    public List<Block> getBlocks(final int fromIndex) {
        if (!blocks.isEmpty())
            for (int i = 0, size = blocks.size(); i < size; i++)
                if (blocks.get(i).index == fromIndex)
                    return Collections.unmodifiableList(
                            blocks.subList(i, size));

        return Collections.unmodifiableList(
                Collections.emptyList());
    }

    /**
     * Appends the given blocks if and only if they are valid and intact. Upon
     * success the transactions provided by the new blocks will be removed from
     * our pending transactions cache.
     *
     * @param candidates The blocks to append to our blockchain.
     * @return Boolean true if the operation could be performed, else false.
     */
    public boolean appendBlocks(final Block... candidates) {
        if (candidates == null || candidates.length == 0)
            return true;

        // Validate the integrity of the new block candidates and that they
        // really continue where our blockchain ends. Don't add any blocks to
        // our chain until we have validated the entire set of candidates.
        Block lastBlock = blocks.get(blocks.size() - 1);
        if (verifyIntegrity(lastBlock, candidates[0])) {
            for (int i = 0, last = candidates.length - 1; i < last; i++)
                if (!verifyIntegrity(candidates[i], candidates[i + 1]))
                    return false;

            // Everything seems legit. Clean up our pending transactions
            // accordingly.
            blocks.addAll(Arrays.asList(candidates));
            for (Block candidate : candidates)
                for (Transaction transaction : candidate.transactions)
                    transactions.remove(transaction);
        }

        // Yeay!!!
        return true;
    }

    /**
     * Returns the index of the next expected block for this blockchain.
     *
     * @return The block index.
     */
    public int getNextBlockIndex() {
        return nextBlockIndex.get();
    }

    /**
     * Returns an unmodifiable version of the pending transactions list as it
     * looks right now.
     */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(
                new ArrayList<Transaction>(transactions));
    }


    /*
     * Verifies the integrity of two neighbouring blocks with regard to their
     * index, nonce and link. The index needs to be one more than the previous
     * blocks index. The nonce needs to be such that when the block header is
     * hashed the resulting hash string starts with the {@link #DIFFICULTY}
     * pattern. Finally the previous hash link of the new block's header needs
     * to be the same as the hash of our last block's header.
     */
    private boolean verifyIntegrity(final Block reference, final Block candidate) {
        // Validate content
        if (reference == null || candidate == null)
            return false;

        // Validate index
        if (candidate.index != reference.index + 1)
            return false;

        // Validate nonce
        if (!BlockHelper.hashBlock(candidate).startsWith(DIFFICULTY))
            return false;

        // Validate link
        if (!BlockHelper.hashBlock(reference).equals(candidate.previousHashString))
            return false;

        return true;
    }

}
