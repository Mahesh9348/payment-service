package com.fplws.services.payment.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fpl.common.logger.FPLComLoggerUtil;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.services.payment.utility.MobileMessageHelper;


@RestController
public class LoadPropertiesController{

	private static final FPLComLoggerUtil logger= FPLComLoggerUtil.getLogger(LoadPropertiesController.class);

	@SuppressWarnings("rawtypes")
	@PostMapping(value = "/public/load" , 
			produces=MediaType.APPLICATION_JSON_VALUE, 
			consumes =MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse getMsgPost() {

		MobileMessageHelper.load();
		
		logger.logInfo("LoadPropertiesService getMsgPost Success");

		return	FPLWSResponse.assemble().build(ResponseHttpStatus.OK);
	}	

}
