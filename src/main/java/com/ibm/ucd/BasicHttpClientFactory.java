package com.ibm.ucd;

import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;

import com.urbancode.commons.httpcomponentsutil.HttpClientBuilder;

public class BasicHttpClientFactory {

    final private HttpClientBuilder clientBuilder;

    public BasicHttpClientFactory(String username, String password) {
        Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

        clientBuilder = new HttpClientBuilder();
        clientBuilder.setUsername(username);
        clientBuilder.setPassword(password);
        clientBuilder.setTrustAllCerts(true);
    }

    public HttpClient getClient() {
        return clientBuilder.buildClient();
    }
}