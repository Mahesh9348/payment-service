package com.fplws.services.payment.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fplws.common.framework.fplcommonframework.beans.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fpl.common.data.PaymentRequestType;
import com.fpl.common.data.PaymentTransactionType;
import com.fpl.common.data.ResponseMessage;
import com.fpl.common.messages.UserMessageCodes;
import com.fpl.webservices.data.transaction.PaymentRequest;
import com.fplws.common.framework.fplcommonframework.constant.Constants;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.services.payment.service.PaymentService;
import com.fplws.services.payment.utility.ExceptionMessageHelper;

import static com.fplws.services.payment.constants.Constants.PARENT_ID;

/**
 * @author nxm01hn Rest Service to Create, Update, Delete or Retrieve Scheduled
 *         Payment details for account number provided mediaChannel = value
 *         (Andriod or iOS)-----> Call from mobile devices mediaChannel = null
 *         -----> Call from web
 *
 */

@RestController
@CrossOrigin
@RequestMapping("/resources/account/{" + PARENT_ID + "}/payment")
public class PaymentServiceController {

	private static Logger logger = LogFactory.getLogger(PaymentServiceController.class);

	@Autowired
	PaymentService paymentService;

	@Autowired
	ExceptionMessageHelper exceptionHelper;

	// Get Scheduled Payment details for Mobile
	@SuppressWarnings("rawtypes")
	@GetMapping(value = { "{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse getScheduledPayment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String transactionType,
			@RequestParam(name = "mediaChannel", required = false) String mediaChannel) {

		logger.info("getScheduledPayment_start | PaymentServiceController | getScheduledPayment Start | Account number - " + accountNumber
				+ " Payment Transaction Type - " + transactionType);

		try {
			if (PaymentTransactionType.SCHEDULED.name().equalsIgnoreCase(transactionType)) {
				return paymentService.getScheduledPayment(accountNumber);
			} else {
				throw new ApiServiceException(UserMessageCodes.PAYMENT_PAYMENT_DATA_NOT_FOUND, "Payment Not Found",
						ResponseHttpStatus.NOT_FOUND);
			}
		} catch (ApiServiceException exp) {
			logger.error(
					"getScheduledPayment_error | PaymentServiceController | getScheduledPayment | PaymentServiceException for " + accountNumber);
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.PAYMENTERRORMESSAGE);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel,responseMessages);
		}

	}

	@SuppressWarnings("rawtypes")
	@PostMapping(value = { "{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse updateScheduledPayment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String transactionType,
			@RequestBody PaymentRequest data,
			@RequestParam(name = "mediaChannel", required = false) String mediaChannel) {

		UserInfo userInfo = (UserInfo) request.getAttribute(Constants.REQUEST_USERINFO);

		if (mediaChannel != null)
			logger.info("updateScheduledPayment_start | PaymentServiceController | Mobile updateScheduledPayment Start for | Account number - "
					+ accountNumber + " Payment Transaction Type - " + transactionType + " mediaChannel - "
					+ mediaChannel + " User - " + userInfo.getPreferred_username() + " PaymentRequest data - " + data);
		else
			logger.info("updateScheduledPayment_start | PaymentServiceController | Web updateScheduledPayment Start for | Account number - "
					+ accountNumber + " Payment Transaction Type - " + transactionType + " mediaChannel - "
					+ mediaChannel + " User - " + userInfo.getPreferred_username() + " PaymentRequest data - " + data);

		try {
			if (PaymentTransactionType.SCHEDULED.name().equalsIgnoreCase(transactionType)) {
				PaymentRequest paymentRequest = data;
				paymentRequest.setRequestType(PaymentRequestType.UpdatePaymentRequest);
				setRequestedBypayment(paymentRequest, mediaChannel, userInfo.getPreferred_username());

				if(null != mediaChannel && ("IOS".equalsIgnoreCase(mediaChannel) || "ANDRD".equalsIgnoreCase(mediaChannel))){
					paymentRequest.setChannel("APP");
				}
				return paymentService.updateScheduledPayment(accountNumber, paymentRequest, userInfo);
			} else {
				logger.error(
						"updateScheduledPayment_error | PaymentServiceController | updateScheduledPayment | PaymentTransactionType NOT SCHEDULED for "
								+ accountNumber);
				throw new ApiServiceException(UserMessageCodes.PAYMENT_PAYMENT_DATA_NOT_FOUND, "Payment Not Found",
						ResponseHttpStatus.NOT_FOUND);
			}
		} catch (ApiServiceException exp) {
			// TODO Auto-generated catch block
			logger.error(
					"updateScheduledPayment_error | PaymentServiceController | updateScheduledPayment | PaymentServiceException for " + accountNumber);
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.PAYMENTERRORMESSAGE);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel,responseMessages);
		}

	}

	@SuppressWarnings("rawtypes")
	@DeleteMapping(value = { "{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse deleteScheduledPayment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String transactionType,
			@RequestBody PaymentRequest data,
			@RequestParam(name = "mediaChannel", required = false) String mediaChannel) {

		UserInfo userInfo = (UserInfo) request.getAttribute(Constants.REQUEST_USERINFO);

		if (mediaChannel != null)
			logger.info("deleteScheduledPayment_start | PaymentServiceController | Mobile deleteScheduledPayment Start for | Account number - "
					+ accountNumber + " Payment Transaction Type - " + transactionType + " mediaChannel - "
					+ mediaChannel + " User - " + userInfo.getPreferred_username() + " PaymentRequest data - " + data);
		else
			logger.info("deleteScheduledPayment_start | PaymentServiceController | Web deleteScheduledPayment Start for | Account number - "
					+ accountNumber + " Payment Transaction Type - " + transactionType + " mediaChannel - "
					+ mediaChannel + " User - " + userInfo.getPreferred_username() + " PaymentRequest data - " + data);

		try {
			if (PaymentTransactionType.SCHEDULED.name().equalsIgnoreCase(transactionType)) {
				PaymentRequest paymentRequest = data;
				paymentRequest.setRequestType(PaymentRequestType.DeletePaymentRequest);
				setRequestedBypayment(paymentRequest, mediaChannel, userInfo.getPreferred_username());


				if(null != mediaChannel && ("IOS".equalsIgnoreCase(mediaChannel) || "ANDRD".equalsIgnoreCase(mediaChannel))){
					paymentRequest.setChannel("APP");
				}
				return paymentService.deleteScheduledPayment(accountNumber, paymentRequest, userInfo);
			} else {
				logger.error(
						"deleteScheduledPayment_error | PaymentServiceController | deleteScheduledPayment | PaymentTransactionType NOT SCHEDULED for "
								+ accountNumber);
				throw new ApiServiceException(UserMessageCodes.PAYMENT_PAYMENT_DATA_NOT_FOUND, "Payment Not Found",
						ResponseHttpStatus.NOT_FOUND);
			}
		} catch (ApiServiceException exp) {
			// TODO Auto-generated catch block
			logger.error(
					"deleteScheduledPayment_error | PaymentServiceController | deleteScheduledPayment | PaymentServiceException for " + accountNumber);
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.PAYMENTERRORMESSAGE);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel,responseMessages);
		}

	}

	@SuppressWarnings("rawtypes")
	@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse createScheduledPayment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @RequestBody PaymentRequest data,
			@RequestParam(name = "mediaChannel", required = false) String mediaChannel) throws ApiServiceException{

		UserInfo userInfo = (UserInfo) request.getAttribute(Constants.REQUEST_USERINFO);

		if (mediaChannel != null)
			logger.info("createScheduledPayment_start | PaymentServiceController | Mobile createScheduledPayment Start for | Account number - "
					+ accountNumber + " mediaChannel - " + mediaChannel + " User - " + userInfo.getPreferred_username()
					+ " PaymentRequest data - " + data);
		else
			logger.info("createScheduledPayment_start | PaymentServiceController | Web createScheduledPayment Start for | Account number - "
					+ accountNumber + " mediaChannel - " + mediaChannel + " User - " + userInfo.getPreferred_username()
					+ " PaymentRequest data - " + data);

		try {

			PaymentRequest paymentRequest = data;
			paymentRequest.setRequestType(PaymentRequestType.CreatePaymentRequest);
			setRequestedBypayment(paymentRequest, mediaChannel, userInfo.getPreferred_username());

			if(null != mediaChannel && ("IOS".equalsIgnoreCase(mediaChannel) || "ANDRD".equalsIgnoreCase(mediaChannel))){
				paymentRequest.setChannel("APP");
			}
			return paymentService.createScheduledPayment(accountNumber, paymentRequest, userInfo);

		} catch (ApiServiceException exp) {
			// TODO Auto-generated catch block
			logger.error(
					"createScheduledPayment_error | PaymentServiceController | createScheduledPayment | PaymentServiceException for " + accountNumber + " Error code : " + exp.getErrorCode());
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.PAYMENTERRORMESSAGE);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel,responseMessages);
		}

	}

	private void setRequestedBypayment(PaymentRequest paymentRequest, String mediaChannel, String userName) {
		// TODO Auto-generated method stub
		String requestedBy = "";
		if (mediaChannel != null) {
			requestedBy = mediaChannel + "_" + userName;
		} else {
			requestedBy = userName;
		}

		if (requestedBy != null && requestedBy.length() > 25) {
			requestedBy = requestedBy.substring(0, 25);
		}
		paymentRequest.setRequestedBy(requestedBy);
	}

}
