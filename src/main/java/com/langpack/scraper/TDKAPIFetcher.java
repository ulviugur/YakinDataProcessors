package com.langpack.scraper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langpack.tdk.Anlam;
import com.langpack.tdk.Madde;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class TDKAPIFetcher {
    
	public static final String TDK_BASE_URL = "https://sozluk.gov.tr/gts";
	
	private static String getPullURL(String word) {
		String retval = TDK_BASE_URL + "?ara=" + word;
		return retval;
	}
	
    public static List<Madde> fetchJsonFromTDK(String word) throws Exception {
        // Create URL object
    	String pullURL = getPullURL(word);
        URL url = new URL(pullURL);
        
        // Open a connection to the URL
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        // Get the response code
        int responseCode = conn.getResponseCode();
        
        // Check if the response code is HTTP OK (200)
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Create a BufferedReader to read the input stream
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            // Read the input stream line by line
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            // Close the BufferedReader
            in.close();
            
            // Use Jackson to parse the JSON response into List<Madde>
            ObjectMapper objectMapper = new ObjectMapper();
            if (response.toString().contains("error")) {
            	return null;
            } else {
                return objectMapper.readValue(response.toString(), new TypeReference<List<Madde>>() {});
            }

        } else {
            throw new Exception("Failed to fetch JSON: HTTP error code : " + responseCode);
        }
    }

    public static void main(String[] args) {
        try {
            // Fetch and parse the JSON
            List<Madde> maddeList = fetchJsonFromTDK("alan");
            
            // Print the parsed data
            for (Madde madde : maddeList) {
                System.out.println("Madde ID: " + madde.getMadde_id());
                System.out.println("Madde: " + madde.getMadde());
                System.out.println("Anlamlar:");
                for (Anlam anlam : madde.getAnlamlarListe()) {
                    System.out.println("  - " + anlam.getAnlam());
                }
                System.out.println("-----------------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

