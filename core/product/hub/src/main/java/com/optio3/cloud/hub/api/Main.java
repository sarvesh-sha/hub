package com.optio3.cloud.hub.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.SendDataRecordPayload;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

public class Main {

	public static void main2(String[] args) {

		try {
			// Read the JSON file into a list of maps
			ObjectMapper mapper = new ObjectMapper();
			List<Map<String, Object>> jsonArray = mapper.readValue(new File("C:\\montage\\444.json"),
					new TypeReference<List<Map<String, Object>>>() {
					});

			// Extract the "gateway" nodes
			int i =0;
			List<List<Object>> gateways = new ArrayList<>();
			for (Map<String, Object> jsonObject : jsonArray) {
				 System.out.println("Element: " + jsonObject);
				if (jsonObject.containsKey("gateway")) {
					/*List<Object> gateway = (List<Object>) jsonObject.get("gateway");
					gateways.add(gateway);
					*/
					 String jsonOutput = mapper.writeValueAsString(jsonObject.get("gateway"));
					// sendData(jsonObject);
					 if(i==3)
						 break;
					 
				}
			}

			// Write the gateways to a file
			File outputFile = new File("gatewaysSegment.json");
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.writeValue(outputFile, gateways);

			System.out.println("Gateways written to file: " + outputFile.getAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();

		}

		// Define the URL

	}

	public static void sendData(String jsonInputString)
    {try {
    	
    	 System.out.println("jsonInputString" + jsonInputString);
    	  URL url = new URL("https://hub.montage-connect.com/api/v1/data-ingest/simulated-gateway/createDeviceElementWithArray");

          // Open connection
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("POST");
          conn.setRequestProperty("Accept", "application/json");
          conn.setRequestProperty("Content-Type", "application/json");
          conn.setRequestProperty("Cookie", "sessionToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmlsLmtrQG9wdGlvMy5jb20iLCJwc3QiOnRydWUsImV4cCI6MTcxOTg0MjA2M30.2bmFOAuSNRMtbfxzCqhaXZgMcxI8gPlkCSus43k54LM");
          conn.setDoOutput(true);

          // Define the request body
          /*String jsonInputString = "[\r\n"
          		+ "  {\r\n"
          		+ "    \"gateway\": [1, 164, 2306151, 536952507, \"0000ED7C2160002\"],\r\n"
          		+ "    \"gnssData\": {\r\n"
          		+ "      \"pID\": 3,\r\n"
          		+ "      \"ts\": 1652939234,\r\n"
          		+ "      \"d\": [\r\n"
          		+ "        [\r\n"
          		+ "          0,\r\n"
          		+ "          33.730442,\r\n"
          		+ "          -117.998497,\r\n"
          		+ "          0.402,\r\n"
          		+ "          0,\r\n"
          		+ "          14,\r\n"
          		+ "          1,\r\n"
          		+ "          0\r\n"
          		+ "        ]\r\n"
          		+ "      ]\r\n"
          		+ "    }\r\n"
          		+ "  }]";
*/
          // Send the request body
          try (OutputStream os = conn.getOutputStream()) {
              byte[] input = jsonInputString.getBytes("utf-8");
              os.write(input, 0, input.length);
          }

          // Get the response code
          int responseCode = conn.getResponseCode();
          System.out.println("POST Response Code :: " + responseCode);
          System.out.println("POST Response Code :: " + conn.getContent().toString());

          // Handle the response (if needed)
          if (responseCode == HttpURLConnection.HTTP_OK) {
              System.out.println("POST request worked");
          } else {
              System.out.println("POST request did not work");
          }

      }catch(

	Exception e)
	{
		e.printStackTrace();
	}
	}

	public static void main(String[] args) {
		try {
			// Read JSON file into Java object
			List<GatewayData> gnsDataList = new ArrayList<GatewayData>();
			ObjectMapper mapper = new ObjectMapper();
			SegmentData segmentData = mapper.readValue(new File("c:\\montage\\segmentData1.json"), SegmentData.class);
			System.out.println(segmentData.segments.size());
			for (int n = 0; n < segmentData.segments.size(); n++) {
				// Extracting the first (and only) segment from the JSON
				List<Double> timestamps = segmentData.getSegments().get(n).getTimestamps();
				List<Double> longitudes = segmentData.getSegments().get(n).getLongitudes();
				List<Double> latitudes = segmentData.getSegments().get(n).getLatitudes();

				// Create a new node structure as specified
				
				int j =0;
				for (int i = 0; i < timestamps.size(); i++) {
					GatewayData gatewayData = new GatewayData();
					List<Object> gateway = new ArrayList<>();
					gateway.add(2);
					gateway.add(165);
					gateway.add(2306152);
					gateway.add(536952508);
					gateway.add("Test_Device_Map_11");
					gatewayData.setGateway(gateway);
					j++;
					GnssData gnssData = new GnssData();
					gnssData.setpID(3);
					// Assuming you want to add the first timestamp from the list to "ts"
					System.out.println(timestamps.get(i).longValue());
				//	Instant ins = Instant.ofEpochSecond(timestamps.get(i).longValue());
				//	System.out.println(ins);
					gnssData.setTs(timestamps.get(i).longValue());
					List<List<Double>> dd = new ArrayList<List<Double>>();
					List<Double> d = new ArrayList<>();
					d.add(0.0); // Placeholder for the first element
					d.add(latitudes.get(i)); // Latitude as the second element
					d.add(longitudes.get(i)); // Longitude as the third element
					d.add(0.402); // Example values for the rest of the elements
					d.add(0.0);
					d.add(14.0);
					d.add(1.0);

					dd.add(d);
					gnssData.setD(dd);

					// Assemble the final structure
					gatewayData.setGnssData(gnssData);
					gnsDataList.add(gatewayData);
					if(j >1000)
					{
						mapper.enable(SerializationFeature.INDENT_OUTPUT);
						String jsonOutput = mapper.writeValueAsString(gnsDataList);
						System.out.println(jsonOutput);
						sendData(jsonOutput);
						j=0;
						gnsDataList.clear();
					}

					
					// System.out.println(i);
				}

				String jsonOutput = mapper.writeValueAsString(gnsDataList);
				System.out.println(jsonOutput);
				sendData(jsonOutput);
				j=0;
				gnsDataList.clear();
			}
			System.out.println("$$$$$$$$$$$$$$$$$$");
			for(int j =0 ;j<gnsDataList.size() ;j++)
			{
				System.out.println(gnsDataList.get(j).getGnssData().getTs());
			}
			// Convert Java object to JSON and pretty print it
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Define the classes matching the JSON structure

	// Class representing the entire JSON structure
	static class SegmentData {
		@JsonProperty("segments")
		private List<Segment> segments;

		public List<Segment> getSegments() {
			return segments;
		}

		public void setSegments(List<Segment> segments) {
			this.segments = segments;
		}
	}

	// Class representing each segment in the JSON
	static class Segment {
		@JsonProperty("timestamps")
		private List<Double> timestamps;

		@JsonProperty("longitudes")
		private List<Double> longitudes;

		@JsonProperty("latitudes")
		private List<Double> latitudes;

		public List<Double> getTimestamps() {
			return timestamps;
		}

		public void setTimestamps(List<Double> timestamps) {
			this.timestamps = timestamps;
		}

		public List<Double> getLongitudes() {
			return longitudes;
		}

		public void setLongitudes(List<Double> longitudes) {
			this.longitudes = longitudes;
		}

		public List<Double> getLatitudes() {
			return latitudes;
		}

		public void setLatitudes(List<Double> latitudes) {
			this.latitudes = latitudes;
		}
	}

	// Class representing the "gateway" and "gnssData" structure
	static class GatewayData {
		@JsonProperty("gateway")
		private List<Object> gateway;

		@JsonProperty("gnssData")
		private GnssData gnssData;

		public List<Object> getGateway() {
			return gateway;
		}

		public void setGateway(List<Object> gateway) {
			this.gateway = gateway;
		}

		public GnssData getGnssData() {
			return gnssData;
		}

		public void setGnssData(GnssData gnssData) {
			this.gnssData = gnssData;
		}
	}

	// Class representing the "gnssData" part of the JSON structure
	static class GnssData {
		@JsonProperty("pID")
		private int pID;

		@JsonProperty("ts")
		private long ts;

		@JsonProperty("d")
		private List<List<Double>> d;

		public int getpID() {
			return pID;
		}

		public void setpID(int pID) {
			this.pID = pID;
		}

		public long getTs() {
			return ts;
		}

		public void setTs(long ts) {
			this.ts = ts;
		}

		public List<List<Double>> getD() {
			return d;
		}

		public void setD(List<List<Double>> d) {
			this.d = d;
		}
	}
}
