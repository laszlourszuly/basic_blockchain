package com.jayway.blockchain.blockchain;

import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.lang.InterruptedException;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.System;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicInteger;


/**
 *  This class represents our blockchain, exposing means of recording new
 *  transactions and generating new blocks.
 */
public class Chain {
    private static final String DIFFICULTY = "00";    

    private final List<Block> blocks;
    private final List<Transaction> transactions;    
    private final ExecutorService workerService;
    private final AtomicInteger nextBlockIndex;


    /**
     *  Prepares a new instance of a blockchain. Each instance will have its
     *  genesis block generated and populated.
     */
    public Chain() {
        blocks = new ArrayList<>();
        transactions = new ArrayList<>();
        workerService = Executors.newSingleThreadExecutor();
        
        nextBlockIndex = new AtomicInteger(0);
        mine();
    }


    /**
     *  Records a new transaction and stores it temporarilly in an internal
     *  cache. This method will enqueue a new job to the worker thread.
     *
     *  @param from     The sender of the new transaction.
     *  @param to       The receiver of the new transaction.
     *  @param data     The data to transact.
     *
     *  @return A future object tied to the enqueued record job. The future will
     *  deliver a hint on which block the transaction may be included in. The
     *  caller can also use the future to cancel this particular job.
     */
    public Future<Integer> record(final String from,
                                  final String to,
                                  final String data) {

        if (from == null || from.isEmpty())
            throw new IllegalArgumentException("Invalid sender: " + from);

        if (to == null || to.isEmpty())
            throw new IllegalArgumentException("Invalid receiver: " + to);
            
        if (data == null || data.isEmpty())
            throw new IllegalArgumentException("Invalid data: " + data);

        return workerService
                .submit(() -> {
                    transactions.add(new Transaction(from, to, data));
                    return nextBlockIndex.get();
                });
    }

    /**
     *  Tries to mine the next block on the blockchain by finding the next nonce
     *  that fulfills the difficulty constraints. This method will enqueue a new
     *  job to the worker thread.
     *
     *  @return A future object tied to the enqueued mining job. The future will
     *  deliver the newly mined block once it's done. The caller can also use
     *  the future to cancel this particular job.
     *
     *  @throws IllegalStateException if there are no transactions to build a
     *  new block from.
     */
    public Future<Block> mine() {
        return workerService
                .submit(() -> {
                    // Take a snapshot of the transactions to include in the
                    // next block. Since we're running on a worker thread, more
                    // transactions may be added while we're mining, but those
                    // won't be included in this block, only the snapshot will.
                    List<Transaction> content = new ArrayList<>(transactions);
                    boolean isGenesisBlock = blocks.isEmpty();

                    if (!isGenesisBlock && content.isEmpty())
                        throw new IllegalStateException("No transactions");

                    // Collect the data that's needed to create a new block.
                    // This data will be input to our mining algorithm.
                    int index = nextBlockIndex.get();
                    long nonce = 0;
                    long timestamp = System.currentTimeMillis();
                    String hashString = "";
                    String previousHashString = isGenesisBlock ?
                            null :
                            generateHeader(blocks.get(index - 1));

                    // Start looking for a nonce that, when added to the new
                    // block data, gives a hash hex string starting with "00".
                    while(!hashString.startsWith(DIFFICULTY))
                        hashString = generateHeader(index,
                                                    ++nonce,
                                                    timestamp,
                                                    previousHashString,
                                                    content);

                    // We have found a nonce that produces the expected
                    // hash. Create and add the new block to the blockchain.
                    blocks.add(new Block(index,
                                         nonce,
                                         timestamp,
                                         previousHashString,
                                         content));

                    // Our new block was successfully mined, update our
                    // internal state accordingly.
                    nextBlockIndex.incrementAndGet();
                    transactions.removeAll(content);

                    return blocks.get(index);
                });
    }

    /**
     *  Synchronizes the blockchain with the network hint. The synchronization
     *  consists of validating length and integrity of the provided blockchain
     *  and choosing the longest, intact chain as a new base line. This method
     *  will enqueue a new job to the worker thread.
     *
     *  @param candidateBlockchain  The possibly dominant blockchain version.
     *
     *  @return A future object tied to the enqueued synchronization job. The
     *  future will deliver boolean true if the provided candidate has been
     *  accepted as the new base line or false otherwise. The caller can also
     *  use the future to cancel this particular job.
     */
    public Future<Boolean> synchronize(final List<Block> candidateBlockchain) {
        return workerService
                .submit(() -> {
                    if (candidateBlockchain == null)
                        return false;
                        
                    // The candidate blockchain is shorter than ours and
                    // "looses" by that. If it's equally long, we'll still
                    // favor our own while we have invested time and effort
                    // into it.
                    if (candidateBlockchain.size() <= blocks.size())
                        return false;
                    
                    // The candidate blockchain seems to be longer. Accept it
                    // as our new base line if its internal integrity is intact.
                    else if (verifyIntegrity(candidateBlockchain)) {
                        blocks.clear();
                        blocks.addAll(candidateBlockchain);
                        Block lastBlock = blocks.get(blocks.size() - 1);
                        nextBlockIndex.set(lastBlock.index + 1);
                        return true;
                    }
                    
                    // The candidate blockchain is indeed longer, but it's
                    // integrity is broken. Keep our current blockchain.
                    else return false;
                });
    }
    
    /**
     *  Returns an unmodifiable version of the entire blockchain as it looks
     *  right now.
     */
    public List<Block> getBlocks() {
        return Collections.unmodifiableList(
                new ArrayList<Block>(blocks));
    }
    
    /**
     *  Returns an unmodifiable version of the pending transactions list as it
     *  looks right now.
     */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(
                new ArrayList<Transaction>(transactions));
    }
    
    
    /*
     *  Generates a header for the provided block.
     */
    private String generateHeader(final Block block) {
        return generateHeader(block.index,
                              block.nonce,
                              block.timestamp,
                              block.previousHashString,
                              block.transactions);
    }
    
    /*
     *  Generates a block header from the provided data. This must be
     *  implimented strictly by the protocol as all implementations must
     *  generate the exact same output from the same input.
     */
    private String generateHeader(final long index,
                                  final long nonce,
                                  final long timestamp,
                                  final String referenceHashString,
                                  final List<Transaction> transactions) {

        StringBuilder stringBuilder = new StringBuilder()
                .append(index)
                .append(nonce)
                .append(timestamp)
                .append(referenceHashString);
        
        for(Transaction transaction : transactions)
            stringBuilder.append(transaction.id);
        
        byte[] hash = HashUtils.hash(stringBuilder.toString());
        return HashUtils.convertToHexString(hash);
    }
    
    /*
     *  Verifies a blockchains internal integrity with regard to a nonce
     *  producing a hash with the correct difficulty and that the link between
     *  two blocks are intact.
     */
    private boolean verifyIntegrity(final List<Block> blockchain) {
        for (int i = 1, count = blockchain.size(); i < count; i++) {
            Block currentBlock = blockchain.get(i - 1);
            Block nextBlock = blockchain.get(i);
            String hashString = generateHeader(currentBlock);
            
            // Validate nonce
            if (!hashString.startsWith(DIFFICULTY))
                return false;
            
            // Validate link
            if (!hashString.equals(nextBlock.previousHashString))
                return false;
        }
        
        return true;
    }

}
