package com.fplws.services.payment.serviceimpl;

import com.fplws.common.framework.fplcommonframework.beans.UserInfo;
import com.fplws.services.payment.utility.NachaHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fpl.common.data.BankInfoOption;
import com.fpl.common.data.MessageType;
import com.fpl.common.data.ResponseMessage;
import com.fpl.common.exception.DataAccessException;
import com.fpl.common.messages.UserMessageCodes;
import com.fpl.webservices.data.BankInfo;
import com.fpl.webservices.data.EmailAddress;
import com.fpl.webservices.data.transaction.PayOnlineEnrollmentRequest;
import com.fpl.webservices.data.transaction.PayOnlineEnrollmentResponse;
import com.fpl.webservices.data.transaction.PayOnlineRetrieveEnrollmentResponse;
import com.fpl.webservices.data.transaction.ProgramEnrollmentRequest;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.mf.payonlinek97a.model.JsonInputarealog;
import com.fplws.mf.payonlinek97a.model.OutputResponse;
import com.fplws.services.payment.dao.ProgramEnrollmentServiceDAO;
import com.fplws.services.payment.model.PayOnlineEnrollmentOptions;
import com.fplws.services.payment.service.ProgramEnrollmentService;
import com.fplws.services.payment.utility.BankingRoutingHelper;
import com.fplws.services.payment.utility.ExceptionMessageHelper;
import com.fplws.services.payment.utility.UpdateBlankInput;

/**
 * @author nxm01hn
 *
 */
@Service("programEnrollment")
public class ProgramEnrollmentServiceImpl implements ProgramEnrollmentService {

	@Autowired
	ExceptionMessageHelper exceptionHelper;

	@Autowired
	UpdateBlankInput updateBlankInput;

	@Autowired
	BankingRoutingHelper bankingRoutingHelper;

	@Autowired
	ProgramEnrollmentServiceDAO programEnrollmentServiceDAO;

    @Autowired
    NachaHelper nachaHelper;

	private static Logger logger = LogFactory.getLogger(ProgramEnrollmentServiceImpl.class);

	@SuppressWarnings("rawtypes")
	@Override
	public FPLWSResponse createEnrollment(ProgramEnrollmentRequest<?> programEnrollmentRequest, String mediaChannel, UserInfo userInfo)
			throws ApiServiceException {
		// TODO Auto-generated method stub
		String type = "CREATE";
		FPLWSResponse<PayOnlineEnrollmentResponse> response = null;
		logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceImpl | createEnrollment Start | Account number - "
				+ programEnrollmentRequest.getAccountNumber());

		try {
			PayOnlineEnrollmentRequest payOnlineEnrollmentRequest = (PayOnlineEnrollmentRequest) programEnrollmentRequest;
            //NACHA Bank account number validation-start
            nachaHelper.validateBankAccount(payOnlineEnrollmentRequest, mediaChannel);
            //NACHA Bank account number validation-end
			PayOnlineEnrollmentResponse payOnlineEnrollmentResponse = new PayOnlineEnrollmentResponse();

			if (payOnlineEnrollmentRequest.getEmailAddress() != null
					&& payOnlineEnrollmentRequest.getEmailAddress().getValue() != null
					&& (payOnlineEnrollmentRequest.getEmailAddress().getValue().equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT")
							|| payOnlineEnrollmentRequest.getEmailAddress().getValue()
									.equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT@default.com"))) {
				String emailAddressUpdated = userInfo.getEmail();
				logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceImpl | createEnrollment | emailAddressUpdated=[{}]", emailAddressUpdated);
				payOnlineEnrollmentResponse.setEmailAddresss(emailAddressUpdated);
			}

			// Server Side Routing Number Validation
			if (payOnlineEnrollmentRequest.getData().getBankInformation().getBankRoutingNumber() != null) {
				bankingRoutingHelper.validRoutingNumber(
						payOnlineEnrollmentRequest.getData().getBankInformation().getBankRoutingNumber());
			}

			JsonInputarealog json_inputarealog = new JsonInputarealog();
			json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

			processEnrollmenttInputRequest(json_inputarealog, payOnlineEnrollmentRequest, type);

			programEnrollmentServiceDAO.createUpdateDeleteEnrollment(json_inputarealog);

			ResponseMessage successMessage = new ResponseMessage("create.enrollment.success",
					"create enrollment successful", MessageType.CONFIRMATION);
			response = FPLWSResponse.ok(PayOnlineEnrollmentResponse.class);
			
			logger.info("createEnrollment_success | ProgramEnrollmentService | ProgramEnrollmentServiceImpl | createEnrollment End | Account number - "
					+ programEnrollmentRequest.getAccountNumber());
			
			return FPLWSResponse.assemble()
					.addResponseMessage(successMessage)
					.build(ResponseHttpStatus.OK, response);

			//return FPLWSResponse.assemble().build(ResponseHttpStatus.OK, successMessage);
		} catch (ApiServiceException e) {
			logger.error("createEnrollment_error | ProgramEnrollmentService | ProgramEnrollmentServiceImpl | createEnrollment | DataAccessException for Account number - "
					+ programEnrollmentRequest.getAccountNumber());
			throw new ApiServiceException(e);

		}

	}

	@SuppressWarnings("rawtypes")
	@Override
	public FPLWSResponse retrieveEnrollment(String accountNumber) throws ApiServiceException {
		// TODO Auto-generated method stub
		//FPLWSResponse<PayOnlineRetrieveEnrollmentResponse> response = null;
		logger.info("ProgramEnrollmentServiceImpl | retrieveEnrollment Start | Account number - " + accountNumber);

		try {

			JsonInputarealog json_inputarealog = new JsonInputarealog();
			json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

			json_inputarealog.setBillaccount(accountNumber);
			json_inputarealog.setAction("VALIDATE");
			json_inputarealog.setRequest("CHANGE");

			OutputResponse retrieveEnrollment = programEnrollmentServiceDAO.retrieveEnrollment(json_inputarealog);

			PayOnlineRetrieveEnrollmentResponse payOnlineRetrieveEnrollmentResponse = new PayOnlineRetrieveEnrollmentResponse();
			String futurePaymentFlag = retrieveEnrollment.getCUOBI105OperationResponse().getJsonOutputarealog()
					.getFutpymtexist();
			if (futurePaymentFlag != null && "Y".equalsIgnoreCase(futurePaymentFlag)) {
				payOnlineRetrieveEnrollmentResponse.setScheduledFuturePaymentExists(true);
				throw new ApiServiceException(UserMessageCodes.RETRIEVE_ENROLLMENT_SCHEDULED_PAYMENT_EXISTS,
						"scheduled payment already exists", ResponseHttpStatus.VALIDATION);
			} else {
				payOnlineRetrieveEnrollmentResponse.setScheduledFuturePaymentExists(false);
			}

			BankInfo bankInformation = new BankInfo();
			bankInformation.setBankAccountNumber(
					retrieveEnrollment.getCUOBI105OperationResponse().getJsonOutputarealog().getBankacctlastfour());
			bankInformation.setBankRoutingNumber(
					retrieveEnrollment.getCUOBI105OperationResponse().getJoProcessinfoout().getBankRtngTrnst());
			payOnlineRetrieveEnrollmentResponse.setBankInformation(bankInformation);
			payOnlineRetrieveEnrollmentResponse.setEmailAddress(
					retrieveEnrollment.getCUOBI105OperationResponse().getJsonOutputarealog().getEmailaddressout());

			ResponseMessage successMessage = new ResponseMessage("retrieve.enrollment.success",
					"retrieve enrollment successful", MessageType.CONFIRMATION);			
			
			logger.info("retrieveEnrollment_success | ProgramEnrollmentServiceImpl | retrieveEnrollment End | Account number - " + accountNumber); 
			
			return FPLWSResponse
					.assemble()
					.addResponseMessage(successMessage)
					.build(ResponseHttpStatus.OK, payOnlineRetrieveEnrollmentResponse);
			

		} catch (ApiServiceException e) {
			logger.error("retrieveEnrollment_error | ProgramEnrollmentServiceImpl | retrieveEnrollment | DataAccessException for Account number - "
					+ accountNumber);
			throw new ApiServiceException(e);

		}
	}
	
	@Override
	public FPLWSResponse validateEnrollment(String accountNumber) {
		// TODO Auto-generated method stub
		logger.info("ProgramEnrollmentServiceImpl | validateEnrollment Start | Account number - " + accountNumber);

		try {

			JsonInputarealog json_inputarealog = new JsonInputarealog();
			json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

			json_inputarealog.setBillaccount(accountNumber);
			json_inputarealog.setAction("VALIDATE");
			json_inputarealog.setRequest("ENROLLMENT");

			OutputResponse retrieveEnrollment = programEnrollmentServiceDAO.retrieveEnrollment(json_inputarealog);
			
			PayOnlineEnrollmentOptions validateEnrollmentResponse = new PayOnlineEnrollmentOptions();
			validateEnrollmentResponse.setEligible(true);
			
			ResponseMessage successMessage = new ResponseMessage("validate.enrollment.success",
					"validate enrollment successful", MessageType.CONFIRMATION);			
			
			logger.info("validateEnrollment_success | ProgramEnrollmentServiceImpl | validateEnrollment End | Account number - " + accountNumber); 
			
			return FPLWSResponse
					.assemble()
					.addResponseMessage(successMessage)
					.build(ResponseHttpStatus.OK, validateEnrollmentResponse);
			

		} catch (ApiServiceException e) {
			logger.error("validateEnrollment_error | ProgramEnrollmentServiceImpl | validateEnrollment | DataAccessException for Account number - "
					+ accountNumber);
			throw new ApiServiceException(e);

		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public FPLWSResponse updateEnrollment(ProgramEnrollmentRequest<?> programEnrollmentRequest, String mediaChannel, UserInfo userInfo)
			throws ApiServiceException {
		// TODO Auto-generated method stub
		String type = "UPDATE";
		FPLWSResponse<PayOnlineEnrollmentResponse> response = null;
		logger.info("ProgramEnrollmentServiceImpl | updateEnrollment Start | Account number - "
				+ programEnrollmentRequest.getAccountNumber());

		try {

			this.retrieveEnrollment((programEnrollmentRequest.getAccountNumber()));
			PayOnlineEnrollmentRequest payOnlineEnrollmentRequest = (PayOnlineEnrollmentRequest) programEnrollmentRequest;
            //NACHA Bank account number validation-start
            nachaHelper.validateBankAccount(payOnlineEnrollmentRequest, mediaChannel);
            //NACHA Bank account number validation-end
			PayOnlineEnrollmentResponse payOnlineEnrollmentResponse = new PayOnlineEnrollmentResponse();

			if (payOnlineEnrollmentRequest.getEmailAddress() != null
					&& payOnlineEnrollmentRequest.getEmailAddress().getValue() != null
					&& (payOnlineEnrollmentRequest.getEmailAddress().getValue().equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT")
							|| payOnlineEnrollmentRequest.getEmailAddress().getValue()
									.equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT@default.com"))) {
				String emailAddressUpdated = userInfo.getEmail();
				logger.info("ProgramEnrollmentServiceImpl | updateEnrollment | emailAddressUpdated=[{}]", emailAddressUpdated);
				payOnlineEnrollmentResponse.setEmailAddresss(emailAddressUpdated);
			}

			// Server Side Routing Number Validation
			if (payOnlineEnrollmentRequest.getData().getBankInformation().getBankRoutingNumber() != null) {
				bankingRoutingHelper.validRoutingNumber(
						payOnlineEnrollmentRequest.getData().getBankInformation().getBankRoutingNumber());
			}

			JsonInputarealog json_inputarealog = new JsonInputarealog();
			json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

			processEnrollmenttInputRequest(json_inputarealog, payOnlineEnrollmentRequest, type);

			programEnrollmentServiceDAO.createUpdateDeleteEnrollment(json_inputarealog);

			ResponseMessage successMessage = new ResponseMessage("update.enrollment.success",
					"update enrollment successful", MessageType.CONFIRMATION);
			response = FPLWSResponse.ok(PayOnlineEnrollmentResponse.class);
			
			logger.info("updateEnrollment_suucess | ProgramEnrollmentServiceImpl | updateEnrollment End | Account number - "
					+ programEnrollmentRequest.getAccountNumber());
			
			return FPLWSResponse.assemble()
					.addResponseMessage(successMessage)
					.build(ResponseHttpStatus.OK, response);

			
		} catch (ApiServiceException e) {
			logger.error("updateEnrollment_error | ProgramEnrollmentServiceImpl | updateEnrollment | DataAccessException for Account number - "
					+ programEnrollmentRequest.getAccountNumber());
			throw new ApiServiceException(e);

		}

	}

	@SuppressWarnings("rawtypes")
	@Override
	public FPLWSResponse deleteEnrollment(ProgramEnrollmentRequest<?> programEnrollmentRequest, UserInfo userInfo) throws ApiServiceException {
		// TODO Auto-generated method stub
		//FPLWSResponse<PayOnlineEnrollmentResponse> response = null;
		logger.info("ProgramEnrollmentServiceImpl | deleteEnrollment Start | Account number - " + programEnrollmentRequest.getAccountNumber());

		try {

			PayOnlineEnrollmentRequest payOnlineEnrollmentRequest = (PayOnlineEnrollmentRequest) programEnrollmentRequest;

			PayOnlineEnrollmentResponse payOnlineEnrollmentResponse = new PayOnlineEnrollmentResponse();

			if (payOnlineEnrollmentRequest.getEmailAddress() != null
					&& payOnlineEnrollmentRequest.getEmailAddress().getValue() != null
					&& (payOnlineEnrollmentRequest.getEmailAddress().getValue().equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT")
							|| payOnlineEnrollmentRequest.getEmailAddress().getValue()
									.equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT@default.com"))) {
				String emailAddressUpdated = userInfo.getEmail();
				logger.info("ProgramEnrollmentServiceImpl | deleteEnrollment | emailAddressUpdated=[{}]", emailAddressUpdated);
				payOnlineEnrollmentResponse.setEmailAddresss(emailAddressUpdated);
			}

			JsonInputarealog json_inputarealog = new JsonInputarealog();
			json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

			json_inputarealog.setBillaccount(programEnrollmentRequest.getAccountNumber());
			json_inputarealog.setAction("VALIDATE");
			json_inputarealog.setRequest("DISCONTINUE");
			
			OutputResponse retrieveEnrollment = programEnrollmentServiceDAO.retrieveEnrollment(json_inputarealog);

			
			String futurePaymentFlag = retrieveEnrollment.getCUOBI105OperationResponse().getJsonOutputarealog()
					.getFutpymtexist();
			
			boolean futurePaymentExists = (futurePaymentFlag != null && "Y".equalsIgnoreCase(futurePaymentFlag));
			ResponseMessage successMessage = null;
			
			if(payOnlineEnrollmentRequest.getEligibilityCheck() == null || !payOnlineEnrollmentRequest.getEligibilityCheck()){
				if (futurePaymentExists) {
					throw new ApiServiceException(UserMessageCodes.DISCONTINUE_ENROLLMENT_SCHEDULED_PAYMENT_EXISTS, "scheduled payment already exists", ResponseHttpStatus.VALIDATION);
				} else {
					json_inputarealog.setAction("UPDATE");
					programEnrollmentServiceDAO.createUpdateDeleteEnrollment(json_inputarealog);
					successMessage = new ResponseMessage("discontinue.enrollment.success", "discontinue enrollment successful", MessageType.CONFIRMATION);
				}
			}else {
				if(futurePaymentExists){					
					throw new ApiServiceException(UserMessageCodes.VALIDATE_DISCONTINUE_ENROLLMENT_SCHEDULED_PAYMENT_EXISTS, "scheduled payment already exists", ResponseHttpStatus.VALIDATION);
				}else{
					payOnlineEnrollmentResponse.setScheduledFuturePaymentExists(false);
				}
				successMessage = new ResponseMessage("validate.discontinue.enrollment.success", "discontinue enrollment validated successfully", MessageType.CONFIRMATION);
			}
			
			logger.info("deleteEnrollment_suucess | ProgramEnrollmentServiceImpl | deleteEnrollment End | Account number - " + programEnrollmentRequest.getAccountNumber());
			
			return FPLWSResponse
					.assemble()
					.addResponseMessage(successMessage)
					.build(ResponseHttpStatus.OK, payOnlineEnrollmentResponse);

		} catch (ApiServiceException e) {
			logger.error("deleteEnrollment_error | ProgramEnrollmentServiceImpl | deleteEnrollment | DataAccessException for Account number - "
					+ programEnrollmentRequest.getAccountNumber());
			throw new ApiServiceException(e);

		}
	}
	
	private void processEnrollmenttInputRequest(JsonInputarealog json_inputarealog,
			PayOnlineEnrollmentRequest payOnlineEnrollmentRequest, String type) {

		logger.info("ProgramEnrollmentServiceImpl | processEnrollmenttInputRequest Start for Account - "
				+ payOnlineEnrollmentRequest.getAccountNumber());

		json_inputarealog.setBillaccount(payOnlineEnrollmentRequest.getAccountNumber());
		json_inputarealog.setAction("UPDATE");
		switch (type) {
		case "CREATE":
			json_inputarealog.setRequest("ENROLLMENT");
			break;
		case "UPDATE":
			json_inputarealog.setRequest("CHANGE");
			break;
		}

		json_inputarealog.setRequestedby(payOnlineEnrollmentRequest.getRequestedBy());
		EmailAddress emailAddress = payOnlineEnrollmentRequest.getEmailAddress();
		if (emailAddress != null) {
			json_inputarealog.setEmailaddress(emailAddress.getValue().toUpperCase());
		}

		setRequestWithBankInfo(json_inputarealog, payOnlineEnrollmentRequest);

	}

	private void setRequestWithBankInfo(JsonInputarealog json_inputarealog,
			PayOnlineEnrollmentRequest payOnlineEnrollmentRequest) {

		BankInfoOption bankInfoOption = payOnlineEnrollmentRequest.getData().getBankInformation().getBankInfoOption();
		switch (bankInfoOption) {
		/*
		 * ********COMMENTED OUT PENDING MAINFRAME CHANGES BEING PROMOTED FROM CT -> CS
		 * case PAY_BY_PHONE: paymentCommArea.setEXISTINGBANKINFO("P"); break; case
		 * AUTOMATIC_BILL_PAYMENT: paymentCommArea.setEXISTINGBANKINFO("A"); break;
		 */
		case NEW_BANK_INFO:
			json_inputarealog.setBankroutetransitnum(
					payOnlineEnrollmentRequest.getData().getBankInformation().getBankRoutingNumber());
			json_inputarealog.setBankaccountnum(
					payOnlineEnrollmentRequest.getData().getBankInformation().getBankAccountNumber());
			break;
		default:
			// TODO: throw service exception
		}
	}

}
