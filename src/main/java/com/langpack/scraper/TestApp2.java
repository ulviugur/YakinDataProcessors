package com.langpack.scraper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class TestApp2 {

	public static void main(String[] args) {

		final String app_id = "aa5105f5";
		final String app_key = "770836b84a7168190fbe91c2cb7cd3c0";
		try {
			URL url = new URL("https://od-api.oxforddictionaries.com/api/v2/entries/en-gb/paddle?fields=definitions");
			HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
			urlConnection.setRequestProperty("Accept", "application/json");
			urlConnection.setRequestProperty("app_id", app_id);
			urlConnection.setRequestProperty("app_key", app_key);

			// read the output from the server
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();

			String line = null;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}

			System.out.println(stringBuilder.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
