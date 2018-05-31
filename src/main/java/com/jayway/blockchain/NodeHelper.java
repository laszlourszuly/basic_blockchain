package com.jayway.blockchain;

import com.google.gson.Gson;

import java.io.IOException;

import java.lang.Math;
import java.lang.String;
import java.lang.SecurityException;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *  This class is responsible for handling the peer relations for this node. It
 *  knows how to register new peers and how to register itself as a peer at
 *  another node.
 */
public class NodeHelper {


    private static final class InstanceHolder {
        static final int MAX_PEERS_COUNT = 5;
        static final String CACHE_FILE = "./root_nodes.json";
        static final NodeHelper INSTANCE = new NodeHelper();
    }


    private HashSet<String> nodes = new HashSet<>();
    private String myAddress = "";


    // Hidden ctor
    private NodeHelper() {
    }


    /**
     *  Initializes this {@code NodeHelper} instance with a persisted list of
     *  known peers. The initialization includes registering ourselves at some
     *  of our peers. The peer will return its peers upon registration allowing
     *  us to expand our list of peers.
     *
     *  @param myUrl The URL of this node on the network.
     */
    public static void init(final String myUrl) {
        // Setup my public address.
        InstanceHolder.INSTANCE.myAddress = myUrl;

        // Read any persisted peers.
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(InstanceHolder.CACHE_FILE), UTF_8);
        } catch (SecurityException | IOException e) {
            lines = Collections.emptyList();
        }

        InstanceHolder.INSTANCE.nodes.clear();
        InstanceHolder.INSTANCE.nodes.addAll(lines);
        InstanceHolder.INSTANCE.nodes.remove(myUrl);

        // Register ourselves at some peers.
        List<String> peers = getSomePeers();
        for (String peer : peers) {
            String[] peersOfPeer = registerAt(peer);
            InstanceHolder.INSTANCE.nodes.addAll(Arrays.asList(peersOfPeer));
        }
    }

    /**
     *  Adds the new node URL to the internal set of known peers if it's not
     *  a URL that can be seen as our own URL (e.g. "localhost" or "127.0.0.1"
     *  etc).
     *
     *  @param nodeUrl  The URL of the peer candidate.
     *  @return Boolean true if the node URL was accepted, or false if the URL
     *  points to us or is already added to the cache.
     */
    public static boolean register(final String nodeUrl) {
        if (isThisMe(nodeUrl))
            return false;

        if (InstanceHolder.INSTANCE.nodes.add(nodeUrl)) {
            try {
                Files.write(
                        Paths.get(InstanceHolder.CACHE_FILE),
                        InstanceHolder.INSTANCE.nodes,
                        UTF_8);
            } catch (SecurityException | IOException e) {
                // Forgive and forget...
            }
            return true;
        }
        
        return false;
    }
    
    /**
     *  Removes the provided node URL from the internal set of known peers.
     *
     *  @param nodeUrl  The URL of the peer to remove.
     *  @return Boolean true if the given URL existed in the cache, or false.
     */
    public static boolean unregister(final String nodeUrl) {
        if (InstanceHolder.INSTANCE.nodes.remove(nodeUrl)) {
            try {
                Files.write(
                        Paths.get(InstanceHolder.CACHE_FILE),
                        InstanceHolder.INSTANCE.nodes,
                        UTF_8);
            } catch (SecurityException | IOException e) {
                // Forgive and forget...
            }
            return true;
        }
        
        return false;
    }
    
    /**
     *  Triggers a registration process. We will post our public address (the
     *  one injected in the {@link #init(String)} method) to the node at the
     *  provided url.
     *
     *  @param nodeUrl  The URL of the node we should register ourself at.
     *  @return A set of peers of the node we registered at.
     */
    public static String[] registerAt(final String nodeUrl) {
        if (isThisMe(nodeUrl))
            return new String[0];

        HashMap<String, String> data = new HashMap<>();
        data.put("address", InstanceHolder.INSTANCE.myAddress);
        
        Gson gson = new Gson();
        String meJson = gson.toJson(data);
        String peersJson = NetworkHelper.post(nodeUrl, meJson);
        return gson.fromJson(peersJson, String[].class);
    }

    /**
     *  Picks a random sub set of our peers.
     *
     *  @return A list of peer URLs. The list may contain 1 to
     *  {@code MAX_PEERS_COUNT} items.
     */    
    public static List<String> getSomePeers() {
        Random random = new Random();
        int someCount = random.nextInt(InstanceHolder.MAX_PEERS_COUNT) + 1;

        if (InstanceHolder.INSTANCE.nodes.size() <= someCount)
            return new ArrayList<>(InstanceHolder.INSTANCE.nodes);

        ArrayList<String> pool = new ArrayList<>(InstanceHolder.INSTANCE.nodes);
        ArrayList<String> result = new ArrayList<>(someCount);
        
        for (int i = 0; i < someCount; i++) {
            int index = random.nextInt(pool.size());
            String peer = pool.remove(index);
            result.add(peer);
        }
        
        return result;
    }
    
    /**
     *  Picks a random sub set of our peers.
     *
     *  @param count    The maximum number of peers to pick.
     *  @return A list of peer URLs. The list may contain 1 to {@code count}
     *  items.
     */    
    public static List<String> getSomePeers(final int count) {
        int someCount = Math.max(0, count);
        ArrayList<String> pool = new ArrayList<>(InstanceHolder.INSTANCE.nodes);

        if (pool.size() <= someCount)
            return pool;

        ArrayList<String> result = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < someCount; i++) {
            int index = random.nextInt(pool.size());
            String peer = pool.remove(index);
            result.add(peer);
        }
        
        return result;
    }
    
    /**
     *  Gets all currently known peers.
     *
     *  @return A copy of all currently known peer URLS.
     */
    public static List<String> getAllPeers() {
        return new ArrayList<>(InstanceHolder.INSTANCE.nodes);
    }


    /*
     *  Tests whether the provided URL may point at this node in any way.
     */
    private static boolean isThisMe(final String nodeUrl) {
        try {
            URL url = new URL(nodeUrl);
            InetAddress inetAddress = InetAddress.getByName(url.getHost());

            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress())
                return true;

            if (NetworkInterface.getByInetAddress(inetAddress) != null)
                return true;

        } catch (MalformedURLException |
                 UnknownHostException |
                 SecurityException |
                 SocketException e) {
            // Make assumptions, just to be on the safe side.
            return true;
        }
        
        return false;
    }

}

