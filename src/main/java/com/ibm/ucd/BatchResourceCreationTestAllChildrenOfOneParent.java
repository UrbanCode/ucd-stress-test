package com.ibm.ucd;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class BatchResourceCreationTestAllChildrenOfOneParent {

    private String serverUrl;
    private String username;
    private String password;
    private String rootResourceName;
    private String rootResourcePath;
    private int createdResources;
    private int resourceCount;

    static private abstract class Result {
        final private long duration;
        final private int cnt;
        
        Result(long duration, int cnt) {
            this.duration = duration;
            this.cnt = cnt;
        }
        
        long getDuration() {
            return duration;
        }
        
        int getCount() {
            return cnt;
        }
        
        abstract String getMessage();
    }
    
    static private class CreateResult extends Result {
        final private String resourcePath;
        
        CreateResult(String path, long duration, int cnt) {
            super(duration, cnt);
            this.resourcePath = path;
        }
        
        String getPath() {
            return this.resourcePath;
        }
        
        @Override
        String getMessage() {
            return new Date().toString() + ", Creation, " + getCount() + ", " + getDuration();
        }
    }
    
    static private class DeleteResult extends Result {
        DeleteResult(long duration, int cnt) {
            super(duration, cnt);
        }

        @Override
        String getMessage() {
            return new Date().toString() + ", Deletion, " + getCount() + ", " + getDuration();
        }
    }
    
    static private abstract class MoveResult extends Result {
        final private String origPath;
        final private String newPath;
        
        MoveResult(String origPath, String newPath, long duration, int cnt) {
            super(duration, cnt);
            this.origPath = origPath;
            this.newPath = newPath;
        }
        
        String getOrigPath() {
            return this.origPath;
        }
        
        String getNewPath() {
            return this.newPath;
        }
        
        abstract String getType();
        
        String getMessage() {
            return new Date().toString() + ", " + getType() + ", " + getCount() + ", " + getDuration();
        }
    }
    
    static private class RenameResult extends MoveResult {
        RenameResult(String origPath, String newPath, long duration, int cnt) {
            super(origPath, newPath, duration, cnt);
        }

        @Override
        String getType() {
            return "Rename";
        }
    }
    
    static private class CopyResult extends MoveResult {
        CopyResult(String origPath, String newPath, long duration, int cnt) {
            super(origPath, newPath, duration, cnt);
        }

        @Override
        String getType() {
            return "Copy";
        }
        
    }

    static public void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("arguments: <serverUrl>");
            System.exit(1);
        }
        List<Result> results = new ArrayList<Result>();
        for (int i = 1; i < 10; i ++) {
            results.addAll(runTest(args[0], "admin", "admin", 100*i));
        }
        for (int i = 1; i < 6; i ++) {
            results.addAll(runTest(args[0], "admin", "admin", 1000*i));
        }
        results.addAll(runTest(args[0], "admin", "admin", 5703));
        System.out.println("Resources, Time(milliseconds)");
        for (Result result : results) {
            System.out.println(result.getMessage());
        }
    }
    
    static private List<Result> runTest(String url, String user, String pass, int resCount) throws Exception {
        List<Result> result = new ArrayList<Result>();
        List<CreateResult> creResults = new BatchResourceCreationTestAllChildrenOfOneParent(url, user, pass, resCount).batchCreateResourcesOneParentManyChildren();
        result.addAll(creResults);
        for (CreateResult cRes : creResults) {
            System.out.println(cRes.getMessage());
        }
        CreateResult res = creResults.get(0);
        String origPath = res.getPath();
        String mvParentPath = origPath + "-mv";
        String mvPath = mvParentPath + origPath;
        String cpParentPath = res.getPath() + "-cp";
        String cpPath = cpParentPath + origPath;
        createRootResource(url, user, pass, mvParentPath.substring(1));
        createRootResource(url, user, pass, cpParentPath.substring(1));
        RenameResult mvRes = renameResource(url, user, pass, origPath, mvParentPath, res.getCount());
        result.add(mvRes);
        System.out.println(mvRes.getMessage());
        CopyResult cpRes = copyResource(url, user, pass, mvPath, cpParentPath, res.getCount());
        result.add(cpRes);
        System.out.println(cpRes.getMessage());
        DeleteResult rmRes = deleteResource(url, user, pass, cpParentPath, res.getCount() + 1);
        result.add(rmRes);
        System.out.println(rmRes.getMessage());
        rmRes = deleteResource(url, user, pass, mvParentPath, res.getCount() + 1);
        result.add(rmRes);
        System.out.println(rmRes.getMessage());
        return result;
    }
    
    static private DeployClient createDeployClient(String url, String user, String pass) throws Exception {
        BasicHttpClientFactory factory = new BasicHttpClientFactory(user, pass);
        HttpClient client = factory.getClient();
        HttpParams params = client.getParams();
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 1000000);
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1000000);
        DeployClient deployClient = new DeployClient(client, new URI(url));
        return deployClient;
    }
    
    static private void createRootResource(String url, String user, String pass, String name) throws Exception{
        DeployClient cli = createDeployClient(url, user, pass);
        cli.createRootResoruce(name);
    }

    static private RenameResult renameResource(String url, String user, String pass, String origPath, String newPath, int resCount) {
        long duration = -1;
        try {
            DeployClient deployClient = createDeployClient(url, user, pass);
            long start = System.currentTimeMillis();
            deployClient.moveResource(origPath, newPath);
            long end = System.currentTimeMillis();
            duration = end - start;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new RenameResult(origPath,newPath, duration, resCount);
    }

    static private CopyResult copyResource(String url, String user, String pass, String origPath, String newPath, int resCount) {
        long duration = -1;
        try {
            DeployClient deployClient = createDeployClient(url, user, pass);
            long start = System.currentTimeMillis();
            deployClient.copyResource(origPath, newPath);
            long end = System.currentTimeMillis();
            duration = end - start;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new CopyResult(origPath,newPath, duration, resCount);
    }

    static private DeleteResult deleteResource(String url, String user, String pass, String path, int resCount) {
        long duration = -1;
        try {
            DeployClient deployClient = createDeployClient(url, user, pass);
            long start = System.currentTimeMillis();
            deployClient.deleteResource(path);
            long end = System.currentTimeMillis();
            duration = end - start;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new DeleteResult(duration, resCount);
    }
    
    public BatchResourceCreationTestAllChildrenOfOneParent(String serverUrl, String username, String password, int resourceCount) {
        this.createdResources = 0;
        this.resourceCount = resourceCount;
        this.rootResourceName = String.format("%06d-resource-root-%d", resourceCount, System.currentTimeMillis());
        this.rootResourcePath = String.format("/%s", rootResourceName);
        this.username = username;
        this.password = password;
        this.serverUrl = serverUrl;
    }

    public List<CreateResult> batchCreateResourcesOneParentManyChildren() {
        List<CreateResult> result = new ArrayList<CreateResult>();
        long duration = -1;
        try {
            DeployClient deployClient = createDeployClient(serverUrl, username, password);
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
                array.put(createResource(String.format("resource_%06d", i), root));
            }

            String data = array.toString();
            long start = System.currentTimeMillis();
            deployClient.batchCreateResoruces(data, rootResourceId);
            duration = System.currentTimeMillis() - start;
            result.add(new CreateResult(rootResourcePath, duration, createdResources));
            
            start = System.currentTimeMillis();
            deployClient.batchCreateResoruces(data, rootResourceId);
            duration = System.currentTimeMillis() - start;
            result.add(new CreateResult(rootResourcePath, duration, createdResources));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
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
}