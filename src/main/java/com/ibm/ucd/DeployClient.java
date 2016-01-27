package com.ibm.ucd;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class DeployClient {

    private HttpClient client;
    private String serverUrl;
    private String allAgentsEndpoint;
    private String componentResourceRolesEndpoint;
    private String nameKey;
    private String idKey;

    public DeployClient(HttpClient client, String serverUrl) {
        this.client = client;
        this.serverUrl = serverUrl;
        allAgentsEndpoint = serverUrl + "/rest/agent";
        componentResourceRolesEndpoint = serverUrl + "/rest/resource/resourceRole/componentRoles";
        nameKey = "name";
        idKey = "id";
    }

    protected Map<String, String> getAgentsFromServer() {
        Map<String, String> name2id = new HashMap<String, String>();

        HttpGet httpGet = new HttpGet(allAgentsEndpoint);
        HttpResponse response;
        try {
            response = client.execute(httpGet);

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            JSONArray agentObjects = new JSONArray(responseString);
            int length = agentObjects.length();

            for (int i = 0; i < length; i++) {
                JSONObject agentObject = agentObjects.getJSONObject(i);
                String agentName = agentObject.getString(nameKey);
                String agentId = agentObject.getString(idKey);
                name2id.put(agentName, agentId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return name2id;
    }

    protected Map<String, String> getComponentResourceRolesFromServer() {
        Map<String, String> name2id = new HashMap<String, String>();

        HttpGet httpGet = new HttpGet(componentResourceRolesEndpoint);
        HttpResponse response;
        try {
            response = client.execute(httpGet);

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            JSONArray resourceRoles = new JSONArray(responseString);
            int length = resourceRoles.length();

            for (int i = 0; i < length; i++) {
                JSONObject resoruceRoleObject = resourceRoles.getJSONObject(i);
                String roleName = resoruceRoleObject.getString(nameKey);
                String roleId = resoruceRoleObject.getString(idKey);
                name2id.put(roleName, roleId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return name2id;
    }

    protected void batchCreateResoruces(String body, String rootResourceId) {
        HttpPut put = new HttpPut(serverUrl + "/rest/resource/resource/" + rootResourceId + "/batch");
        StringEntity entity;
        entity = new StringEntity(body, Charset.forName("UTF-8"));
        put.setEntity(entity);

        try {
            client.execute(put);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean mapResourcesToComponents(Map<String, Map<String,String>> application2environment2baseResource) {
        for (String applicationNameOrId : application2environment2baseResource.keySet()) {
            Map<String, String> environment2baseResource = application2environment2baseResource.get(applicationNameOrId);
            for (String environmentNameOrId : environment2baseResource.keySet()) {
                String baseResource = environment2baseResource.get(environmentNameOrId);
                mapBaseResourceToEnvironment(environmentNameOrId, applicationNameOrId, baseResource);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void mapBaseResourceToEnvironment(String environmentNameOrId, String applicationNameOrId, String baseResourceIdOrPath) {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost("localhost").setPort(8080).setPath("/cli/environment/addBaseResource");

        builder.setParameter("environment", environmentNameOrId);
        builder.setParameter("application", applicationNameOrId);
        builder.setParameter("resource", baseResourceIdOrPath);

        HttpPut httpPut = null;
        try {
            httpPut = new HttpPut(builder.build());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        try {
            client.execute(httpPut);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String createRootResoruce(String name) {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http")
                .setHost("localhost")
                .setPort(8080)
                .setPath("/cli/resource/create")
                .setParameter("name", name);

        HttpPut httpPut = null;
        try {
            httpPut = new HttpPut(builder.build());
            StringEntity entity = new StringEntity("{\"name\" : \"" + name + "\"}", Charset.forName("UTF-8"));
            httpPut.setEntity(entity);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpResponse response = null;
        try {
            response = client.execute(httpPut);
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new RuntimeException("Http status code: " + statusCode);
        }

        String resultString;
        try {
            resultString = EntityUtils.toString(response.getEntity());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            JSONObject obj = new JSONObject(resultString);
            return obj.getString("id");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}