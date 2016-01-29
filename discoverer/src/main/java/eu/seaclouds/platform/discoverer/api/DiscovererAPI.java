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

package eu.seaclouds.platform.discoverer.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.seaclouds.platform.discoverer.core.Discoverer;
import eu.seaclouds.platform.discoverer.core.Offering;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

@Path("/")
public class DiscovererAPI {

    private final Discoverer discoverer;
    private org.json.simple.parser.JSONParser JSONParser;
    static Logger log = LoggerFactory.getLogger(DiscovererAPI.class);

    public DiscovererAPI(Discoverer discoverer) {
        this.discoverer = discoverer;
        this.JSONParser = new JSONParser();
    }

    @DELETE
    @Path("/delete/{oid}")
    public Response deleteOfferingById(@PathParam("oid") String offerId)
            throws IOException {

        /* return if the offering id is valid and the discoverer is able to remove the offering associated */
        if (this.discoverer.removeOffering(offerId)) {
            return Response.ok("Offering deleted").build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Offering not found").build();
        }

    }

    @GET
    @Path("/fetch_all")
    @Produces(MediaType.APPLICATION_JSON)
    public FetchAllRepresentation getAllOfferings() throws IOException {

        /* collecting all the ids within the repository */
        ArrayList<String> ids = new ArrayList<String>();
        Collection<String> offeringIds = discoverer.getAllOfferingIds();
        for (String offeringId : offeringIds) {
            ids.add(offeringId);
        }

        Offering offering = this.discoverer.fetchOffer("all");
        String offeringTOSCA = "";

        if(offering != null) {
            offeringTOSCA = offering.toscaString;
        }

        if (offeringTOSCA.isEmpty()) {
            ids.clear();
        }

        return new FetchAllRepresentation(ids, offeringTOSCA);
    }

    @GET
    @Path("/fetch")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<String> getOfferingIds()
            throws IOException {

        /* collecting all the ids within the repository */
        ArrayList<String> ids = new ArrayList<String>();
        Collection<String> offeringIds = discoverer.getAllOfferingIds();
        for (String offeringId : offeringIds) {
            ids.add(offeringId);
        }

        return ids;
    }

    @GET
    @Path("/fetch/{oid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOfferingById(@PathParam("oid") String offerId) {

        /* input check */
        if(!Offering.validateOfferingId(offerId)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Offering not found.").build();
        }

        /* fetching the offering */
        Offering offering = this.discoverer.fetchOffer(offerId);

        if (offering == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);

        return Response.ok(new OfferingRepresentation(offerId, offering.toTosca())).build();
    }

    @GET
    @Path("/fetchif")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<OfferingRepresentation> getOfferingsIf(@QueryParam("constraints") String constraints, @QueryParam("provider") String provider)
            throws IOException {

        ArrayList<OfferingRepresentation> offerings = new ArrayList<>();

        JSONObject constraintsObject;

        try {
            constraintsObject = (JSONObject) JSONParser.parse(constraints);
            Collection<String> offeringIds = discoverer.getAllOfferingIds();
            /* removing the special offer containing all offerings */
            offeringIds.remove("all");

            for (String offeringId : offeringIds) {
                Offering offering = this.discoverer.fetchOffer(offeringId);
                if (this.satisfyAllConstraints(constraintsObject, provider, offering)) {
                    offerings.add(new OfferingRepresentation(offeringId, offering.toTosca()));
                }
            }
        } catch (ParseException e) {
            log.error("Cannot parse constraints");
            log.error(e.getMessage());
        }

        return offerings;
    }

    @PUT
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public void refreshRepository() {
        this.discoverer.refreshRepository();
    }

    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public StatisticsRepresentation getStatistics() {
        int crawledTimes = discoverer.crawledTimes;
        int totalCrawledOfferings = discoverer.totalCrawledOfferings;
        Date lastCrawl = discoverer.lastCrawl;

        return new StatisticsRepresentation(crawledTimes, totalCrawledOfferings, lastCrawl);
    }

    private class FetchAllRepresentation {

        private ArrayList<String> offeringIds;
        private String offering;

        public FetchAllRepresentation(ArrayList<String> offeringIds, String offering) {
            this.offeringIds = offeringIds;
            this.offering = offering;
        }

        @JsonProperty("offering_ids")
        public ArrayList<String> getOfferingIds() {
            return this.offeringIds;
        }

        @JsonProperty("offering")
        public String getOffering() {
            return this.offering;
        }
    }

    public class OfferingRepresentation {

        private String offeringId;
        private String offering;

        public OfferingRepresentation(String offeringId, String offering) {
            this.offeringId = offeringId;
            this.offering = offering;
        }

        @JsonProperty("offering_id")
        public String getOfferingId() {
            return offeringId;
        }

        @JsonProperty("offering")
        public String getOffering() {
            return offering;
        }
    }

    private boolean satisfyAllConstraints(JSONObject constraintsObject, String provider, Offering offering) {
        String toscaString = offering.toTosca();
        Set keys = constraintsObject.keySet();

        /* if the provider is specified, but it is not the one of this offerings, it does not satisfy all the constraints */
        if (provider != null && !hasProvider(toscaString, provider))
            return false;

        for (Object key : keys) {
            String constraintName = (String) key;
            JSONArray constraintOperatorAndValue = (JSONArray) constraintsObject.get(key);
            String constraintOperator = (String) constraintOperatorAndValue.get(0);

            if (!satisfyConstraint(constraintName, constraintOperator, constraintOperatorAndValue, toscaString))
                return false;
        }

        return true;
    }

    private boolean hasProvider(String toscaString, String expectedProvider) {
        String pre = "type: seaclouds.nodes.";
        int i = toscaString.indexOf(pre) + pre.length();
        while (toscaString.charAt(i++) != '.');
        toscaString = toscaString.substring(i);

        String actualProvider = toscaString.split(System.getProperty("line.separator"))[0];

        return actualProvider.equals(expectedProvider);
    }

    private boolean satisfyConstraint(String constraintName, String constraintOperator, JSONArray constraintValue, String toscaString) {
        /* look for properties inside tosca string */
        toscaString = toscaString.substring(toscaString.indexOf("properties:") + "properties:".length());
        /* look for specified property */
        int i = toscaString.indexOf(constraintName);

        /* property is not present, constraint is not satisfied */
        if (i == -1) {
            return false;
        }

        i += constraintName.length();
        while (toscaString.charAt(i++) != ':');
        if (toscaString.charAt(i) == ' ') i++;
        toscaString = toscaString.substring(i);

        String actualValue = toscaString.split(System.getProperty("line.separator"))[0];

        if (constraintOperator.equals("in")) {
            return inOperator(actualValue, constraintValue);
        } else {
            String expectedValue = (String) constraintValue.get(1);
            return numberOperator(constraintOperator, Float.parseFloat(actualValue), Float.parseFloat(expectedValue));
        }
    }

    private boolean inOperator(String actualValue, JSONArray expectedValues) {
        for (int i = 1; i < expectedValues.size(); i++){
            String value = (String) expectedValues.get(i);
            if (value.equals(actualValue))
                return true;
        }

        return false;
    }

    private boolean numberOperator(String constraintOperator, Float actualValue, Float expectedValue) {
        switch (constraintOperator)
        {
            case ">":
                return actualValue > expectedValue;
            case "<":
                return actualValue < expectedValue;
            case "=":
                return actualValue.equals(expectedValue);
            default:
                return false;
        }
    }

    private class StatisticsRepresentation {

        private int crawledTimes;
        private int totalCrawledOfferings;
        private Date lastCrawl;

        public StatisticsRepresentation(int crawledTimes, int totalCrawledOfferings, Date lastCrawl) {
            this.crawledTimes = crawledTimes;
            this.totalCrawledOfferings = totalCrawledOfferings;
            this.lastCrawl = lastCrawl;
        }

        @JsonProperty("crawled_times")
        public int getCrawledTimes() {
            return crawledTimes;
        }

        @JsonProperty("total_crawled_offerings")
        public int getTotalCrawledOfferings() {
            return totalCrawledOfferings;
        }

        @JsonProperty("last_crawl")
        public Date getLastCrawl() {
            return lastCrawl;
        }
    }
}