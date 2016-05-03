package com.ibm.ucd;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ResourceCreationAnMappingUtil {
    final private int applicationCount;
    final private int componentCount;
    final private String rootResourceName;
    final private String rootResourcePath;
    final private String applicationNameFormat;
    final private String agentNameFormat;
    final private String componentNameFormat;
    final private DeployClient deployClient;

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.out.println("java -jar ucd-create-app.jar "
                    + "<server_url> "
                    + "<username> "
                    + "<password> "
                    + "<application_count> "
                    + "<component_count>");
            System.exit(1);
        }
        URI serverUrl = new URI(args[0]);
        String username = args[1];
        String password = args[2];
        int appCount = Integer.parseInt(args[3]);
        int componentCount = Integer.parseInt(args[4]);

        new ResourceCreationAnMappingUtil(serverUrl, username, password, appCount, componentCount).mapResources();
    }

    public ResourceCreationAnMappingUtil(
            URI serverUrl,
            String username,
            String password,
            int applicationCount,
            int componentCount)
    {
        this.applicationCount = applicationCount;
        this.componentCount = componentCount;
        this.applicationNameFormat = "%d-Load-%d";
        this.componentNameFormat = "%s-Component-%d";
        this.agentNameFormat = System.getProperty("agent.pattern", "agent%06d");
        this.rootResourceName = "ROOT-" + System.currentTimeMillis();
        this.rootResourcePath = "/" + rootResourceName;

        BasicHttpClientFactory factory = new BasicHttpClientFactory(username, password);
        HttpClient client = factory.getClient();
        this.deployClient = new DeployClient(client, serverUrl);
    }

    public void mapResources()
    throws JSONException {
        String rootResourceId = deployClient.createRootResoruce(rootResourceName);

        Map<String, String> agent2id = deployClient.getAgentsFromServer();
        Map<String, String> role2Id = deployClient.getComponentResourceRolesFromServer();
        deployClient.getComponentResourceRolesFromServer();

        JSONArray array = new JSONArray();
        JSONObject root = new JSONObject();
        root.put("name", rootResourceName);
        root.put("path", rootResourcePath);
        root.put("inheritTeam", "true");
        root.put("description", "");
        array.put(root);

        Map<String, Map<String, String>> application2environment2baseResource = new HashMap<String, Map<String,String>>();
        for (int appNum = 0; appNum < applicationCount; appNum++) {
            String appName = String.format(applicationNameFormat, componentCount, appNum + 1);
            JSONObject appResource = createResource(appName, root, null, null);
            String appResourcePath = appResource.getString("path");

            Map<String, String> environment2baseResource = new HashMap<String, String>();
            environment2baseResource.put("Load Test", appResourcePath);
            application2environment2baseResource.put(appName, environment2baseResource);
            array.put(appResource);

            // Create the agent resources
            for (int i = 0; i < componentCount; i++) {
                int agentNumber = (appNum * componentCount) + i + 1;
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

    private JSONObject createResource(String name, JSONObject parent, String agentId, String roleId) {
        JSONObject resource = new JSONObject();
        try {
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
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return resource;
    }
}
