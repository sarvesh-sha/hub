package com.optio3.cloud.hub.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.optio3.cloud.hub.api.DeviceData.DataEntry;
import com.optio3.cloud.hub.api.DeviceData.DeviceToServerData;
import com.optio3.cloud.hub.api.DeviceData.ReportData;
import com.optio3.cloud.hub.api.reportData.PowerData;

public class JsonSplitter {
	static long ts2 = 1714236593l;
	

	private static void iterateJsonNode(JsonNode node, String parentKey, Long ts) {
		boolean isB = false;
		if (node.isObject()) {

			// Iterate over object fields
			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				String fieldKey = parentKey.isEmpty() ? field.getKey() : parentKey + "." + field.getKey();
				if ("ts".equals(field.getKey())) {
					//ts = field.getValue().asLong();
				//	System.out.println("adding " +ts);
					long ts1 = ts + 25000l;
					ts2 = ts2 + 1000l;
					String s = String.valueOf(ts);

				//	System.out.println("adding 1 " +ts);
					((ObjectNode) node).put(field.getKey(), ts2);
				}
				if ("d".equals(field.getKey())) {
					isB = true;
					if (field.getValue().asLong() == 0) {
						// field.setValue()
						// ((ObjectNode) node).put(field.getKey(), 22);
						// System.out.println(field.getKey());
					}
				} else {
					isB = false;
				}

				iterateJsonNode(field.getValue(), fieldKey, ts);
			}
		} else if (node.isArray()) {
			// Iterate over array elements
			// if(isB)
			{
				for (int i = 0; i < node.size(); i++) {
					String arrayKey = parentKey + "[" + i + "]";
					// System.out.println(node.get(i).asInt());

					if (node.get(i).asLong() == 0) {
						if (parentKey.contains("d")) {
							// System.out.println( parentKey);
						//	System.out.println(" value " + node.get(i).asLong() + " " + node.get(i).asLong());
							node.get(i).asLong(20);

						}
					}
					if (isB) {
						// System.out.println(node.get(i).asInt());
					}
					iterateJsonNode(node.get(i), arrayKey, ts);
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			// Read the JSON file into a list of maps
			long ts =20l;
			int ccc = 0;
			List<String> latLongList = readFile();
			ObjectMapper mapper = new ObjectMapper();
			List<DeviceToServerData> dataObjects = mapper.readValue(
					new File("C:\\montage\\New_query_2024_06_24.csv"),
					new TypeReference<List<DeviceToServerData>>() {
					});
	/*		for (int i = 0; i < dataObjects.size(); i++) {
				if (dataObjects.get(i).trhData != null) {

					if (dataObjects.get(i).trhData.d.get(0).get(0) == 0) {
					//	System.out.println("ffff" + dataObjects.get(i).trhData.d.get(0).get(0));
						if (i % 2 == 0)
							dataObjects.get(i).trhData.d.get(0).set(0, 20);
						else
							dataObjects.get(i).trhData.d.get(0).set(0, -30);
					}

				}

				if (dataObjects.get(i).trhData != null) {

					if (dataObjects.get(i).trhData.d.get(0).get(1) == 0) {
						// System.out.println("ffff" + dataObjects.get(i).trhData.d.get(0).get(0));
						if (i % 2 == 0)
							dataObjects.get(i).trhData.d.get(0).set(1, 20);
						else
							dataObjects.get(i).trhData.d.get(0).set(1, 30);
					}

				}

				if (dataObjects.get(i).trhData != null) {

					if (dataObjects.get(i).trhData.d.get(0).get(1) == 0) {
						// System.out.println("ffff" + dataObjects.get(i).trhData.d.get(0).get(0));
						if (i % 2 == 0)
							dataObjects.get(i).trhData.d.get(0).set(2, 20);
						else
							dataObjects.get(i).trhData.d.get(0).set(2, 30);
					}

				}
			}

			for (int i = 0; i < dataObjects.size(); i++) {
				dataObjects.get(i).powerData = new DataEntry();
				dataObjects.get(i).powerData.d =  new ArrayList<List<Integer>>();
				
				dataObjects.get(i).powerData.d.add(new ArrayList<Integer>());
				dataObjects.get(i).powerData.d.set(0, new ArrayList<Integer>());
				dataObjects.get(i).powerData.d.get(0).add(0);
				dataObjects.get(i).powerData.d.get(0).add(0);
				dataObjects.get(i).powerData.d.get(0).add(0);
				if (dataObjects.get(i).powerData != null) {
					
					if (dataObjects.get(i).powerData.d.get(0).get(0) == 0) {
					//	System.out.println("ffff" + dataObjects.get(i).powerData.d.get(0).get(0));
						if (i % 2 == 0)
							dataObjects.get(i).powerData.d.get(0).set(0, 20);
						else
							dataObjects.get(i).powerData.d.get(0).set(0, -30);
					}

				}

				if (dataObjects.get(i).powerData != null) {

					if (dataObjects.get(i).powerData.d.get(0).get(1) == 0) {
						// System.out.println("ffff" + dataObjects.get(i).powerData.d.get(0).get(0));
						if (i % 2 == 0)
							dataObjects.get(i).powerData.d.get(0).set(1, 20);
						else
							dataObjects.get(i).powerData.d.get(0).set(1, 30);
					}

				}

				if (dataObjects.get(i).powerData != null) {

					if (dataObjects.get(i).powerData.d.get(0).get(2) == 0) {
						// System.out.println("ffff" + dataObjects.get(i).powerData.d.get(0).get(0));
						if (i % 2 == 0)
							dataObjects.get(i).powerData.d.get(0).set(2, 20);
						else
							dataObjects.get(i).powerData.d.get(0).set(2, 30);
					}

				}
			}

			for (int i = 0; i < dataObjects.size(); i++) {
				if (dataObjects.get(i).gnssData != null) {

					if (ccc > 98) {
						ccc = 0;
					}
					ccc++;
					String[] latLong = latLongList.get(ccc).split(",");

					if (dataObjects.get(i).gnssData.d.get(0).get(0) == 0) {
					//	System.out.println("ffff" + dataObjects.get(i).gnssData.d.get(0).get(0));
						if (i % 2 == 0)
							dataObjects.get(i).gnssData.d.get(0).set(0, 20.00);
						else
							dataObjects.get(i).gnssData.d.get(0).set(0, -30.00);
					}

//					if (dataObjects.get(i).gnssData.d.get(0).get(1) == 0)
{
						// System.out.println("ffff" + dataObjects.get(i).gnssData.d.get(0).get(0));

						dataObjects.get(i).gnssData.d.get(0).set(1, Double.valueOf(latLong[0]));
					}

					//if (dataObjects.get(i).gnssData.d.get(0).get(1) == 0) 
					{
						// System.out.println("ffff" + dataObjects.get(i).gnssData.d.get(0).get(0));

						dataObjects.get(i).gnssData.d.get(0).set(2, Double.valueOf(latLong[1]));

					}

				}
			}*/
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.setSerializationInclusion(Include.NON_NULL);
			String jsonOutput = mapper.writeValueAsString(dataObjects);
			writeFile(jsonOutput,"C:\\montage\\outFile");
		//	System.out.println(jsonOutput);
			//if(true)
			//	return;
		//	System.out.println(jsonOutput);
			int counter = 0;
			int fileIndex = 0;

			JsonNode rootNode = mapper.readTree(new File("C:\\montage\\outFile"));
		//	iterateJsonNode(rootNode, "",ts);
			String modifiedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
			// over the root node  
			mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
			System.out.println(modifiedJson);

			List<Map<String, Object>> jsonArray = mapper.readValue(modifiedJson,
					new TypeReference<List<Map<String, Object>>>() {
					});

			// Process the JSON objects int counter = 0; int fileIndex = 0;
			List<Map<String, Object>> chunk = new ArrayList<>();

			for (Map<String, Object> jsonObject : jsonArray) {

				counter++;
				if (jsonObject.containsKey("gateway")) {
					List<Object> gateway = (List<Object>) jsonObject.get("gateway");

					if (gateway.size() > 4) { // System.out.println(gateway.get(4));
						if (gateway.get(4).toString().startsWith("1234")) {
							System.out.println("continue");
							continue;
						}

					}
				}
				if(jsonObject.containsKey("trh"))

					{chunk.add(jsonObject);
					
					List<Object> gateway = (List<Object>) jsonObject.get("gateway");
					} // When the chunk reaches 10 elements, write it to a new file
				if (counter == 200) {
					writeChunkToFile(mapper, chunk, fileIndex , "gateways");
					String jsonOutput1 = mapper.writeValueAsString(chunk); //
					
					System.out.println(jsonOutput1);
					System.out.println("**************************************");
					//sendData(jsonOutput1);
					chunk.clear();
					
					counter = 0;
					fileIndex++;
				}
			}

			// Write any remaining elements to a new file
			if (!chunk.isEmpty()) {
				writeChunkToFile(mapper, chunk, fileIndex , "gateways");
				// String jsonOutput =
				String jsonOutput1  = mapper.writeValueAsString(chunk); // System.out.println(jsonOutput); //
				System.out.println("^^^^^^^^^^^^^^^^^^^");
		//		sendData(jsonOutput1);
			}

			System.out.println("Chunks written to files.");

		} catch (

		IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendData(String jsonInputString) {
		try {

			// System.out.println("jsonInputString" + jsonInputString);
			URL url = new URL(
					"https://hub.montage-connect.com/api/v1/data-ingest/simulated-gateway/createDeviceElementWithArray");

			// Open connection
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Cookie",
					"sessionToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmlsLmtrQG9wdGlvMy5jb20iLCJwc3QiOnRydWUsImV4cCI6MTcxOTg0MjA2M30.2bmFOAuSNRMtbfxzCqhaXZgMcxI8gPlkCSus43k54LM");
			conn.setDoOutput(true);

			// Define the request body
			/*
			 * String jsonInputString = "[\r\n" + "  {\r\n" +
			 * "    \"gateway\": [1, 164, 2306151, 536952507, \"0000ED7C2160002\"],\r\n" +
			 * "    \"gnssData\": {\r\n" + "      \"pID\": 3,\r\n" +
			 * "      \"ts\": 1652939234,\r\n" + "      \"d\": [\r\n" + "        [\r\n" +
			 * "          0,\r\n" + "          33.730442,\r\n" +
			 * "          -117.998497,\r\n" + "          0.402,\r\n" + "          0,\r\n" +
			 * "          14,\r\n" + "          1,\r\n" + "          0\r\n" +
			 * "        ]\r\n" + "      ]\r\n" + "    }\r\n" + "  }]";
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

		} catch (

		Exception e) {
			e.printStackTrace();
		}
	}

	private static void writeChunkToFile(ObjectMapper mapper, List<Map<String, Object>> chunk, int fileIndex, String fileName)
			throws IOException {
		File outputFile = new File("C:\\montage\\" + fileName + "_"+fileIndex + ".json");
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(outputFile, chunk);
	//	System.out.println("Chunk written to file: " + outputFile.getAbsolutePath());
	}

	public static List<String> readFile() {
		String filePath = "c:\\montage\\latlong.txt"; // Replace with the path to your file
		List<String> latLongList = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				latLongList.add(line);
				//System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return latLongList;
	}
	
	public static void writeFile(String str , String fileName) 
			  throws IOException {
			//    String str = "Hello";
			    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
			    writer.write(str);
			    
			    writer.close();
			}
}