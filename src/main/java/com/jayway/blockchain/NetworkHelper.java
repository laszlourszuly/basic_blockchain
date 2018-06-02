package com.jayway.blockchain;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.String;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class NetworkHelper {

    private static final class InstanceHolder {
        static final MediaType CONTENT_TYPE_JSON = MediaType.parse("application/json");
        static final OkHttpClient CLIENT = new OkHttpClient();
    }

    /**
     * Sends the provided JSON as a synchronous POST request to the given URL.
     * No validation is made on the JSON. JSON content type headers are
     * automagically attached.
     *
     * @param url     The URL to send the POST request to.
     * @param payload The POST request payload.
     * @return The response body as a string.
     */
    public static String post(final String url, final Object payload) {
        try {
            return InstanceHolder.CLIENT
                    .newCall(new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(
                                    InstanceHolder.CONTENT_TYPE_JSON,
                                    payload instanceof String ?
                                            (String) payload :
                                            new Gson().toJson(payload)))
                            .build())
                    .execute()
                    .body()
                    .string();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Sends the provided JSON as a synchronous PUT request to the given URL.
     * No validation is made on the JSON. JSON content type headers are
     * automagically attached.
     *
     * @param url     The URL to send the PUT request to.
     * @param payload The PUT request payload.
     * @return The response body as a string.
     */
    public static String put(final String url, final Object payload) {
        try {
            return InstanceHolder.CLIENT
                    .newCall(new Request.Builder()
                            .url(url)
                            .put(RequestBody.create(
                                    InstanceHolder.CONTENT_TYPE_JSON,
                                    payload instanceof String ?
                                            (String) payload :
                                            new Gson().toJson(payload)))
                            .build())
                    .execute()
                    .body()
                    .string();
        } catch (IOException e) {
            return "";
        }
    }

    public static String get(final String url) {
        try {
            return InstanceHolder.CLIENT
                    .newCall(new Request.Builder()
                            .url(url)
                            .get()
                            .build())
                    .execute()
                    .body()
                    .string();
        } catch (IOException e) {
            return "";
        }
    }
}

