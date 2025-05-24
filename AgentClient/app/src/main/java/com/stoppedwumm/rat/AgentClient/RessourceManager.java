package com.stoppedwumm.rat.AgentClient;

import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class RessourceManager {
    public static JsonNode readConfig() throws Exception {
        InputStream R = RessourceManager.class.getResourceAsStream("/config.json");
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(R);
            return rootNode;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}