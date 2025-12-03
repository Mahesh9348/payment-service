package com.fplws.services.payment.serviceimpl;

import com.fpl.common.converters.AmountConverter;
import com.fpl.common.data.MessageType;
import com.fpl.common.data.ResponseMessage;
import com.fpl.common.exception.DataAccessException;
import com.fpl.common.messages.UserMessageCodes;
import com.fpl.common.util.StringUtil;
import com.fpl.webservices.data.*;
import com.fpl.webservices.data.transaction.PaymentRequest;
import com.fplws.common.framework.fplcommonframework.beans.UserInfo;
import com.fplws.common.framework.fplcommonframework.constant.EnvConstants;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.mf.payonlinek97a.model.JsonInputarealog;
import com.fplws.mf.payonlinek97a.model.OutputResponse;
import com.fplws.services.payment.dao.PaymentServiceDAO;
import com.fplws.services.payment.exception.PaymentServiceException;
import com.fplws.services.payment.service.PaymentService;
import com.fplws.services.payment.utility.ExceptionMessageHelper;
import com.fplws.services.payment.utility.UpdateBlankInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author nxm01hn 
 * Payment Service Impl
 *
 */
@Service("paymentService")
public class PaymentServiceImpl implements PaymentService {

	@Autowired
	PaymentServiceDAO paymentServiceDao;

	@Autowired
	ExceptionMessageHelper exceptionHelper;

	@Autowired
	UpdateBlankInput updateBlankInput;

	private static HashMap<Integer, String> donationUpdateMap;

	private static Logger logger = LogFactory.getLogger(PaymentServiceImpl.class);

	static {
		donationUpdateMap = new HashMap<Integer, String>();
		donationUpdateMap.put(1, "Donationone");
		donationUpdateMap.put(2, "Donationtwo");
		donationUpdateMap.put(3, "Donationthree");
		donationUpdateMap.put(4, "Donationfour");
		donationUpdateMap.put(5, "Donationfive");
		donationUpdateMap.put(6, "Donationsix");
	}

	private static Map<MessageType, String> messageTypeMap;

	static {
		messageTypeMap = new HashMap<MessageType, String>();
		messageTypeMap.put(MessageType.WARNING, "2");
		messageTypeMap.put(MessageType.CONFIRMATION, "3");
	}

	AmountConverter amountConverter = new AmountConverter();
	SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

	@SuppressWarnings("rawtypes")
	@Override
	public FPLWSResponse getScheduledPayment(String accountNumber) throws ApiServiceException  {
		// TODO Auto-generated method stub

		Payment scheduledPayment = null;

		logger.info("getScheduledPayment | PaymentServiceImpl | getScheduledPayment Start | Account number - " + accountNumber);


			scheduledPayment = getScheduledPaymentDAO(accountNumber);

			if (scheduledPayment != null) {
				logger.info("getScheduledPayment_success | PaymentServiceImpl | getScheduledPayment End | Account number - " + accountNumber);
				return FPLWSResponse.assemble().build(ResponseHttpStatus.OK, scheduledPayment);
			} else {
				throw new ApiServiceException ("scheduledPayment.noDataFound",
						"No scheduled payments found for account: " + accountNumber, ResponseHttpStatus.NO_CONTENT,
						false);
			}


	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public FPLWSResponse updateScheduledPayment(String accountNumber, PaymentRequest paymentRequest, UserInfo userInfo)
			throws ApiServiceException  {

		logger.info("updateScheduledPayment | PaymentServiceImpl | updateScheduledPayment Start | Account number - " + accountNumber);
		//FPLWSResponse<PaymentResult> response = null;
		JsonInputarealog json_inputarealog = new JsonInputarealog();
		json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

		try {

			paymentRequest.setAccountNumber(accountNumber);

			// In case of no email on account, get it from the jwt token
			boolean emailAddressUpdatedWithLdapEmailAddress = false;

			if (paymentRequest.getEmailAddress() != null && !paymentRequest.getEmailAddress().isEmpty()
					&& paymentRequest.getEmailAddress().equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT")) {
				String emailAddress = userInfo.getPreferred_username();

				logger.info("updateScheduledPayment | PaymentServiceImpl | updateScheduledPayment | emailAddress=[{}]", emailAddress);
				paymentRequest.setEmailAddress(emailAddress);
				emailAddressUpdatedWithLdapEmailAddress = true;
			}

			PaymentOption polInfo = getPaymentOption(accountNumber);

			logger.info("updateScheduledPayment | PaymentServiceImpl | updateScheduledPayment | Received Payment Options for Account number - "
					+ accountNumber);

			// Validate Payment
			validateScheduledPayment(polInfo.getExistestScheduledPayment());
			validatePaymentAmount(polInfo, paymentRequest);
			validateUpdatingScheduledPayment(polInfo, paymentRequest);
			validateSchedulingPaymentDate(polInfo, paymentRequest);
			validateBankInfo(polInfo, paymentRequest);

			json_inputarealog.setAction("UPDATE");
			json_inputarealog.setRequest("AMEND");

			json_inputarealog = processPaymentInputCommarea(json_inputarealog, paymentRequest);
			json_inputarealog.setPymtchannel(paymentRequest.getChannel());

			ResponseMessage successMessage = new ResponseMessage(UserMessageCodes.UPDATE_SCHEDULED_PAYMENT_SUCCESS,
					"Message Payment - Updated Scheduled Payment ", MessageType.CONFIRMATION);

			logger.info(
					"updateScheduledPayment | PaymentServiceImpl | updateScheduledPayment | Call createUpdateDeleteSchedulePayment DAO for Account - "
							+ accountNumber);

			OutputResponse updateScheduledPayment = paymentServiceDao
					.createUpdateDeleteSchedulePayment(json_inputarealog);

			PaymentResult paymentResult = new PaymentResult(
					updateScheduledPayment.getCUOBI105OperationResponse().getJoProcessinfoout().getConfirmationnumber()
							.trim(),
					emailAddressUpdatedWithLdapEmailAddress ? paymentRequest.getEmailAddress() : null);

			logger.info("updateScheduledPayment_success | PaymentServiceImpl | updateScheduledPayment Emd | Account number - " + accountNumber);
			return FPLWSResponse.assemble()
					.addResponseMessage(successMessage)
					.build(ResponseHttpStatus.OK, paymentResult);

		} catch (ApiServiceException de) {
			logger.error("updateScheduledPayment_error | PaymentServiceImpl | updateScheduledPayment | ApiServiceException for " + accountNumber);
			throw new ApiServiceException (de);
		} catch (Exception e) {
			logger.error("updateScheduledPayment_error | PaymentServiceImpl | updateScheduledPayment | Exception for " + accountNumber);
			throw new ApiServiceException (e);
		}

	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public FPLWSResponse deleteScheduledPayment(String accountNumber, PaymentRequest paymentRequest, UserInfo userInfo)
			throws ApiServiceException  {

		logger.info("PaymentServiceImpl | deleteScheduledPayment Start | Account number - " + accountNumber);
		FPLWSResponse<PaymentResult> response = null;
		JsonInputarealog json_inputarealog = new JsonInputarealog();
		json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

		try {

			paymentRequest.setAccountNumber(accountNumber);

			// In case of no email on account, get it from the jwt token
			boolean emailAddressUpdatedWithLdapEmailAddress = false;

			if (paymentRequest.getEmailAddress() != null && !paymentRequest.getEmailAddress().isEmpty()
					&& paymentRequest.getEmailAddress().equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT")) {
				String emailAddress = userInfo.getPreferred_username();
				logger.info("PaymentServiceImpl | deleteScheduledPayment | emailAddress=[{}]", emailAddress);
				paymentRequest.setEmailAddress(emailAddress);
				emailAddressUpdatedWithLdapEmailAddress = true;
			}

			Payment scheduledPayment = getScheduledPaymentDAO(accountNumber);

			logger.info(
					"PaymentServiceImpl | deleteScheduledPayment | Received Schedule Payment Details for Account number - "
							+ accountNumber);

			validateScheduledPayment(scheduledPayment);

			PaymentDetail pm = new PaymentDetail();
			pm.setAmount(scheduledPayment.getAmount());
			pm.setPaymentDate(scheduledPayment.getPaymentDate());
			paymentRequest.setData(pm);

			json_inputarealog.setAction("UPDATE");
			json_inputarealog.setRequest("WITHDRAW");

			json_inputarealog = processPaymentInputCommarea(json_inputarealog, paymentRequest);
			json_inputarealog.setPymtchannel(paymentRequest.getChannel());

			ResponseMessage successMessage = new ResponseMessage(UserMessageCodes.DELETE_SCHEDULED_PAYMENT_SUCCESS,
					"Message Payment - Delete Scheduled Payment ", MessageType.CONFIRMATION);

			initializeDonations(json_inputarealog);

			logger.info(
					"PaymentServiceImpl | deleteScheduledPayment | Call createUpdateDeleteSchedulePayment DAO for Account - "
							+ accountNumber);

			OutputResponse deleteScheduledPayment = paymentServiceDao
					.createUpdateDeleteSchedulePayment(json_inputarealog);

			response = FPLWSResponse.ok(PaymentResult.class);
			PaymentResult paymentResult = new PaymentResult(
					deleteScheduledPayment.getCUOBI105OperationResponse().getJoProcessinfoout().getConfirmationnumber()
							.trim(),
					emailAddressUpdatedWithLdapEmailAddress ? paymentRequest.getEmailAddress() : null);

			response.message(successMessage);

			logger.info("deleteScheduledPayment_success | PaymentServiceImpl | deleteScheduledPayment Emd | Account number - " + accountNumber);
			return FPLWSResponse
					.assemble()
					.addResponseMessage(successMessage)
					.build(ResponseHttpStatus.OK, paymentResult);

		} catch (ApiServiceException de) {
			logger.error("deleteScheduledPayment_error | PaymentServiceImpl | deleteScheduledPayment | ApiServiceException for " + accountNumber);
			throw new ApiServiceException (de);
		} catch (Exception e) {
			logger.error("deleteScheduledPayment_error | PaymentServiceImpl | deleteScheduledPayment | Exception for " + accountNumber);
			throw new ApiServiceException (e);
		} catch (Throwable e) {
			logger.error("deleteScheduledPayment_error | PaymentServiceImpl | deleteScheduledPayment | Throwable Exception for " + accountNumber);
			throw new ApiServiceException (e);
		}

	}

	@SuppressWarnings("rawtypes")
	@Override
	public FPLWSResponse createScheduledPayment(String accountNumber, PaymentRequest paymentRequest, UserInfo userInfo)
			throws ApiServiceException  {
		// TODO Auto-generated method stub

		logger.info("PaymentServiceImpl | createScheduledPayment Start | Account number - " + accountNumber);
		//FPLWSResponse<PaymentResult> response = null;

		JsonInputarealog json_inputarealog = new JsonInputarealog();
		json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);

		try {

			paymentRequest.setAccountNumber(accountNumber);
			Boolean allowedDonations = true;
			Double donationAmount = 0.00;

			// In case of no email on account, get it from the jwt token
			boolean emailAddressUpdatedWithLdapEmailAddress = false;

			if (paymentRequest.getEmailAddress() != null && !paymentRequest.getEmailAddress().isEmpty()
					&& paymentRequest.getEmailAddress().equalsIgnoreCase("NO_EMAIL_ON_ACCOUNT")) {
				String emailAddress = userInfo.getPreferred_username();
				logger.info("PaymentServiceImpl | createScheduledPayment | emailAddress=[{}]", emailAddress);
				paymentRequest.setEmailAddress(emailAddress);
				emailAddressUpdatedWithLdapEmailAddress = true;
			}

			PaymentOption polInfo = getPaymentOption(accountNumber);

			// Validate Payment
			validateBankInfo(polInfo, paymentRequest);

			logger.info("PaymentServiceImpl | createScheduledPayment | Received Payment Options for Account number - "
					+ accountNumber);
			json_inputarealog.setAction("UPDATE");
			json_inputarealog.setRequest("PAYMENT");

			ResponseMessage todayOrScheduledMessage = new ResponseMessage(UserMessageCodes.CREATE_PAYMENT_SUCCESS_TODAY,
					"Message Payment - Payment for Today", MessageType.CONFIRMATION);
			Boolean scheduled = !isPaymentForToday(polInfo, paymentRequest);

			if (scheduled) {
				logger.info("PaymentServiceImpl | createScheduledPayment | Schedule - " + scheduled
						+ " for Account number - " + accountNumber);
				allowedDonations = true;
				validateCreatingScheduledPayment(polInfo, paymentRequest);
				validateSchedulingPaymentDate(polInfo, paymentRequest);
				todayOrScheduledMessage.setMessageCode(UserMessageCodes.CREATE_PAYMENT_SUCCESS_SCHEDULED);
				todayOrScheduledMessage.setMessage("Message Payment - Payment Scheduled");
			}

			json_inputarealog = processPaymentInputCommarea(json_inputarealog, paymentRequest);
			json_inputarealog = processPaymentEMBOffer(json_inputarealog, paymentRequest, polInfo);

			ResponseMessage successMessage = new ResponseMessage(UserMessageCodes.CREATE_PAYMENT_SUCCESS_NO_DONATIONS,
					"Message Payment - Payment without Donations", MessageType.CONFIRMATION);

			if (allowedDonations) {
				logger.info("PaymentServiceImpl | createScheduledPayment | In Allow Donation for Account - "
						+ accountNumber);
				json_inputarealog = processDonations(json_inputarealog, paymentRequest, polInfo);
				List<PaymentDonation> er = paymentRequest.getData().getDonations();
				// If it got to this point, all entered donations were allowed and need to get
				// accumulated for a total
				if ((er != null) && (er.size() > 0)) {
					for (PaymentDonation ed : er) {						
							donationAmount = donationAmount + ed.getAmount();
					}
					// Modify success message
					if (donationAmount > 0) {
						successMessage.setMessageCode(UserMessageCodes.CREATE_PAYMENT_SUCCESS_DONATIONS);
						successMessage.setMessage("Message Payment - Payment with Donations");
					}
				}
			}

			json_inputarealog.setPymtchannel(paymentRequest.getChannel());
			logger.info(
					"PaymentServiceImpl | createScheduledPayment | Call createUpdateDeleteSchedulePayment DAO for Account - "
							+ accountNumber);

			OutputResponse createScheduledPayment = paymentServiceDao
					.createUpdateDeleteSchedulePayment(json_inputarealog);

			int noDonations = Integer.parseInt(json_inputarealog.getNodonations());

			logger.info("createScheduledPayment_success | PaymentServiceImpl | createScheduledPayment Emd | Account number - " + accountNumber);

			return FPLWSResponse
					.assemble()
					.addResponseMessage(successMessage)
					.addResponseMessage(processEMBMessage(paymentRequest, polInfo))
					.addResponseMessage(todayOrScheduledMessage)
					.addResponseMessage(processRCSMessage(createScheduledPayment, paymentRequest, polInfo))
					.build(ResponseHttpStatus.OK, processCreatePaymentOutput(createScheduledPayment, paymentRequest, polInfo, donationAmount,
							emailAddressUpdatedWithLdapEmailAddress, noDonations));
		} catch (ApiServiceException de) {
			logger.error("createScheduledPayment_error | PaymentServiceImpl | createSchedulePayment | ApiServiceException for " +
					"accountNumber=[{}], Exception=[{}]", accountNumber, de.getMessage());
			throw new ApiServiceException (de);
		} catch (Exception e) {
			logger.error("createScheduledPayment_error | PaymentServiceImpl | createScheduledPayment | Exception for " + accountNumber+" Cause "+e.getMessage());
			throw new ApiServiceException (e);
		} catch (Throwable e) {
			logger.error("createScheduledPayment_error | PaymentServiceImpl | createScheduledPayment | Throwable Exception for " + accountNumber);
			throw new ApiServiceException (e);
		}

	}

	private Payment getScheduledPaymentDAO(String accountNumber) {
		JsonInputarealog json_inputarealog = new JsonInputarealog();
		json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);
		json_inputarealog.setBillaccount(accountNumber);
		json_inputarealog.setAction("VALIDATE");
		json_inputarealog.setRequest("PAYMENT");

		return paymentServiceDao.getScheduledPayment(json_inputarealog);
	}

	private PaymentOption getPaymentOption(String accountNumber) throws ApiServiceException{
		try {
			JsonInputarealog json_inputarealog = new JsonInputarealog();
			json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);
			json_inputarealog.setBillaccount(accountNumber);
			json_inputarealog.setAction("VALIDATE");
			json_inputarealog.setRequest("PAYMENT");

			return paymentServiceDao.getPaymentOption(json_inputarealog);
		}catch (ApiServiceException de) {
			logger.error("getPaymentOption_error | PaymentServiceImpl | getPaymentOption | ApiServiceException for accountNumber=[{}]" +
					", Exception=[{}]", accountNumber, de.getMessage());
			throw new ApiServiceException (de);
		} 
		
	}

	private ResponseMessage processRCSMessage(OutputResponse createScheduledPayment, PaymentRequest paymentRequest,
			PaymentOption polInfo) {

		logger.info("PaymentServiceImpl | processRCSMessage Start for Account - " + paymentRequest.getAccountNumber());
		ResponseMessage rrm = null;
		Double reconnectAmount = polInfo.getRcsInformation().getReconnectAmount();
		Double totalAmount = paymentRequest.getData().getAmount();

		if (createScheduledPayment.getCUOBI105OperationResponse().getJoProcessinfoout().getMessageindicator()
				.equals(messageTypeMap.get(MessageType.CONFIRMATION))) {
			if (totalAmount > 0 && reconnectAmount > 0) {
				// Additional Reconnect message
				rrm = new ResponseMessage(null, "Message Payment - Reconnect", MessageType.CONFIRMATION);
				if (reconnectAmount > totalAmount)
					if (polInfo.getRcsInformation().getRcsFlag())
						rrm.setMessageCode(UserMessageCodes.CREATE_PAYMENT_SUCCESS_UCSD_NIGHT_RCS);
					else
						rrm.setMessageCode(UserMessageCodes.CREATE_PAYMENT_SUCCESS_UCSD_NIGHT_NONRCS);
				else if (polInfo.getRcsInformation().getRcsFlag())
					rrm.setMessageCode(UserMessageCodes.CREATE_PAYMENT_SUCCESS_UCSD_DAY_RCS);
				else
					rrm.setMessageCode(UserMessageCodes.CREATE_PAYMENT_SUCCESS_UCSD_DAY_NONRCS);
			}
		}
		return rrm;
	}

	private ResponseMessage processEMBMessage(PaymentRequest paymentRequest, PaymentOption polInfo) {
		logger.info("PaymentServiceImpl | processEMBMessage Start for Account - " + paymentRequest.getAccountNumber());
		ResponseMessage embrm = null;
		Boolean embCanBeOffered = polInfo.getEmbEligible(); // True if account is eligible
		Boolean embAccepted = paymentRequest.getEmbAccepted(); // True if user accepted emb enroll
		if (embCanBeOffered) {
			embrm = new ResponseMessage(UserMessageCodes.PAYMENT_SUCCESS_EMB_NOT_ACCEPTED,
					"Message Payment - Emb Offer", MessageType.CONFIRMATION);
			if (embAccepted)
				embrm.setMessageCode(UserMessageCodes.PAYMENT_SUCCESS_EMB_ACCEPTED);
		}
		return embrm;
	}

	private PaymentResult processCreatePaymentOutput(OutputResponse outputResponse, PaymentRequest paymentRequest,
			PaymentOption polInfo, Double donationAmount, boolean emailAddressUpdatedWithLdapEmailAddress,
			int noDonations) {

		String BankAccountnumber = "NA";
		logger.info("PaymentServiceImpl | createScheduledPayment | processCreatePaymentOutput Start for Account - "
				+ paymentRequest.getAccountNumber());
		PaymentResult paymentResult = new PaymentResult(
				outputResponse.getCUOBI105OperationResponse().getJoProcessinfoout().getConfirmationnumber().trim());
		
		if(polInfo.getBankInformation().getBankAccountNumber().trim()!=null)
			BankAccountnumber = polInfo.getBankInformation().getBankAccountNumber().trim();
		
		Payment pr = new Payment(paymentRequest.getData().getPaymentDate(), paymentRequest.getData().getAmount(), null,
				BankAccountnumber, null);
		paymentResult.setPayment(pr);
		paymentResult.setDonationAmount(donationAmount);
		paymentResult.setDonationCounter(noDonations);
		paymentResult.setRcsInformation(polInfo.getRcsInformation());
		paymentResult.setSystemDate(new Date());

		if (emailAddressUpdatedWithLdapEmailAddress) {
			paymentResult.setEmailAddress(paymentRequest.getEmailAddress());
		}

		if (outputResponse.getCUOBI105OperationResponse().getJsonOutputarealog().getClfm27elig() != null
				&& outputResponse.getCUOBI105OperationResponse().getJsonOutputarealog().getClfm27elig().trim()
						.equals("Y")) {
			paymentResult.setClfmFactorEligibility(true);
		} else {
			paymentResult.setClfmFactorEligibility(false);
		}

		return paymentResult;
	}

	private JsonInputarealog processDonations(JsonInputarealog json_inputarealog, PaymentRequest paymentRequest,
			PaymentOption polInfo) throws ApiServiceException , Throwable {
		logger.info("PaymentServiceImpl | processDonations Start for Account - " + paymentRequest.getAccountNumber());

		ArrayList<Integer> allowedKeys = new ArrayList<Integer>();
		int noDon = 0;
		int countOfDonations = 0;
		List<PaymentDonation> enteredDonations = paymentRequest.getData().getDonations();

		// quick fix to allow submitting payment if mainframe has only one donation
		// configured.
		// so first we initialize all donation amount with "000000000.00" and donation
		// codes
		json_inputarealog = initializeDonations(json_inputarealog);

		for (PaymentDonation pd : polInfo.getDonations()) {
			allowedKeys.add(pd.getCode());
		}

		if ((enteredDonations != null) && (enteredDonations.size() > 0)) {
			Collections.sort(enteredDonations);
			for (PaymentDonation ed : enteredDonations) {
				if (allowedKeys.contains(ed.getCode())) {
					// As per Edward during testing of new K97A, donations should be initialized to
					// zeroes even if they were not selected.
					if(ed.getAmount() == null) {
						ed.setAmount(0.00);						
					}
					
					if (ed.getAmount() > 0)
						countOfDonations++;
					
					noDon = noDon + 1;
					String donationName = donationUpdateMap.get(noDon);
					String mfCode = String.valueOf(ed.getCode());
					amountConverter.setParameter("000000000.00");
					String mfAmt = amountConverter.convertFrom(ed.getAmount(), null);
					setDonation(donationName, json_inputarealog, mfAmt, mfCode);
				} else {
					throw new PaymentServiceException(UserMessageCodes.CREATE_PAYMENT_DONATION_INACTIVE,
							"Donation Inactive", ResponseHttpStatus.VALIDATION);
				}
			}
			json_inputarealog.setNodonations(String.valueOf(countOfDonations));
		}
		return json_inputarealog;
	}

	private JsonInputarealog initializeDonations(JsonInputarealog json_inputarealog)
			throws ApiServiceException , Throwable {
		logger.info(
				"PaymentServiceImpl | initializeDonations Start for Account - " + json_inputarealog.getBillaccount());
		for (Map.Entry<Integer, String> entry : donationUpdateMap.entrySet()) {
			String donationName = entry.getValue();
			String mfCode = "0";
			amountConverter.setParameter("000000000.00");
			String mfAmt = amountConverter.convertFrom(0.0, null);
			setDonation(donationName, json_inputarealog, mfAmt, mfCode);
		}
		return json_inputarealog;
	}

	private void setDonation(String donationName, JsonInputarealog json_inputarealog, String mfAmt, String mfCode) {
		// TODO Auto-generated method stub

		switch (donationName) {
		case "Donationone":
			json_inputarealog.getDonationone().setDonationonecode(mfCode);
			json_inputarealog.getDonationone().setDonationoneamt(mfAmt);
			break;
		case "Donationtwo":
			json_inputarealog.getDonationtwo().setDonationtwocode(mfCode);
			json_inputarealog.getDonationtwo().setDonationtwoamt(mfAmt);
			break;
		case "Donationthree":
			json_inputarealog.getDonationthree().setDonationthreecode(mfCode);
			json_inputarealog.getDonationthree().setDonationthreeamt(mfAmt);
			break;
		case "Donationfour":
			json_inputarealog.getDonationfour().setDonationfourcode(mfCode);
			json_inputarealog.getDonationfour().setDonationfouramt(mfAmt);
			break;
		case "Donationfive":
			json_inputarealog.getDonationfive().setDonationfivecode(mfCode);
			json_inputarealog.getDonationfive().setDonationfiveamt(mfAmt);
			break;
		case "Donationsix":
			json_inputarealog.getDonationsix().setDonationsixcode(mfCode);
			json_inputarealog.getDonationsix().setDonationsixamt(mfAmt);
			break;

		}
	}

	private JsonInputarealog processPaymentEMBOffer(JsonInputarealog json_inputarealog, PaymentRequest paymentRequest,
			PaymentOption polInfo) throws ApiServiceException  {
		logger.info(
				"PaymentServiceImpl | processPaymentEMBOffer Start for Account - " + paymentRequest.getAccountNumber());
		EmbOfferEnum offerToEnrollAcctInEmb = EmbOfferEnum.NOTELIG; // Default
		Boolean embCanBeOffered = polInfo.getEmbEligible(); // True if account is eligible
		Boolean embAccepted = paymentRequest.getEmbAccepted(); // True if user accepted emb enroll
		if (embCanBeOffered) {
			if (embAccepted == null)
				throw new ApiServiceException (UserMessageCodes.PAYMENT_EMB_REQUIRED,
						"Answer of EMB enroll is required", ResponseHttpStatus.VALIDATION);
			if (embAccepted) {
				offerToEnrollAcctInEmb = EmbOfferEnum.ACCEPTED;
				json_inputarealog.setEmailbillenroll("Y");
			} else
				offerToEnrollAcctInEmb = EmbOfferEnum.DECLINED;
		} else if (embAccepted != null && embAccepted)
			throw new ApiServiceException (UserMessageCodes.PROGRAM_NOT_ELIGIBLE, "Not Authorized to enroll in EMB",
					ResponseHttpStatus.UNAUTHORIZED);
		json_inputarealog.setEmailbilloffer(offerToEnrollAcctInEmb.toString());
		return json_inputarealog;
	}

	private JsonInputarealog processPaymentInputCommarea(JsonInputarealog json_inputarealog,
			PaymentRequest paymentRequest) {
		logger.info("PaymentServiceImpl | processPaymentInputCommarea Start for Account - "
				+ paymentRequest.getAccountNumber());
		json_inputarealog.setRequestedby(paymentRequest.getRequestedBy().trim());
		json_inputarealog.setEmailaddress(paymentRequest.getEmailAddress().trim().toUpperCase());
		json_inputarealog.setBillaccount(StringUtil.formatBANumber(paymentRequest.getAccountNumber()));
		amountConverter.setParameter("000000000.00");
		json_inputarealog.setPaymentamount(amountConverter.convertFrom(paymentRequest.getData().getAmount(), null));
		json_inputarealog.setPaymentdate(format.format(paymentRequest.getData().getPaymentDate()));
		// For some reason, MF wants always a N on EMB enroll, even if the account is
		// not eligible
		json_inputarealog.setEmailbillenroll("N");
		return json_inputarealog;
	}

	private void validateCreatingScheduledPayment(PaymentOption polInfo, PaymentRequest paymentRequest)
			throws ApiServiceException  {

		logger.info("PaymentServiceImpl | validateCreatingScheduledPayment Start for Account - "
				+ paymentRequest.getAccountNumber());
		List<PaymentDonation> enteredDonations = paymentRequest.getData().getDonations();

		if (!polInfo.getFuturePayEligibility())
			throw new ApiServiceException (UserMessageCodes.CREATE_PAYMENT_SCHEDULE_PAYMENT_NOT_ELIGIBLE,
					"Schedule Payment Not Eligible", ResponseHttpStatus.VALIDATION);
		if (polInfo.getFuturePaymentExists() || polInfo.getExistestScheduledPayment() != null)
			throw new ApiServiceException (UserMessageCodes.CREATE_PAYMENT_SCHEDULE_PAYMENT_ALREADY_IN_PLACE,
					"Schedule Payment Already in Place", ResponseHttpStatus.VALIDATION);
		boolean donationsAdded = false;
		for (PaymentDonation ed : enteredDonations) {
			if (ed.getAmount() != null) {
				if(ed.getAmount() > 0) 
					donationsAdded = true;
			}
		}
		if (donationsAdded)
			throw new ApiServiceException (UserMessageCodes.CREATE_PAYMENT_SCHEDULE_PAYMENT_NO_DONATIONS,
					"No Donations Allowed in Schedule Payment", ResponseHttpStatus.VALIDATION);

	}

	@SuppressWarnings("deprecation")
	private boolean isPaymentForToday(PaymentOption polInfo, PaymentRequest paymentRequest)
			throws ApiServiceException  {
		logger.info("PaymentServiceImpl | isPaymentForToday Start for Account - " + paymentRequest.getAccountNumber());
		logger.info("PaymentServiceImpl | isPaymentForToday for Account - " + paymentRequest.getAccountNumber()+ "MF date - "+polInfo.getMfDate());
		Date paymentDate = paymentRequest.getData().getPaymentDate();
		paymentDate.setHours(0);
		logger.info("PaymentServiceImpl | isPaymentForToday for Account - " + paymentRequest.getAccountNumber()+ "Requested date - "+paymentRequest.getData().getPaymentDate());
		return (paymentDate.equals(polInfo.getMfDate()));
	}

	private void validateSchedulingPaymentDate(PaymentOption polInfo, PaymentRequest paymentRequest)
			throws ApiServiceException  {
		logger.info("validateSchedulingPaymentDate | PaymentServiceImpl | validateSchedulingPaymentDate Start for Account - "
				+ paymentRequest.getAccountNumber());
		Date firstEligibleDate = getMFCurrentDate(polInfo);
		Date paymentDate = paymentRequest.getData().getPaymentDate();
		if (firstEligibleDate.after(polInfo.getLastEligibilPayDate()))
			throw new ApiServiceException (UserMessageCodes.CREATE_PAYMENT_SCHEDULE_DUE_DATE_PASSED,
					"Invalid Payment - Due Date has Passed", ResponseHttpStatus.VALIDATION);
		if (!(paymentDate.after(firstEligibleDate) && !(paymentDate.compareTo(polInfo.getLastEligibilPayDate()) > 0)))
			throw new ApiServiceException (UserMessageCodes.CREATE_PAYMENT_SCHEDULE_DATE_NOT_IN_RANGE,
					"Invalid Payment Date - Date not in Valid Eligibility Range For Scheduling",
					ResponseHttpStatus.VALIDATION);
	}

	private void validateUpdatingScheduledPayment(PaymentOption polInfo, PaymentRequest paymentRequest)
			throws ApiServiceException  {
		// TODO Auto-generated method stub
		if (polInfo.getExistestScheduledPayment().getAmount().equals(paymentRequest.getData().getAmount()) && polInfo
				.getExistestScheduledPayment().getPaymentDate().equals(paymentRequest.getData().getPaymentDate()))
			throw new ApiServiceException (UserMessageCodes.UPDATE_PAYMENT_SCHEDULE_DATA_UNCHANGED,
					"Schedule Payment Data not changed ", ResponseHttpStatus.VALIDATION);
	}

	private void validatePaymentAmount(PaymentOption polInfo, PaymentRequest paymentRequest)
			throws ApiServiceException  {
		logger.info(
				"validatePaymentAmount | PaymentServiceImpl | validatePaymentAmount Start for Account - " + paymentRequest.getAccountNumber());
		Double paymentAmount = paymentRequest.getData().getAmount();
		Double balance = polInfo.getCurrentAccountBalance();
		if (paymentAmount > balance + 10)
			throw new ApiServiceException (UserMessageCodes.PAYMENT_PAYMENT_AMOUNT_OVERPAY,
					"Invalid Payment Amount - Overpay", ResponseHttpStatus.VALIDATION);
	}

	private void validateScheduledPayment(Payment existestScheduledPayment) throws ApiServiceException  {
		logger.info("PaymentServiceImpl | validatePaymentAmount Start");
		if (existestScheduledPayment == null) {
			logger.error("validateScheduledPayment_error | Schedule Payment - Data not found");
			throw new ApiServiceException (UserMessageCodes.PAYMENT_SCHEDULED_PAYMENT_DATA_NOT_FOUND,
					"Schedule Payment - Data not found ", ResponseHttpStatus.VALIDATION);
		}
	}

	private void validateBankInfo(PaymentOption polInfo, PaymentRequest paymentRequest)
			throws ApiServiceException  {
		logger.info(
				"validateBankInfo | PaymentServiceImpl | validateBankInfo Start for Account - " + paymentRequest.getAccountNumber());
		Boolean isValidAccountNumber = false;
		Boolean isValidRoutingNumber = false;

		if (polInfo.getBankInformation().getBankAccountNumber() != null && !polInfo.getBankInformation().getBankAccountNumber().isEmpty() )
			isValidAccountNumber = true;

		if (polInfo.getBankInformation().getBankRoutingNumber() != null && !polInfo.getBankInformation().getBankRoutingNumber().isEmpty() )
			isValidRoutingNumber = true;

		if (!isValidAccountNumber || !isValidRoutingNumber)
			throw new ApiServiceException (UserMessageCodes.PAYMENT_ACC_NUM_REQUIRED,
					"Payment Bank Info - Data not found ", ResponseHttpStatus.VALIDATION);
	}

	private Date getMFCurrentDate(PaymentOption polInfo) {
		Date currentDate = polInfo.getMfDate();
		String envName = EnvConstants.getEnv();
		if (envName.equalsIgnoreCase("DEV") || envName.equalsIgnoreCase("WTE") || envName.equalsIgnoreCase("TEST")
				|| envName.equalsIgnoreCase("QA")) {
			Date fed = polInfo.getFirstEligibilPayDate();
			currentDate = new Date(fed.getTime() - 1000 * 60 * 60 * 24); // not sure why to subtract one day here....
		}
		
		logger.info("MF Current Date " + "**" + currentDate + "**");
		return currentDate;
	}

}
