/*
 * JIRA Plugin for SonarQube Copyright (C) 2009 SonarSource
 * dev@sonar.codehaus.org
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02
 */
package org.sonar.plugins.jira;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CheckProject;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.plugins.jira.rest.Authenticator;
import org.sonar.plugins.jira.rest.JiraRestServiceWrapper;
import org.sonar.plugins.jira.rest.JiraRestSession;

import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.IssueFieldId;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;

/**
 * @author samuelnunnally
 *
 */
public class JiraIssueCreatorPostJob implements PostJob, CheckProject {


  private static final Logger LOG = LoggerFactory.getLogger(JiraIssueCreatorPostJob.class);
  private Settings settings = null;
  private RuleFinder ruleFinder = null;
  private boolean autoCreateBlocker = false;
  private boolean autoCreateMajor = false;
  private boolean autoCreateCritical = false;
  private boolean autoCreateMinor = false;
  private boolean autoCreateInfo = false;

  public JiraIssueCreatorPostJob(Settings settings, RuleFinder ruleFinder) {
    this.settings = settings;

    if (this.settings != null) {
      this.autoCreateBlocker =
          Boolean.valueOf(settings.getString(JiraConstants.JIRA_AUTOCREATE_BLOCKER));
      this.autoCreateMajor =
          Boolean.valueOf(settings.getString(JiraConstants.JIRA_AUTOCREATE_MAJOR));
      this.autoCreateCritical =
          Boolean.valueOf(settings.getString(JiraConstants.JIRA_AUTOCREATE_CRITICAL));
      this.autoCreateMinor =
          Boolean.valueOf(settings.getString(JiraConstants.JIRA_AUTOCREATE_MINOR));
      this.autoCreateInfo = Boolean.valueOf(settings.getString(JiraConstants.JIRA_AUTOCREATE_INFO));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.sonar.api.batch.CheckProject#shouldExecuteOnProject(org.sonar.api.
   * resources.Project)
   */
  @Override
  public boolean shouldExecuteOnProject(Project arg0) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.sonar.api.batch.PostJob#executeOn(org.sonar.api.resources.Project,
   * org.sonar.api.batch.SensorContext)
   */
  @Override
  public void executeOn(Project project, SensorContext context) {
    LOG.info("ListAllIssuesPostJob");

    Client client = ClientBuilder.newBuilder().newClient();
    WebTarget target =
        client.target("http://localhost:9000").register(new Authenticator("admin", "admin"));

    target = target.path("api/issues/search").queryParam("projectKeys", project.getKey())
        .queryParam("additionalFields", "_all");

    Invocation.Builder builder = target.request();
    Response response = builder.get();
    Map<String, Object> jsonResponse = response.readEntity(Map.class);

    Object o = jsonResponse.get("issues");

    if (o != null) {

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> issues = (List<Map<String, Object>>) o;

      try {
        createIssues(project, context, issues);
      } catch (MalformedURLException e) {
        LOG.error(e.toString());
      }
    }

  }

  private List<Map<String, Object>> createIssues(Project project, SensorContext context,
      List<Map<String, Object>> issues) throws MalformedURLException {
    List<Map<String, Object>> issuesForJira = new ArrayList<Map<String, Object>>();

    try (JiraRestSession session =
        new JiraRestSession(new URL(settings.getString(JiraConstants.SERVER_URL_PROPERTY)));) {
      session.connect(settings.getString(JiraConstants.USERNAME_PROPERTY),
          settings.getString(JiraConstants.PASSWORD_PROPERTY));
      JiraService service = session.getJiraService(null, settings);

      Client client = ClientBuilder.newBuilder().newClient();


      for (Map<String, Object> issue : issues) {
        if (issue.containsKey("comments") && issue.get("comments") != null) {
          Object comments = issue.get("comments");

          if (comments instanceof List) {
            boolean hasJiraIssue = false;
            for (Map comment : (List<Map>) comments) {
              if (comment.containsKey("htmlText") && comment.get("htmlText") != null) {
                String htmlText = (String) comment.get("htmlText");
                if (StringUtils.contains(htmlText, "Issue linked to JIRA issue")) {
                  hasJiraIssue = true;
                  continue;
                }
              }
            }
            if(hasJiraIssue){
              continue;
            }
          }
        }

        Object severity = issue.get("severity");
        if (severity != null) {
          IssueInput jiraIssue = null;
          switch ((String) severity) {
            case "BLOCKER":
              if (autoCreateBlocker) {
                jiraIssue = createSonarIssue(context, issue);
                issuesForJira.add(issue);
              }
              break;
            case "CRITICAL":
              if (autoCreateCritical) {
                jiraIssue = createSonarIssue(context, issue);
                issuesForJira.add(issue);
              }
              break;
            case "MAJOR":
              if (autoCreateMajor) {
                jiraIssue = createSonarIssue(context, issue);
                issuesForJira.add(issue);
              }
              break;
            case "MINOR":
              if (autoCreateMinor) {
                jiraIssue = createSonarIssue(context, issue);
                issuesForJira.add(issue);
              }
              break;
            case "INFO":
              if (autoCreateInfo) {
                jiraIssue = createSonarIssue(context, issue);
                issuesForJira.add(issue);
              }
              break;
            default:
              continue;
          }

          if (jiraIssue != null) {
            BasicIssue basicIssue = service.createIssue(jiraIssue);


            Form form = new Form();
            form.param("issue", (String) issue.get("key"));
            form.param("text", generateCommentText(basicIssue, settings));

            WebTarget target = client.target("http://localhost:9000").path("api/issues/add_comment")
                .register(new Authenticator("admin", "admin"));
            Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

            Map<String, Object> jsonResponse = response.readEntity(Map.class);
          }
        }
      }
    }
    return issuesForJira;
  }

  protected String generateCommentText(BasicIssue issue, Settings settings) {
    StringBuilder message = new StringBuilder();
    message.append("Issue linked to JIRA issue: ");
    message.append(settings.getString(JiraConstants.SERVER_URL_PROPERTY));
    message.append("/browse/");
    message.append(issue.getKey());
    return message.toString();
  }

  private IssueInput createSonarIssue(SensorContext context, Map<String, Object> issue) {

    StringBuilder summary = new StringBuilder("SonarQube Issue #");
    summary.append(issue.get("key")).append(" - ").append(issue.get("rule"));



    IssueInputBuilder builder =
        new IssueInputBuilder(this.settings.getString(JiraConstants.JIRA_PROJECT_KEY_PROPERTY),
            Long.valueOf(settings.getString(JiraConstants.JIRA_ISSUE_TYPE_ID)), summary.toString());


    builder.setPriorityId(Long.valueOf(JiraRestServiceWrapper.sonarSeverityToJiraPriorityId(
        RulePriority.valueOf((String) issue.get("severity")), settings)));
    builder.setDescription(generateIssueDescription(issue));


    List<String> labels = getLabels();
    if (labels != null && !labels.isEmpty())
      builder.setFieldValue(IssueFieldId.LABELS_FIELD.id, labels);
    IssueInput result = builder.build();

    return result;
  }

  private String generateIssueDescription(Map<String, Object> issue) {
    StringBuilder description = new StringBuilder("Issue detail:");
    description.append("\"").append(issue.get("message")).append("\"").append("\ncomponent ")
        .append(issue.get("component")).append(", line ").append(issue.get("line"))
        .append("\n\nCheck it on SonarQube: ")
        .append(settings.getString(CoreProperties.SERVER_BASE_URL)).append("/issues/show/")
        .append(issue.get("key"));
    return description.toString();
  }

  protected List<String> getLabels() {
    List<String> labels = new ArrayList<>();
    String sonarLabel = settings.getString(JiraConstants.JIRA_ISSUE_SONAR_LABEL);
    if (sonarLabel != null)
      labels.add(sonarLabel);
    return labels;
  }

  private void printMap(Map<String, Object> jsonResponse) {
    for (String key : jsonResponse.keySet()) {
      Object value = jsonResponse.get(key);

      if (value instanceof Map) {
        LOG.info("key=" + key);
        printMap((Map) value);
      } else if (value instanceof List) {
        LOG.info("key=" + key);
        printList((List) value);
      } else {
        LOG.info("key=" + key + " : value=" + value);
      }
    }
  }

  private void printList(List list) {
    for (Object o : list) {

      if (o instanceof Map) {
        printMap((Map) o);
      } else if (o instanceof List) {
        printList((List) o);
      } else {
        LOG.info("value=" + o);
      }
    }
  }
}
