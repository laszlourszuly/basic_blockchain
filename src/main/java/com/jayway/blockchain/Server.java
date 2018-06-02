package com.jayway.blockchain;

import java.lang.IllegalStateException;
import java.util.List;

import ratpack.handling.Context;
import ratpack.jackson.Jackson;
import ratpack.server.RatpackServer;


/**
 * This is our blockchain server. It knows how to handle the blockcain REST API.
 */
public class Server {

    /**
     * Starts the blockchain web server through which all communication with
     * the network will flow.
     *
     * @param args Any arguments passed on by the caller.
     */
    public static void main(String... args) throws Exception {
        NodeHelper.init(args[0]);
        Server impl = new Server();

        RatpackServer.start(server -> server.handlers(chain -> chain
                .path("transactions", context -> context.byMethod(method -> method
                        .get(() -> impl.debug_servePendingTransactions(context))
                        .post(() -> impl.recordTransaction(context))))
                .path("blocks", context -> context.byMethod(method -> method
                        .get(() -> impl.serveBlocks(context))
                        .post(() -> impl.validateBlock(context))
                        .put(() -> impl.debug_mineBlock(context))))
                .path("nodes", context -> context.byMethod(method -> method
                        .get(() -> impl.debug_servePeers(context))
                        .post(() -> impl.registerPeer(context))
                        .delete(() -> impl.unregisterPeer(context))))
        ));
    }


    private final Blockchain blockchain;
    private final Blockchain.OnBlockMinedListener miningListener;


    // Hidden constructor
    private Server() {
        blockchain = new Blockchain();
        miningListener = new Blockchain.OnBlockMinedListener() {
            @Override
            public void onBlockMined(final Block block) {
                // Propagate the new block.
                List<String> peers = NodeHelper.getSomePeers();
                for (String peer : peers)
                    NetworkHelper.post(peer + "/blocks", block);

                // Maybe start mining again.
                blockchain.mine(Server.this.miningListener);
            }
        };

        synchronizeBlockchain();
    }

    /**
     * Serves a set of blocks to the request described by the given context.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void serveBlocks(final Context ratpackContext) {
        String indexString = ratpackContext
                .getRequest()
                .getQueryParams()
                .get("index");

        // Get all blocks on top of the one with the requested
        // index, or all blocks if no index is provided
        List<Block> blocks;
        if (indexString == null) {
            blocks = blockchain.getBlocks();
        } else {
            int index = Integer.valueOf(indexString);
            blocks = blockchain.getBlocks(index);
        }

        // Serve the blocks to the requester
        ratpackContext.render(Jackson.json(blocks));
    }

    /**
     * Validates the provided block against our version of the blockchain. If
     * the block seems valid, it will be appended to our blockchain. If not,
     * we will request any potentially missing blocks from one of our peers. If
     * the new block still isn't valid it will be discarded, otherwise appended
     * to our now complete blockchain. When a block is added to the blockchain,
     * any corresponding transactions will be removed from our transactions
     * cache. Any ongoing mining process will be canceled.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void validateBlock(final Context ratpackContext) {
        // Stop mining
        blockchain.stopMining();

        ratpackContext
                .parse(Jackson.jsonNode())
                .then(jsonNode -> {
                    // Release the HTTP request.
                    ratpackContext.getResponse().status(200).send();

                    // If we already have this block in our blockchain we
                    // don't want to propagate it (to avoid resonance).
                    String json = jsonNode.toString();
                    Block block = BlockHelper.parseBlock(json);
                    if (!blockchain.getBlocks(block.index).isEmpty())
                        return;

                    // We don't have this block; propagate it in the network.
                    List<String> peers = NodeHelper.getSomePeers();
                    for (String peer : peers)
                        NetworkHelper.post(peer + "/blocks", block);

                    // Try to append the new block to our blockchain. A failure
                    // may be an indication on missing blocks. Fallback to a
                    // sync and then retry again.
                    if (!blockchain.appendBlocks(block))
                        if (synchronizeBlockchain())
                            blockchain.appendBlocks(block);
                });
    }

    /**
     * Synchronizes our version of the blockchain with a random peer of ours.
     *
     * @return Boolean true if the synchronziation could be performed, else
     * boolean false.
     */
    private boolean synchronizeBlockchain() {
        List<String> peers = NodeHelper.getSomePeers(1);
        if (peers == null || peers.isEmpty())
            return true;

        Block[] missingBlocks = BlockHelper.parseBlocks(
                NetworkHelper.get(peers.get(0) + "/blocks"));

        return blockchain.appendBlocks(missingBlocks);
    }

    /**
     * Saves the given transaction to our pending transactions cache if not
     * already cached.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void recordTransaction(final Context ratpackContext) {
        ratpackContext
                .parse(Jackson.jsonNode())
                .then(jsonNode -> {
                    // Release the HTTP request.
                    ratpackContext.getResponse().status(200).send();

                    // Cache next transaction. Don't propagate things we already
                    // have in our cache (to avoid resonance).
                    if (!blockchain.record(
                            jsonNode.get("sender").asText(),
                            jsonNode.get("receiver").asText(),
                            jsonNode.get("data").asText(),
                            jsonNode.get("timestamp").asLong()))
                        return;

                    // We have cached the transaction, propagate further.
                    String json = jsonNode.toString();
                    List<String> peers = NodeHelper.getSomePeers();
                    for (String peer : peers)
                        NetworkHelper.post(peer + "/transactions", json);

                    // Start mining if we aren't already and if there are
                    // any transactions to mine.
                    blockchain.mine(miningListener);
                });
    }

    /**
     * Registers a peer and the peer's peers in our internal list of peers and
     * requests the peer to return the favor by registering this node in its
     * own list of peers.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void registerPeer(final Context ratpackContext) {
        ratpackContext
                .parse(Jackson.jsonNode())
                .then(jsonNode -> {
                    // Get the address of the node that want's to register.
                    String peer = jsonNode.get("address").asText();

                    // Respond with some of our peers.
                    List<String> someOfMyPeers = NodeHelper.getSomePeers();
                    ratpackContext.render(Jackson.json(someOfMyPeers));

                    // Register the peer and some of its peers. Ask the peer to
                    // return the favor by registering us in its peers list.
                    if (NodeHelper.register(peer)) {
                        String[] peersOfPeer = NodeHelper.registerAt(peer);
                        for (String p : peersOfPeer)
                            NodeHelper.register(p);
                    }
                });
    }

    /**
     * Removes a peer from our interlan list of peers.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void unregisterPeer(final Context ratpackContext) {
        ratpackContext
                .parse(Jackson.jsonNode())
                .then(jsonNode -> {
                    ratpackContext.getResponse().status(200).send();
                    String peer = jsonNode.get("address").asText();
                    NodeHelper.unregister(peer);
                });
    }


    /**
     * Serves our pending transactions to the requesting party. This method
     * should only be used in debugging purpouses.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void debug_servePendingTransactions(final Context ratpackContext) {
        List<Transaction> transactions = blockchain.getTransactions();
        ratpackContext.render(Jackson.json(transactions));
    }

    /**
     * Serves ALL our peers to the requesting party. This method should only be
     * used in debugging purposes.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void debug_servePeers(final Context ratpackContext) {
        List<String> peers = NodeHelper.getAllPeers();
        ratpackContext.render(Jackson.json(peers));
    }

    /**
     * Manually starts a mining process if not already mining.
     *
     * @param ratpackContext The context providing the request metrics.
     */
    private void debug_mineBlock(final Context ratpackContext) {
        blockchain.mine(miningListener);
        ratpackContext.getResponse().status(200).send();
    }

}