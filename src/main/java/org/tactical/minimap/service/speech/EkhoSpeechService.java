package org.tactical.minimap.service.speech;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

@Component
@Qualifier("EkhoSpeechService")
public class EkhoSpeechService extends SpeechService {
	String url = "http://120.24.87.124/cgi-bin/ekho2.pl";

	public String getSpeech(String text) {
		String response = null;

		HttpResponse<byte[]> byteResponse = Unirest.get(url)
				.queryString("cmd", "SPEAK")
				.queryString("voice", "iflytekXiaomei")
				.queryString("speedDelta", "-10")
				.queryString("text", text)
				.asBytes();
		
		if (byteResponse.getStatus() == 200) {
			response = Base64.getEncoder().encodeToString(byteResponse.getBody());
		}

		return response;
	};

}
