package org.tactical.minimap.service.speech;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

@Component
@Qualifier("AzureSpeechService")
public class AzureSpeechService extends SpeechService {
	String url = "https://southcentralus.api.cognitive.microsoft.com";

	public String getSpeech(String text) {
		String response = null;
		logger.info("prepare to post");
		HttpResponse<String> tokenResponse = null;
		try {
			tokenResponse = Unirest.post(url + "/sts/v1.0/issueToken")
				.header("Ocp-Apim-Subscription-Key", "03dd2743879543fa9a651c00a800be7a")				
				.asString();
		} catch (UnirestException ue) {
			ue.printStackTrace();
		}
		
		logger.info("making azure request {} ", tokenResponse);
		if (tokenResponse.getStatus() == 200) {
			String accessToken = tokenResponse.getBody();

			logger.info("response {}" , tokenResponse);
			String xmlRaw = "<speak version='1.0' xml:lang='zh-HK'><voice xml:lang='zh-HK' xml:gender='Female' name='zh-HK-Tracy-Apollo'>"+text+"</voice></speak>";

			HttpResponse<byte[]> byteResponse = Unirest.post("https://southcentralus.api.cognitive.microsoft.com/cognitiverservice/v1")
				.header("X-Microsoft-OutputFormat", "riff-24khz-16bit-mono-pcm")
				.header("Content-Type", "application/ssml+xml")
				.header("Host", "southcentralus.tts.speech.microsoft.com")
				.header("Authorization", "Bearer " + accessToken)
				.body(xmlRaw)
				.asBytes();

			if (byteResponse.getStatus() == 200) {
				logger.info("response {}" , byteResponse);
				response = Base64.getEncoder().encodeToString(byteResponse.getBody());
			}

		}

		return response;
	};

}
