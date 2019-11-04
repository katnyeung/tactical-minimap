package org.tactical.minimap.service.speech;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.xembly.Directives;
import org.xembly.ImpossibleModificationException;
import org.xembly.Xembler;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

@Component
@Qualifier("AzureSpeechService")
public class AzureSpeechService extends SpeechService {
	String url = "https://japanwest.api.cognitive.microsoft.com";

	public String getSpeech(String text) {
		String response = null;
		logger.info("prepare to post");
		HttpResponse<String> tokenResponse = null;
		try {
			tokenResponse = Unirest.post(url + "/sts/v1.0/issueToken")
				.header("Ocp-Apim-Subscription-Key", "07cbd33a37524f7e890e320013b312cc")				
				.asString();
		} catch (UnirestException ue) {
			ue.printStackTrace();
		}
		
		logger.info("making azure request {} ", tokenResponse);
		if (tokenResponse.getStatus() == 200) {
			String accessToken = tokenResponse.getBody();

			logger.info("response {}" , tokenResponse);
			try {
				String xml = new Xembler(
				    new Directives().add("speak")
				    	.attr("version", "1.0")
				    	.attr("xml:lang", "zh-HK")
				      .add("voice")
				      	.attr("xml:lang", "zh-HK")
				      	.attr("xml:gender", "Female")
				      	.attr("name", "zh-HK-Tracy-Apollo")
				      .set(text)
				  ).xml();
				
				HttpResponse<byte[]> byteResponse = Unirest.post("https://japanwest.tts.speech.microsoft.com/cognitiverservice/v1")
					.header("X-Microsoft-OutputFormat", "raw-16khz-16bit-mono-pcm")
					.header("Content-Type", "application/ssml+xml")
					.header("Host", "japanwest.tts.speech.microsoft.com")
					.header("Authorization", "Bearer " + accessToken)
					.body(xml)
					.asBytes();

				if (byteResponse.getStatus() == 200) {
					logger.info("response {}" , byteResponse);
					response = Base64.getEncoder().encodeToString(byteResponse.getBody());
				}
			} catch (ImpossibleModificationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return response;
	};

}
