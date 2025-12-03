package com.fplws.services.payment.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fplws.common.framework.fplcommonframework.beans.UserInfo;
import com.fplws.services.payment.utility.NachaHelper;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.fpl.common.data.BaseDataConverter;
import com.fpl.common.data.ResponseMessage;
import com.fpl.common.util.StringUtil;
import com.fpl.webservices.data.AccountProgramName;
import com.fpl.webservices.data.transaction.PayOnlineEnrollmentRequest;
import com.fpl.webservices.data.transaction.ProgramEnrollmentRequest;
import com.fplws.common.framework.fplcommonframework.constant.Constants;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.services.payment.service.ProgramEnrollmentService;
import com.fplws.services.payment.utility.ExceptionMessageHelper;

/**
 * @author nxm01hn Rest Service to Create, Update, Delete or Retrieve Enrollment details 
 *         for account number provided
 *         mediaChannel = value (ANdriod or iOS)-----> Call from mobile devices
 *         mediaChannel = null -----> Call from web 
 *
 */

@RestController
@CrossOrigin
@RequestMapping("/resources/account/{parentId}/programEnrollment")
public class ProgramEnrollmentServiceController {

	private static Logger logger = LogFactory.getLogger(ProgramEnrollmentServiceController.class);

	@Autowired
	ProgramEnrollmentService programEnrollment;

	@Autowired
	ExceptionMessageHelper exceptionHelper;

	@SuppressWarnings("rawtypes")
	@PutMapping(value = { "{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse createEnrollment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String enrollmentType,
			@RequestBody HashMap<String, Object> map, @RequestParam(required = false) String mediaChannel) {

		if(mediaChannel!=null)
			logger.info("createEnrollment_start | ProgramEnrollmentService | ProgramEnrollmentServiceController | Mobile createEnrollment Start | Account number - " + accountNumber
				+ " Enrollment Type - " + enrollmentType + " Data " + map);
		else
			logger.info("createEnrollment_start | ProgramEnrollmentService | ProgramEnrollmentServiceController | Web createEnrollment Start | Account number - " + accountNumber
					+ " Enrollment Type - " + enrollmentType + " Data " + map);
		UserInfo userInfo = (UserInfo) request.getAttribute(Constants.REQUEST_USERINFO);

		try {
			logger.info("createEnrollment_start | ProgramEnrollmentService | ProgramEnrollmentServiceController | createEnrollment |" +
					" creating ProgramEnrollmentRequest from request.");
			ProgramEnrollmentRequest<?> programEnrollmentRequest = mapUpdate(map, enrollmentType);
			programEnrollmentRequest.setAccountNumber(StringUtil.formatBANumber(accountNumber));
			setRequestedBypayment(programEnrollmentRequest, mediaChannel, userInfo.getPreferred_username());

			return programEnrollment.createEnrollment(programEnrollmentRequest, mediaChannel, userInfo);

		} catch (ApiServiceException exp) {
			logger.error("createEnrollment_error | ProgramEnrollmentServiceController | createEnrollment | PaymentServiceException for "
					+ accountNumber);
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.ADDEDITMSG);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel,responseMessages);
		}

	}

	@SuppressWarnings("rawtypes")
	@GetMapping(value = { "{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse retrieveEnrollment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String enrollmentType,
			@RequestParam(required = false) String mediaChannel) {

		if(mediaChannel!=null)
			logger.info("retrieveEnrollment_start | ProgramEnrollmentServiceController | Mobile retrieveEnrollment Start | Account number - " + accountNumber
				+ " Enrollment Type - " + enrollmentType);
		else
			logger.info("retrieveEnrollment_start | ProgramEnrollmentServiceController | Web retrieveEnrollment Start | Account number - " + accountNumber
					+ " Enrollment Type - " + enrollmentType);

		try {

			return programEnrollment.retrieveEnrollment(StringUtil.formatBANumber(accountNumber));

		} catch (ApiServiceException exp) {
			logger.error("retrieveEnrollment_error | ProgramEnrollmentServiceController | retrieveEnrollment | PaymentServiceException for "
					+ accountNumber);
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.ADDEDITMSG);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel,responseMessages);
		}

	}
	
	
	@SuppressWarnings("rawtypes")
	@GetMapping(value = { "{id}/validate" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse validateEnrollment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String enrollmentType,
			@RequestParam(required = false) String mediaChannel) {

		if(mediaChannel!=null)
			logger.info("validateEnrollment_start | ProgramEnrollmentServiceController | Mobile validateEnrollment Start | Account number - " + accountNumber
				+ " Enrollment Type - " + enrollmentType);
		else
			logger.info("validateEnrollment_start | ProgramEnrollmentServiceController | Web validateEnrollment Start | Account number - " + accountNumber
					+ " Enrollment Type - " + enrollmentType);

		try {

			return programEnrollment.validateEnrollment(StringUtil.formatBANumber(accountNumber));

		} catch (ApiServiceException exp) {
				logger.error("retrieveEnrollment_error | ProgramEnrollmentServiceController | retrieveEnrollment | PaymentServiceException for "
						+ accountNumber);
				List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.ADDEDITMSG);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel, responseMessages);
		}

	}
	
	@SuppressWarnings("rawtypes")
	@PostMapping(value = { "{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse updateEnrollment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String enrollmentType,
			@RequestBody HashMap<String, Object> map, @RequestParam(required = false) String mediaChannel) {

		if(mediaChannel!=null)
			logger.info("updateEnrollment_start | ProgramEnrollmentServiceController | Mobile updateEnrollment Start | Account number - " + accountNumber
				+ " Enrollment Type - " + enrollmentType + " Data " + map);
		else
			logger.info("updateEnrollment_start | ProgramEnrollmentServiceController | Web updateEnrollment Start | Account number - " + accountNumber
					+ " Enrollment Type - " + enrollmentType + " Data " + map);
		UserInfo userInfo = (UserInfo) request.getAttribute(Constants.REQUEST_USERINFO);

		try {
			ProgramEnrollmentRequest<?> programEnrollmentRequest = mapUpdate(map, enrollmentType);
			programEnrollmentRequest.setAccountNumber(StringUtil.formatBANumber(accountNumber));
			setRequestedBypayment(programEnrollmentRequest, mediaChannel, userInfo.getPreferred_username());

			return programEnrollment.updateEnrollment(programEnrollmentRequest, mediaChannel, userInfo);

		} catch (ApiServiceException exp) {
			logger.error("updateEnrollment_error | ProgramEnrollmentServiceController | updateEnrollment | PaymentServiceException for "
					+ accountNumber);
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.ADDEDITMSG);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel, responseMessages);
		}

	}
	
	@SuppressWarnings("rawtypes")
	@DeleteMapping(value = { "{id}" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public FPLWSResponse deleteEnrollment(HttpServletRequest request, HttpServletResponse response,
			@PathVariable("parentId") String accountNumber, @PathVariable("id") String enrollmentType,
			@RequestBody HashMap<String, Object> map, @RequestParam(required = false) String mediaChannel) {

		if(mediaChannel!=null)
			logger.info("deleteEnrollment_start | ProgramEnrollmentServiceController | Mobile deleteEnrollment Start | Account number - " + accountNumber
				+ " Enrollment Type - " + enrollmentType + " Data " + map);
		else
			logger.info("deleteEnrollment_start | ProgramEnrollmentServiceController | Web deleteEnrollment Start | Account number - " + accountNumber
					+ " Enrollment Type - " + enrollmentType + " Data " + map);

		UserInfo userInfo = (UserInfo) request.getAttribute(Constants.REQUEST_USERINFO);

		try {
			ProgramEnrollmentRequest<?> programEnrollmentRequest = mapUpdate(map, enrollmentType);
			programEnrollmentRequest.setAccountNumber(StringUtil.formatBANumber(accountNumber));
			setRequestedBypayment(programEnrollmentRequest, mediaChannel, userInfo.getPreferred_username());

			return programEnrollment.deleteEnrollment(programEnrollmentRequest, userInfo);

		} catch (ApiServiceException exp) {
			logger.error("deleteEnrollment_error | ProgramEnrollmentServiceController | deleteEnrollment | PaymentServiceException for "
					+ accountNumber);
			List<ResponseMessage> responseMessages = exp.getResponseMessages();
			exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.ADDEDITMSG);
			return exceptionHelper.returnCustomResponse.apply(mediaChannel, responseMessages);
		}

	}

	protected ProgramEnrollmentRequest<?> mapUpdate(Map<String, Object> jsonData, String enrollmentType)
			throws ApiServiceException {
		logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceController | mapUpdate. enrollmentType=[{}]", enrollmentType);
		ProgramEnrollmentRequest<?> programEnrollmentRequest;
		try {
			programEnrollmentRequest = this.getRequestType(enrollmentType);
			BeanUtilsBean b = new BeanUtilsBean(BaseDataConverter.CONVERT);
			b.populate(programEnrollmentRequest, jsonData);
		} catch (Exception e) {
			logger.error("ProgramEnrollmentService | ProgramEnrollmentServiceController | mapUpdate failed with enrollmentType=[{}], Exception=[{}]", enrollmentType, Arrays.toString(e.getStackTrace()));
			throw new ApiServiceException(e);
		}
		return programEnrollmentRequest;
	}

	private ProgramEnrollmentRequest<?> getRequestType(String id) {
		AccountProgramName programName = AccountProgramName.fromLabel(id);
		logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceController | getRequestType. programName=[{}]", programName);
		ProgramEnrollmentRequest<?> programEnrollmentRequest = new PayOnlineEnrollmentRequest();
		logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceController | getRequestType. className=[{}]", programEnrollmentRequest);
		return programEnrollmentRequest;
	}

	private void setRequestedBypayment(ProgramEnrollmentRequest<?> programEnrollmentRequest, String mediaChannel,
			String userName) {
		logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceController | setRequestedBypayment |" +
				" setting mediaChannel=[{}], userName=[{}]", mediaChannel, userName);
		String requestedBy = "";
		if (mediaChannel != null) {
			requestedBy = mediaChannel + "_" + userName;
		} else {
			requestedBy = userName;
		}

		if (requestedBy != null && requestedBy.length() > 25) {
			requestedBy = requestedBy.substring(0, 25);
		}
		programEnrollmentRequest.setRequestedBy(requestedBy);
	}

}
