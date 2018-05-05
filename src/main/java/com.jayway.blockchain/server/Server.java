package com.jayway.blockchain.server;

import com.jayway.blockchain.blockchain.Block;
import com.jayway.blockchain.blockchain.Chain;

import java.lang.Exception;
import java.lang.String;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ratpack.form.Form;
import ratpack.handling.Context;
import ratpack.http.Status;
import ratpack.http.client.HttpClient;
import ratpack.jackson.Jackson;
import ratpack.server.RatpackServer;


public class Server {

    public static void main(String... args) throws Exception {
        final Server instance = new Server();
        RatpackServer.start(server -> server
            .handlers(chain -> chain
                .get("block", instance::serveMinedBlock)
                .get("blockchain", instance::serveBlockchain)
                .get("synchronize", instance::synchronize)
                .path("transactions", context -> context
                    .byMethod(method -> method
                        .get(() -> instance.servePendingTransactions(context))
                        .post(() -> instance.recordTransaction(context))))
                .path("nodes", context -> context
                    .byMethod(method -> method
                        .get(() -> instance.serveNodes(context))
                        .post(() -> instance.registerNode(context))
                        .delete(() -> instance.unregisterNode(context))
                ))
            ));
    }


    private final Chain blockchain;
    private final Set<String> nodes;

    private Server() {
        blockchain = new Chain();
        nodes = new HashSet<>();
    }

    private void serveMinedBlock(final Context context) {
        try {
            // Only mine for a maximum of 40 seconds. More than that is a
            // waste of time as it's fair to assume that the general state
            // of the blockchain has already changed considerably by then.
            // In a real world scenario a significant amount of maths and
            // science would be thrown on this timeout.
            serveSuccess(context, blockchain.mine().get(40, TimeUnit.SECONDS));
        } catch (Exception e) {
            serveError(context, e);                 
        }
    }

    private void serveBlockchain(final Context context) {
        serveSuccess(context, blockchain.getBlocks());
    }

    private void synchronize(final Context context) {
        for (String url : nodes)
            try {
                context.get(HttpClient.class)
                    .get(new URI(url + "/blockchain"))
                    .then(response -> context
                        .parse(Jackson.fromJson(Block[].class))
                        .then(result -> blockchain
                            .synchronize(Arrays.asList(result))));
            } catch (Exception e) {
                // Don't serve an error to the caller here,
                // just ignore this peer and try with the next.
            }

        serveSuccess(context);
    }

    private void servePendingTransactions(final Context context) {
        serveSuccess(context, blockchain.getTransactions());
    }

    private void recordTransaction(final Context context) {
        context
            .parse(Form.class)
            .then(formData -> {
                String sender = formData.get("sender");
                String receiver = formData.get("receiver");
                String data = formData.get("data");
                blockchain.record(sender, receiver, data);
                serveSuccess(context);
            });
    }
    
    private void serveNodes(final Context context) {
        serveSuccess(context, nodes);
    }

    private void registerNode(final Context context) {
        context
            .parse(Jackson.jsonNode())
            .map(node -> node.get("address").asText())
            .then(peer -> {
                nodes.add(peer);
                serveSuccess(context, nodes);
            });
    }
    
    private void unregisterNode(final Context context) {
        context
            .parse(Jackson.jsonNode())
            .map(node -> node.get("address").asText())
            .then(peer -> {
                nodes.remove(peer);
                serveSuccess(context, nodes);
            });
    }

    private void serveSuccess(final Context context) {
        context
            .getResponse()
            .status(200)
            .send();
    }
    
    private void serveSuccess(final Context context, final Object content) {
        context.render(Jackson.json(content));
    }
    
    private void serveError(final Context context, final Throwable cause) {
        context
            .getResponse()
            .status(404)
            .send();
    }

}

