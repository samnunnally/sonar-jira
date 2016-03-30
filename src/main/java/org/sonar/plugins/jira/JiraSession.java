/*
JIRA Plugin for SonarQube
Copyright (C) 2009 SonarSource
dev@sonar.codehaus.org

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
*/

package org.sonar.plugins.jira;

import java.net.URL;

import org.sonar.api.config.Settings;
import org.sonar.api.rules.RuleFinder;

public interface JiraSession extends AutoCloseable
{

	public void connect(String userName, String password);
	public JiraService getJiraService(RuleFinder ruleFinder, Settings settings);
	public String getAuthenticationToken();
	public URL getWebServiceUrl();
	public void close();
	
}
