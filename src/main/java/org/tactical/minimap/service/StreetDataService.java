package org.tactical.minimap.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tactical.minimap.DAO.StreetDataDAO;
import org.tactical.minimap.repository.StreetData;
import org.tactical.minimap.util.MarkerGeoCoding;

@Service
public class StreetDataService {
	@Autowired
	StreetDataDAO streetDataDAO;

	@Value("${MAP_FOLDER}")
	String mapFolder;

	Pattern pattern = Pattern.compile("^(.*)\\[(.*),(.*)\\];\\[(.*),(.*):(.*),(.*)\\] .*$");

	@Transactional
	public void save(StreetData streetData) {
		streetDataDAO.save(streetData);
	}

	public StreetData findStreetData(String key) {
		List<StreetData> sdList = streetDataDAO.findStreetDataByName(key);

		if (sdList.size() > 0) {
			return sdList.get(0);
		}

		return null;
	}

	public List<StreetData> findStreetDataList(String streetType, String key) {
		return streetDataDAO.findStreetDataByStreetTypeAndName(streetType, key);
	}

	public String getRegionByLatlng(MarkerGeoCoding latlng) {
		String region = null;

		if (latlng != null) {
			File file = new File(mapFolder + "/pattern_data/v2/region_stat");

			try {
				Scanner scanner = new Scanner(file);

				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						String index = matcher.group(1);
						double nwLat = Double.parseDouble(matcher.group(4));
						double nwLng = Double.parseDouble(matcher.group(5));

						double seLat = Double.parseDouble(matcher.group(6));
						double seLng = Double.parseDouble(matcher.group(7));

						if (seLat < latlng.getLat() && latlng.getLat() < nwLat && nwLng < latlng.getLng() && latlng.getLng() < seLng) {
							region = index;
						}

					}

				}
				scanner.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return region;
	}

	public MarkerGeoCoding getLatlngByRegion(String region) throws FileNotFoundException {
		MarkerGeoCoding latlng = null;

		File file = new File(mapFolder + "/pattern_data/v2/region_stat");
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					String index = matcher.group(1);
					double lat = Double.parseDouble(matcher.group(2));
					double lng = Double.parseDouble(matcher.group(3));

					if (index.equals(region)) {
						latlng = MarkerGeoCoding.latlng(lat, lng);
					}

				}

			}
			scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return latlng;
	}

	public List<MarkerGeoCoding> getStatRegionList() {
		List<MarkerGeoCoding> latlngList = new LinkedList<MarkerGeoCoding>();

		File file = new File(mapFolder + "/pattern_data/v2/region_stat");

		try {
			Scanner scanner = new Scanner(file);

			while (scanner.hasNextLine()) {

				String line = scanner.nextLine();
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					String index = matcher.group(1);
					double lat = Double.parseDouble(matcher.group(2));
					double lng = Double.parseDouble(matcher.group(3));

					MarkerGeoCoding latlng = MarkerGeoCoding.latlng(lat, lng);
					latlng.setLabel(index);

					latlngList.add(latlng);
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return latlngList;
	}
}
