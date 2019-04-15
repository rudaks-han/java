package service;

import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JiraService
{
    private String jiraUrl;
    private String user;
    private String password;

    private String jiraSearchUrl;

    public JiraService(String jiraUrl, String user, String password)
    {
        this.jiraUrl = jiraUrl;
        this.user = user;
        this.password = password;

        jiraSearchUrl = jiraUrl + "/rest/api/2/search";
    }

    public List<HashMap<String, String>> getPatchList(String searchCondition) throws UnsupportedEncodingException
    {
        List resultList = null;
        String param = URLEncoder.encode(searchCondition, "UTF-8");

        HashMap<String, Object> resultMap = searchJiraByJql(param);

        if (resultMap != null)
        {
            resultList = parseJiraSearchMap(resultMap);
        }

        return resultList;
    }

    public HashMap<String, Object> searchJiraByJql(String param)
    {
        HashMap resultMap = null;

        try
        {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(getMessageConverters());
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(user, password));

            URI uri = new URI(jiraSearchUrl + "?jql=" + param);
            Util.debug("[condition] " + uri.getQuery());

            resultMap = restTemplate.getForObject(uri, HashMap.class);
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
            Util.debug(e.getMessage());
        }

        return resultMap;
    }

    private List parseJiraSearchMap(HashMap map)
    {
        List<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();

        if (map != null)
        {
            ArrayList<HashMap> issues = (ArrayList<HashMap>) map.get("issues");
            Util.debug("[issue count] " + issues.size() + "");

            for (HashMap issueMap : issues)
            {
                HashMap<String, String> resultMap = new HashMap<String, String>();

                String key = (String) issueMap.get("key");
                HashMap<String, Object> fields = (HashMap<String, Object>) issueMap.get("fields");
                String summary = (String) fields.get("summary");
                String priority = "";
                if (fields.get("priority")!= null)
                    priority = (String) ((HashMap<String, Object>) fields.get("priority")).get("name");

                String patchImportance = "";
                if (fields.get("customfield_11100") != null)
                    patchImportance = (String) ((HashMap<String, Object>) fields.get("customfield_11100")).get("value");

                String menu = "";
                if (fields.get("customfield_10202") != null)
                    menu = (String) ((HashMap<String, Object>) fields.get("customfield_10202")).get("value");

                String revision = (String) fields.get("customfield_10203"); // revision
                String responseHistory = (String) fields.get("customfield_10021"); // 처리내역
                String description = (String) fields.get("description"); // description

                /*
                System.err.println("==============================================================");
                System.err.println("key : " + key);
                System.err.println("summary : " + summary);
                System.err.println("priority : " + priority);
                System.err.println("patchImportance : " + patchImportance);
                System.err.println("menu : " + menu);
                System.err.println("revision : " + revision);
                System.err.println("responseHistory : " + responseHistory);
                //System.err.println("description : " + description);
                System.err.println("==============================================================");
                */

                resultMap.put("key", key);
                resultMap.put("summary", summary);
                resultMap.put("priority", priority);
                resultMap.put("patchImportance", patchImportance);
                resultMap.put("menu", menu);
                resultMap.put("revision", revision);
                resultMap.put("responseHistory", responseHistory);
                resultMap.put("description", description);

                resultList.add(resultMap);
            }
        }

        return resultList;
    }

    private List<HttpMessageConverter<?>> getMessageConverters()
    {
        List<HttpMessageConverter<?>> converters =
                new ArrayList<HttpMessageConverter<?>>();
        converters.add(new MappingJackson2HttpMessageConverter());
        return converters;
    }
}
