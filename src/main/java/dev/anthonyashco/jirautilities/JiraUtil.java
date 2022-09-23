package dev.anthonyashco.jirautilities;

import dev.anthonyashco.exceptions.HTTPException;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

/**
 * JiraUtil is a utility class for handling common Jira API tasks.
 */
public class JiraUtil {
    private final Properties prop;
    private final String projectId;
    private final UrlUtil url;
    private String versionId = null;
    private String cycleId = null;

    /**
     * Generates the base64-encoded string for the user/pass pair in jira.properties.
     */
    private String getB64AuthKey() {
        String credentials = prop.getProperty("user") + ":" + prop.get("pass");
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateB64AuthKey(String user, String pass) {
        String credentials = user + ":" + pass;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    public JiraUtil() throws IOException, HTTPException {
        prop = new Properties();
        prop.load(new FileInputStream("jira.properties"));
        String projectUrl = prop.getProperty("url");
        String authKey;
        if ((authKey = prop.getProperty("authKey")).isEmpty()) authKey = getB64AuthKey();
        url = new UrlUtil(projectUrl, authKey);
        projectId = getProjectId(prop.getProperty("projectName"));
    }

    /**
     * Gets the Jira project ID from the project name and stores it as a property.
     */
    public String getProjectId(String projectName) throws IOException, HTTPException {
        JsonObject json = url.getJson("rest/api/2/project/" + projectName);
        return json.get("id").getAsString();
    }

    /**
     * Gets the Jira version ID from the version name and stores it as a property.
     */
    public String getVersionId(String versionName) throws IOException, HTTPException {
        JsonObject json = url.getJson("rest/zapi/latest/util/versionBoard-list?projectId=" + projectId);
        JsonArray jsonArray = json.getAsJsonArray("unreleasedVersions");
        for (JsonElement element : jsonArray) {
            String label = element.getAsJsonObject().get("label").getAsString();
            if (label.equalsIgnoreCase(versionName)) {
                return (versionId = element.getAsJsonObject().get("value").getAsString());
            }
        }
        throw new JsonParseException(String.format("Version name %s not found.", versionName));
    }

    /**
     * Gets the Jira cycle ID if a cycle with the given name exists, and creates a new cycle if it doesn't.
     */
    public String getOrCreateCycle(String cycleName, String versionName) throws HTTPException, IOException {
        getVersionId(versionName);
        return getOrCreateCycle(cycleName);
    }

    /**
     * Gets the Jira cycle ID if a cycle with the given name exists, and creates a new cycle if it doesn't.
     * Requires versionId to be set before use. Try the {@link #getOrCreateCycle(String, String) overloaded} version
     * if the versionId still needs to be set.
     */
    public String getOrCreateCycle(String cycleName) throws HTTPException, IOException {
        if (versionId == null) throw new NullPointerException("Version ID is not set.");
        try {
            return getCycleId(cycleName);
        } catch (JsonParseException e) {
            JsonObject payload = new JsonObject();
            payload.addProperty("name", cycleName);
            payload.addProperty("projectId", projectId);
            payload.addProperty("versionId", versionId);
            JsonObject json = url.postJson("rest/zapi/latest/cycle/", payload);
            return json.get("id").getAsString();
        }
    }

    /**
     * Gets the Jira cycle ID from the cycle name and sets it as a property.
     */
    public String getCycleId(String cycleName, String versionName) throws HTTPException, IOException {
        getVersionId(versionName);
        return getCycleId(cycleName);
    }

    /**
     * Gets the Jira cycle ID from the cycle name and sets it as a property.
     * Requires versionId to be set before use. Try the {@link #getCycleId(String, String) overloaded} version
     * if the versionId still needs to be set.
     */
    public String getCycleId(String cycleName) throws HTTPException, IOException {
        if (versionId == null) throw new NullPointerException("Version ID is not set.");
        JsonObject json = url.getJson("rest/zapi/latest/cycle?projectId=" + projectId +
                "&versionId=" + versionId);
        for (String keyString : json.keySet()) {
            Object keyValue = json.get(keyString);
            if (keyValue instanceof JsonObject) {
                String name = ((JsonObject) keyValue).get("name").getAsString();
                if (name.equalsIgnoreCase(cycleName)) return (cycleId = keyString);
            }
        }
        throw new JsonParseException(String.format("Cycle name %s not found.", cycleName));
    }

    /**
     * Gets the Jira issue ID from the issue name.
     */
    public String getIssueId(String issueName) throws HTTPException, IOException {
        JsonObject json = url.getJson("/rest/api/2/issue/" + issueName);
        return json.get("id").getAsString();
    }

    /**
     * Gets the Jira execution ID from the execution name.
     * Requires versionId and cycleId to be set first.
     */
    public String getExecutionId(String issueName) throws HTTPException, IOException {
        if (versionId == null || cycleId == null)
            throw new NullPointerException("Version ID and Cycle ID must both be set.");
        JsonObject json = url.getJson("rest/zapi/latest/execution?cycleId=" + cycleId
                + "&projectId=" + projectId + "&versionId=" + versionId + "&issueId=" + getIssueId(issueName));
        return json.get("executions").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
    }

    /**
     * Adds an array of tests to a Jira test cycle using their names.
     *
     * @return Jira jobProgressToken
     */
    public String addTestsToCycle(String[] issues, String cycleName, String versionName) throws HTTPException, IOException {
        getVersionId(versionName);
        getCycleId(cycleName);
        return addTestsToCycle(issues);
    }

    /**
     * Adds an array of tests to a Jira test cycle using their names.
     * Requires versionId to be set first. Try the {@link #addTestsToCycle(String[], String, String) overloaded}
     * version if the versionId still needs to be set.
     *
     * @return Jira jobProgressToken
     */
    public String addTestsToCycle(String[] issues, String cycleName) throws HTTPException, IOException {
        getCycleId(cycleName);
        return addTestsToCycle(issues);
    }

    /**
     * Adds an array of tests to a Jira test cycle using their names.
     * Requires versionId and cycleId to be set first. Try one of the overloaded versions if the versionId still needs
     * to be set.
     *
     * @return Jira jobProgressToken
     */
    public String addTestsToCycle(String[] issues) throws HTTPException, IOException {
        if (cycleId == null) throw new NullPointerException("Cycle ID is not set.");

        JsonArray issuesList = new JsonArray();
        for (String issue : issues) issuesList.add(issue);

        JsonObject payload = new JsonObject();
        payload.addProperty("method", "1");
        payload.addProperty("projectId", projectId);
        payload.addProperty("versionId", versionId);
        payload.addProperty("cycleId", cycleId);
        payload.add("issues", issuesList);

        JsonObject json = url.postJson("/rest/zapi/latest/execution/addTestsToCycle/", payload);
        return json.get("jobProgressToken").getAsString();
    }

    /**
     * Updates the Jira execution status of a test execution.
     *
     * @param statusCode Status codes can be found in {@link ExecutionStatus ExecutionStatus}.
     * @return Whether the operation was successful.
     */
    public boolean updateExecutionStatus(String executionId, int statusCode) throws HTTPException, IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("status", statusCode);

        JsonObject json = url.putJson("rest/zapi/latest/execution/" + executionId + "/execute", payload);
        int executionStatus = json.get("executionStatus").getAsInt();
        if (executionStatus == statusCode) return true;
        else throw new HTTPException("Execution status updated failed.");
    }

    /**
     * Attaches a file to a Jira execution.
     *
     * @param fileMimeType The mime type of the uploaded file. Unfortunately needs to be Googled.
     * @return Whether the operation was successful.
     */
    public boolean attachExecutionReport(String executionId, Path reportPath, String fileMimeType) throws HTTPException, IOException {
        JsonObject json = url.postFile("rest/zapi/latest/attachment?entityId=" + executionId + "&entityType=Execution", reportPath.toFile(), fileMimeType);
        String success = json.get("success").getAsString();
        if (success.contains("successfully uploaded")) return true;
        else throw new HTTPException("File upload failed.");
    }

    /**
     * Executes a Jira test execution, updating the execution status and uploading a file.
     *
     * @param statusCode Status codes can be found in {@link ExecutionStatus ExecutionStatus}.
     * @param fileMimeType The mime type of the uploaded file. Unfortunately needs to be Googled.
     * @return Whether the operation was successful.
     */
    public boolean executeTest(String issueName, int statusCode, Path reportPath, String fileMimeType) throws HTTPException, IOException {
        if (versionId == null || cycleId == null)
            throw new NullPointerException("Version ID and Cycle ID must both be set.");
        String executionId = getExecutionId(issueName);
        boolean executionStatus = updateExecutionStatus(executionId, statusCode);
        boolean attachStatus = attachExecutionReport(executionId, reportPath, fileMimeType);
        return (executionStatus && attachStatus);
    }

    public static void main(String[] args) throws HTTPException, IOException {
        JiraUtil ju = new JiraUtil();
        System.out.println(ju.getB64AuthKey());
    }
}
