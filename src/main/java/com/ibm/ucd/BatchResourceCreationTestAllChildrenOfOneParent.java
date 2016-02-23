package com.ibm.ucd;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class BatchResourceCreationTestAllChildrenOfOneParent {
    static private String serverUrl = "https://perf2.uclab.ibm.com:8443";
    static private String username = "admin";
    static private String password = "admin";

    static public void main(String[] args) throws Exception {
        List<String> results = new ArrayList<String>();
        String res = "";
        for (int i = 1; i < 10; i ++) {
            res = new BatchResourceCreationTestAllChildrenOfOneParent(100 * i).batchCreateResourcesOneParentManyChildren();
            results.add(res);
            System.out.println(res);
        }
        for (int i = 1; i < 6; i ++) {
            res = new BatchResourceCreationTestAllChildrenOfOneParent(1000 * i).batchCreateResourcesOneParentManyChildren();
            results.add(res);
            System.out.println(res);
        }
        System.out.println("Resources, Time(milliseconds)");
        for (String result : results) {
            System.out.println(result);
        }
    }

    public BatchResourceCreationTestAllChildrenOfOneParent(int resourceCount) {
        this.createdResources = 0;
        this.resourceCount = resourceCount;
        this.rootResourceName = String.format("%6d-resource-root-%d", resourceCount, System.currentTimeMillis());
        this.rootResourcePath = String.format("/%s", rootResourceName);
    }

    public String batchCreateResourcesOneParentManyChildren() {
        long duration = -1;
        try {
            BasicHttpClientFactory factory = new BasicHttpClientFactory(username, password);
            HttpClient client = factory.getClient();
            DeployClient deployClient = new DeployClient(client, new URI(serverUrl));
            String rootResourceId = deployClient.createRootResoruce(rootResourceName);
            deployClient.getComponentResourceRolesFromServer();
            JSONArray array = new JSONArray();

            JSONObject root = new JSONObject();
            root.put("name", rootResourceName);
            root.put("path", rootResourcePath);
            root.put("inheritTeam", "true");
            root.put("description", "");
            array.put(root);
            for (int i = 0; i < resourceCount; i++) {
                array.put(createResource(String.format("resource_%6d", i), root));
            }

            long start = System.currentTimeMillis();
            deployClient.batchCreateResoruces(array.toString(), rootResourceId);
            duration = System.currentTimeMillis() - start;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new Date().toString() + ", " + createdResources + ", " + duration;
    }

    public JSONObject createResource(String name, JSONObject parent) throws JSONException {
        createdResources++;
        JSONObject resource = new JSONObject();
        resource.put("name", name);
        resource.put("path", parent.get("path") + "/" + name);
        resource.put("inheritTeam", "true");
        resource.put("description", "");
        return resource;
    }

    private String rootResourceName;
    private String rootResourcePath;
    private int createdResources;
    private int resourceCount;
}