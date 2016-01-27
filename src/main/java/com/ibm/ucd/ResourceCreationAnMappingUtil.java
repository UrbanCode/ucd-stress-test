package com.ibm.ucd;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ResourceCreationAnMappingUtil {
    static private String rootResourceName = "ROOT4";
    static private String rootResourcePath = "/" + rootResourceName;
    static private String applicationNameFormat = "5-Load-%d"; // 5-Load-[1-5]
    static private String agentNameFormat = "perf-agent-%06d"; // perf-agent-000[001-500]
    static private String componentNameFormat = "%s-Component-%d"; // 5-Load-[1-100]-Component-[1-5]
    static private String serverUrl = "http://localhost:8080";
    static private String username = "admin";
    static private String password = "admin";
    final static private int appCount = 1;
    final static private int agentCount = 5;
    final static private int agentPerAppCount = agentCount/appCount;

    public static void main(String[] args) throws Exception {
        BasicHttpClientFactory factory = new BasicHttpClientFactory(username, password);
        HttpClient client = factory.getClient();

        DeployClient deployClient = new DeployClient(client, serverUrl);

        String rootResourceId = deployClient.createRootResoruce(rootResourceName);

        Map<String, String> agent2id = deployClient.getAgentsFromServer();
        Map<String, String> role2Id = deployClient.getComponentResourceRolesFromServer();
        deployClient.getComponentResourceRolesFromServer();

        JSONArray array = new JSONArray();

        // create the root resource
        JSONObject root = new JSONObject();
        root.put("name", rootResourceName);
        root.put("path", rootResourcePath);
        root.put("inheritTeam", "true");
        root.put("description", "");
        array.put(root);

        Map<String, Map<String, String>> application2environment2baseResource = new HashMap<String, Map<String,String>>();
        for (int app = 0; app < appCount; app++) {
            String appName = String.format(applicationNameFormat, app + 1);
            JSONObject appResource = createResource(appName, root, null, null);
            String appResourcePath = appResource.getString("path");

            Map<String, String> environment2baseResource = new HashMap<String, String>();
            environment2baseResource.put("Load Test", appResourcePath);
            application2environment2baseResource.put(appName, environment2baseResource);
            array.put(appResource);

            // Create the agent resources
            for (int i = 0; i < agentPerAppCount; i++) {
                int agentNumber = (app * agentPerAppCount) + i + 1;
                String agentName = String.format(agentNameFormat, agentNumber);
                JSONObject agentResource = createResource(agentName, appResource, agent2id.get(agentName), null);
                array.put(agentResource);

                String componentName = String.format(componentNameFormat, appName, i+1);
                JSONObject componentResource = createResource(componentName, agentResource, null, role2Id.get(componentName));
                array.put(componentResource);
            }
        }
        deployClient.batchCreateResoruces(array.toString(), rootResourceId);
        deployClient.mapResourcesToComponents(application2environment2baseResource);
    }

    public static JSONObject createResource(String name, JSONObject parent, String agentId, String roleId) throws JSONException {
        JSONObject resource = new JSONObject();
        resource.put("name", name);
        resource.put("path", parent.get("path") + "/" + name);
        resource.put("inheritTeam", "true");
        resource.put("description", "");
        if (agentId != null) {
            resource.put("agentId", agentId);
            resource.put("useImpersonation", "false");
        }
        if (roleId != null) {
            resource.put("roleId", roleId);
            JSONObject roleProps = new JSONObject();
            roleProps.put("exampleProp", "exampleValue");
            resource.put("roleProperties", roleProps);
        }
        return resource;
    }
}