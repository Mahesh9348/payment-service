package com.fplws.services.payment.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fpl.common.data.ResponseMessage;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.services.payment.utility.ExceptionMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.services.payment.service.PaymentOptionPostService;

import java.util.List;

import static com.fplws.services.payment.constants.Constants.PARENT_ID;

/**
 * @author nxm01hn Rest Service to retrieve payment option for Account number
 *         provided
 *
 */

@RestController
@CrossOrigin
@RequestMapping("/resources/account/{" + PARENT_ID + "}/payment-option")
public class PaymentOptionServiceController {

	private static Logger logger = LogFactory.getLogger(PaymentOptionServiceController.class);

	@Autowired
	PaymentOptionPostService paymentOptionService;

	@Autowired
	ExceptionMessageHelper exceptionHelper;

	@SuppressWarnings("rawtypes")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse paymentOptionServiceController(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber) throws ApiServiceException{

		logger.info("PaymentOption_get | paymentOptionServiceController Start | Account number - " + accountNumber);
		String mediaChannel = request.getParameter("mediaChannel");
		try {
			return paymentOptionService.getPaymentOption(accountNumber);
		} catch (ApiServiceException exp) {
			logger.error("PaymentOption_post | PaymentOptionServiceController | getPaymentOption | PaymentServiceException" +
					" for accountNumber=[{}], Exception=[{}]", accountNumber, (exp.getErrorCode() + " - " + exp.getMessage()));
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.PYOPTIONMSG);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel,responseMessages);
		}
	}

}
