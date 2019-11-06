package org.tactical.minimap.service.speech;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

@Component
@Qualifier("LocalSpeechService")
public class LocalSpeechService extends SpeechService {
	String url = "http://192.168.0.100:8999/";

	public String getSpeech(String text) {
		String response = null;

		HttpResponse<byte[]> byteResponse = Unirest.get(url)
				.queryString("text", text)
				.asBytes();
		
		if (byteResponse.getStatus() == 200) {
			response = Base64.getEncoder().encodeToString(byteResponse.getBody());
		}

		return response;
	};

}
