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

import java.util.List;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.jira.metrics.JiraMetrics;
import org.sonar.plugins.jira.metrics.JiraSensor;
import org.sonar.plugins.jira.metrics.JiraWidget;
import org.sonar.plugins.jira.reviews.JiraActionDefinition;
import org.sonar.plugins.jira.reviews.JiraIssueCreator;
import org.sonar.plugins.jira.reviews.LinkFunction;

import com.google.common.collect.ImmutableList;

@Properties({
    @Property(key = JiraConstants.SERVER_URL_PROPERTY, name = "Server URL",
        description = "Example : http://jira.codehaus.org", global = true, project = true,
        module = false),
    @Property(key = JiraConstants.USERNAME_PROPERTY, defaultValue = "", name = "Username",
        global = true, project = true, module = false),
    @Property(key = JiraConstants.PASSWORD_PROPERTY, name = "Password", global = true,
        project = true, module = false),
    @Property(key = JiraConstants.JIRA_SONAR_API_URL, defaultValue = "", name = "Sonar API URL",
        description = "Sonar API interaction URL", global = true, project = true, module = false),
    @Property(key = JiraConstants.JIRA_SONAR_API_USER, defaultValue = "",
        name = "Sonar API account", description = "Sonar user account for API interaction",
        global = true, project = true, module = false),
    @Property(key = JiraConstants.JIRA_SONAR_API_PASSWORD, name = "Password",
        description = "Sonar user account password for API interaction", global = true,
        project = true, module = false),
    @Property(key = JiraConstants.JIRA_EPIC_CUSTOM_FIELD, name = "Epic Custom Field",
        description = "Custom Field ID for Epic", global = true, project = true, module = false)

})
public final class JiraPlugin extends SonarPlugin {

  @SuppressWarnings("rawtypes")
  public List getExtensions() {
    return ImmutableList.of(
        // metrics part
        JiraMetrics.class, JiraSensor.class, JiraWidget.class,

    // issues part
        JiraIssueCreator.class, LinkFunction.class, JiraActionDefinition.class,

    // create JIRA issues if configured
        JiraIssueCreatorPostJob.class);
  }
}
