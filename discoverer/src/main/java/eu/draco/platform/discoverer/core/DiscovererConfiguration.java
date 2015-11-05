/**
 * Copyright 2014 SeaClouds
 * Contact: SeaClouds
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.draco.platform.discoverer.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;


public class DiscovererConfiguration extends Configuration {
	@Valid
	@NotNull
	@JsonProperty("refreshInterval")
	private int refreshInterval;

	@Valid
	@NotNull
	@JsonProperty("crawlOnStartup")
	private boolean crawlOnStartup;

	@Valid
	@JsonProperty("activeCrawlers")
	private ArrayList<String> activeCrawlers = new ArrayList<String>();

	@JsonProperty("refreshInterval")
	public void setRefreshInterval(int refreshInterval) { this.refreshInterval = refreshInterval; }

	@JsonProperty("refreshInterval")
	public int getRefreshInterval() { return this.refreshInterval; }

	@JsonProperty("crawlOnStartup")
	public void setCrawlOnStartup(boolean crawlOnStartup) { this.crawlOnStartup = crawlOnStartup; }

	@JsonProperty("crawlOnStartup")
	public boolean getCrawlOnStartup() { return this.crawlOnStartup; }

	@JsonProperty("activeCrawlers")
	public void setActiveCrawlers(ArrayList<String> activeCrawlers) {
		for (String crawlerName : activeCrawlers) {
			this.activeCrawlers.add(crawlerName);
		}
	}

	@JsonProperty("activeCrawlers")
	public ArrayList<String> getActiveCrawlers() { return this.activeCrawlers; }
}

