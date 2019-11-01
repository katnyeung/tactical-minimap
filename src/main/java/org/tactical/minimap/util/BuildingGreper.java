package org.tactical.minimap.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class BuildingGreper {
	Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@Test
	public void test() throws IOException, InterruptedException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
	
		HashMap<String, Integer> index = new HashMap<String, Integer>();
		index.put("prh", 18);
		index.put("hos", 18);
		index.put("shop", 18);
		index.put("fact", 13);

		Unirest.config().verifySsl(false);

		PrintWriter pw = new PrintWriter("e:/estate.building");
		for (String category : index.keySet()) {
			for (int z = 1; z <= index.get(category); z++) {
				String url = "https://www.housingauthority.gov.hk/json/property-location/detail/" + category + "/" + z + ".json";
				HttpResponse<JsonNode> jsonResponse = Unirest.get(url).asJson();
				logger.info("URL : {} , status : {}", url, jsonResponse.getStatus());
				// logger.info("{}", jsonResponse.getBody().toPrettyString());
				JSONArray estateArray = jsonResponse.getBody().getArray();

				for (int i = 0; i < estateArray.length(); i++) {
					JSONObject estate = estateArray.getJSONObject(i);

					// logger.info("{}", estate.getJSONObject("blockName").getString("zh-Hant"));
					// logger.info("{}", estate.getJSONObject("name").getString("zh-Hant"));

					pw.println(estate.getJSONObject("name").getString("zh-Hant"));

					if (!estate.isNull("blockName")) {
						String building = estate.getJSONObject("blockName").getString("zh-Hant");
						String[] buildingList = building.split("<br>");

						for (int j = 0; j < buildingList.length; j++) {
							pw.println(buildingList[j]);
						}
					}
				}
			}
		}

		pw.close();
	}

	public void greper() throws IOException {
		logger.info("-- making connection");
		ArrayList<String> buildingList = new ArrayList<String>();

		HttpResponse<String> response = Unirest.post("http://bmis2.buildingmgt.gov.hk/bd_hadbiex/content/searchbuilding/building_search.jsf;jsessionid=u3lGvW0FRimIfLZi-8wP_WF7Pt4GycVIG--WPzjO.node2").field("_bld_result_frm:_result_tbl_first", "950").field("_bld_result_frm:_result_tbl_rows", "10")
				.field("_bld_result_frm_SUBMIT", "1").field("_bld_result_frm:_result_tbl_pagination", "true").field("_bld_result_frm:_result_tbl_encodeFeature", "true").field("javax.faces.partial.ajax", "true").field("javax.faces.source", "_bld_result_frm:_result_tbl")
				.field("javax.faces.partial.execute", "_bld_result_frm:_result_tbl").field("javax.faces.partial.render", "_bld_result_frm:_result_tbl").field("javax.faces.behavior.event", "page").field("javax.faces.partial.event", "page")
				.field("javax.faces.ViewState", "MGjsMyqs3wyi3XzgMsWpchm067BzY4HuMOT7P61jOldAmdT9").asString();

		logger.info("--{}", response.getBody().toString());
		Pattern pattern = Pattern.compile("onclick=\"PrimeFaces\\.addSubmitParam\\('_bld_result_frm',\\{'_bld_result_frm:_result_tbl:[0-9]*:j_id_54':'_bld_result_frm:_result_tbl:([0-9]{1,5}):j_id_54'\\}\\)\\.submit\\('_bld_result_frm'\\);return false;\">(.*?)<\\/a>");
		Matcher matcher = pattern.matcher(response.getBody());

		while (matcher.find()) {
			// logger.info("--{} " , matcher.group(1));
			// logger.info("--{} " , matcher.group(2));
			buildingList.add(matcher.group(1) + "\t\t" + matcher.group(2));
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter("e:/result.txt"));
		for (String building : buildingList) {
			writer.write(building + "\n");
		}
		writer.close();
	}

}
