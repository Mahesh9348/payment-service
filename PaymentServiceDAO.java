/**
 * 
 */
package com.fplws.services.payment.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpl.common.exception.CicsProgramException;
import com.fpl.common.exception.DataAccessException;
import com.fpl.webservices.data.Payment;
import com.fpl.webservices.data.PaymentOption;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.mf.payonlinek97a.model.CUOBI105Operation;
import com.fplws.mf.payonlinek97a.model.InputRequest;
import com.fplws.mf.payonlinek97a.model.JsonInputarealog;
import com.fplws.mf.payonlinek97a.model.OutputResponse;
import com.fplws.mf.payonlinek97a.service.PayOnline;
import com.fplws.mf.rcsInformationk359.service.RscInformationService;
import com.fplws.services.payment.utility.HeaderUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.resource.ResourceException;

/**
 * @author nxm01hn
 *
 */
@Service("paymentServiceDao")
public class PaymentServiceDAO {

	@Autowired
	PayOnline payOnline;

	@Autowired
	RscInformationService rscInformationService;

	@Autowired
	HeaderUtility headerUtility;

	private static Logger logger = LogFactory.getLogger(PaymentServiceDAO.class);

	public PaymentOption getPaymentOption(JsonInputarealog json_inputarealog) {

		logger.info("PaymentServiceDAO | getPaymentOption Start for Account number - "
				+ json_inputarealog.getBillaccount());

		InputRequest request = new InputRequest();
		CUOBI105Operation CUOBI105Operation = new CUOBI105Operation();

		PaymentOption payment = new PaymentOption();

		CUOBI105Operation.setJsonInputarealog(json_inputarealog);
		request.setCUOBI105Operation(CUOBI105Operation);

		try {
			logger.info("PaymentServiceDAO | getPaymentOption | calling MF transaction K97A to get payment options for  accountNumber=[{}]"
					, json_inputarealog.getBillaccount());

			payment = payOnline.getPaymentOption(request, headerUtility.getHeaders());
			logger.info(
					"PaymentServiceDAO | getPaymentOption | MF_K97A_GET_PAYMENT_OPTIONS_SUCCESS for accountNumber=[{}]"
					, json_inputarealog.getBillaccount());
		} catch (ApiServiceException e) {
			logger.error("PaymentServiceDAO | getPaymentOption | MF_K97A_GET_PAYMENT_OPTIONS_FAILED Error communicating with MF K971a (CUOBI105)" +
					" for accountNumber=[{}], errorCode=[{}], Exception=[{}]", json_inputarealog.getBillaccount(), e.getErrorCode(), e.getMessage());
			throw new ApiServiceException(e);
		} catch (Exception e) {
			logger.error("PaymentServiceDAO | getPaymentOption | MF_K97A_GET_PAYMENT_OPTIONS_FAILED_OTHER Error communicating with MF K971a (CUOBI105)" +
					" for accountNumber=[{}], Exception=[{}]", json_inputarealog.getBillaccount(), e.getMessage());
			throw new ApiServiceException(e);
		}
		return payment;
	}

	public Payment getScheduledPayment(JsonInputarealog json_inputarealog) {
		// TODO Auto-generated method stub
		logger.info("getScheduledPayment | PaymentServiceDAO | getScheduledPayment Start for accountNumber=[{}]",
				json_inputarealog.getBillaccount());
		InputRequest inputRequest = new InputRequest();

		CUOBI105Operation cuobi105Operation = new CUOBI105Operation();
		cuobi105Operation.setJsonInputarealog(json_inputarealog);
		inputRequest.setCUOBI105Operation(cuobi105Operation);


		Payment scheduledPayment = null;
		try {
			logger.info(
					"getScheduledPayment | PaymentServiceDAO | getScheduledPayment | calling MF transaction K97A " +
							"get schedule payment for accountNumber=[{}]", json_inputarealog.getBillaccount());
			scheduledPayment = payOnline.invokePOLRequest(inputRequest, headerUtility.getHeaders());
			logger.info(
					"getScheduledPayment | PaymentServiceDAO | getScheduledPayment | MF_K97A_GET_SCHEDULED_PAYMENT_SUCCESS " +
							" for accountNumber=[{}]", json_inputarealog.getBillaccount());
		} catch (ApiServiceException e) {
			logger.logError(
					"getScheduledPayment_error | PaymentServiceDAO | getScheduledPayment | MF_K97A_GET_SCHEDULED_PAYMENT_FAILED" +
							"  Error in get schedule payment for accountNumber=[{}], errorCode=[{}], Exception=[{}]", json_inputarealog.getBillaccount(),
					e.getErrorCode(), e.getMessage());
			throw new ApiServiceException(e);
		} catch (Exception e) {
			logger.logError(
					"getScheduledPayment_error | PaymentServiceDAO | getScheduledPayment | MF_K97A_GET_SCHEDULED_PAYMENT_FAILED_OTHER" +
							"  Error in get schedule payment for accountNumber=[{}], Exception=[{}]", json_inputarealog.getBillaccount(), e.getMessage());
			throw new ApiServiceException(e);
		}

		return scheduledPayment;
	}

	public OutputResponse createUpdateDeleteSchedulePayment(JsonInputarealog json_inputarealog) {
		logger.info("createUpdateDeleteSchedulePayment | PaymentServiceDAO | createUpdateDeleteSchedulePayment Start for accountNumber=[{}], action=[{}]"
				, json_inputarealog.getBillaccount(), json_inputarealog.getAction());

		InputRequest inputRequest = new InputRequest();
		CUOBI105Operation cuobi105Operation = new CUOBI105Operation();
		cuobi105Operation.setJsonInputarealog(json_inputarealog);
		inputRequest.setCUOBI105Operation(cuobi105Operation);

		OutputResponse outputResponse = null;
		try {
			logger.info(
					"createUpdateDeleteSchedulePayment | PaymentServiceDAO | createUpdateDeleteSchedulePayment | calling MF transaction K97A  " +
							" for modify schedule payment for accountNumber=[{}], action=[{}]", json_inputarealog.getBillaccount(), json_inputarealog.getAction());
			outputResponse = payOnline.getOutputResponse(inputRequest, headerUtility.getHeaders());
			logger.info("createUpdateDeleteSchedulePayment | PaymentServiceDAO | createUpdateDeleteSchedulePayment | MF_K97A_MODIFY_SCHEDULED_PAYMENT_SUCCESS " +
					" for accountNumber=[{}], action=[{}]", json_inputarealog.getBillaccount(), json_inputarealog.getAction());
		} catch (ApiServiceException e) {
			logger.logError(
					"createUpdateDeleteSchedulePayment_error | PaymentServiceDAO | createUpdateDeleteSchedulePayment | MF_K97A_MODIFY_SCHEDULED_PAYMENT_FAILED " +
							"  Error in modify schedule payment for accountNumber=[{}], action=[{}], errorCode=[{}], Exception=[{}]", json_inputarealog.getBillaccount(),
					json_inputarealog.getAction(), e.getErrorCode(), e.getMessage());
			throw new ApiServiceException(e);
		} catch (Exception e) {
			logger.logError(
					"createUpdateDeleteSchedulePayment_error | PaymentServiceDAO | createUpdateDeleteSchedulePayment | MF_K97A_MODIFY_SCHEDULED_PAYMENT_FAILED_OTHER " +
							"  Error in modify schedule payment for accountNumber=[{}], action=[{}], Exception=[{}]", json_inputarealog.getBillaccount(),
					json_inputarealog.getAction(), e.getMessage());
			throw new ApiServiceException(e);
		}

		return outputResponse;
	}

}
