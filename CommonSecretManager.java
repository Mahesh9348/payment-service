package com.fplws.services.payment.secret;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fplws.common.aws.awssecrets.SecretsHelper;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;


public class CommonSecretManager {

	static Logger logger = LogFactory.getLogger(CommonSecretManager.class);
	private final String className = this.getClass().getName();
	public CommonSecretManager() {
	}

	public Map<String, String> loadSecret(String secretName) {
		final String methodName = "loadSecret";
		try {
			return convertToSecretValue(SecretsHelper.getSecret(secretName));
		} catch (Exception e) {
			logger.error("{} | {} | Error in loading secret for secretName=[{}], Exception=[{}]", className, methodName,
					secretName, e.getMessage());
			throw new ApiServiceException("Error in loading secret for secretName=" + secretName);
		}
	}

	private Map<String, String> convertToSecretValue(String secretText) throws IOException {
		final String methodName = "convertToSecretValue";
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> response = new HashMap<>();
		if (!StringUtils.isEmpty(secretText)) {
			JsonNode jsonNode = objectMapper.readTree(secretText);
			jsonNode.fields().forEachRemaining(stringJsonNodeEntry -> response.put(stringJsonNodeEntry.getKey(),
					stringJsonNodeEntry.getValue().asText()));
			logger.info("{} | {} | DocdbSecret load success", className, methodName);
		}
		return response;
	}

}