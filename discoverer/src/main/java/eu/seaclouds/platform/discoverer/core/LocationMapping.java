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


package eu.seaclouds.platform.discoverer.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

public class LocationMapping {

    private static HashMap<String, String> map = new HashMap<>();
    static Logger log = LoggerFactory.getLogger(LocationMapping.class);

    public static void initializeMap(InputStream resource) {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(resource);
            br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                String parts[] = line.split("=");
                map.put(parts[0], parts[1]);
            }

        } catch (IOException e) {
            log.error("Cannot open location mapping file");
            log.error(e.getMessage());
        } finally {
            try {
                if (isr != null) {
                    isr.close();
                }
                if (br != null) {
                    br.close();
                }

                if (resource != null) {
                    resource.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }


    /**
     * Gets the sanitized location of an offering
     *
     * @param providerName the name of the provider
     * @return the sanitized location string if sanitizable, null otherwise
     */
    public static String getLocation(String providerName) {
        for (String key : map.keySet()) {
            if (providerName.startsWith(key)) {
                return map.get(key);
            }
        }

        return null;
    }

}
