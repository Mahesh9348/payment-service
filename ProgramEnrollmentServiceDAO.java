/**
 * 
 */
package com.fplws.services.payment.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpl.common.exception.CicsProgramException;
import com.fpl.common.exception.DataAccessException;
import com.fpl.webservices.data.Payment;
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
@Service("programEnrollmentServiceDao")
public class ProgramEnrollmentServiceDAO {

	private static Logger logger = LogFactory.getLogger(ProgramEnrollmentServiceDAO.class);

	@Autowired
	PayOnline payOnline;

	@Autowired
	RscInformationService rscInformationService;

	@Autowired
	HeaderUtility headerUtility;

	public void createUpdateDeleteEnrollment(JsonInputarealog json_inputarealog) {

		logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceDAO | createUpdateDeleteEnrollment Start for accountNumber=[{}], , action=[{}]"
				, json_inputarealog.getBillaccount(), json_inputarealog.getAction());

		InputRequest request = new InputRequest();
		CUOBI105Operation CUOBI105Operation = new CUOBI105Operation();

		Payment enroll = new Payment();

		CUOBI105Operation.setJsonInputarealog(json_inputarealog);
		request.setCUOBI105Operation(CUOBI105Operation);

		try {
			logger.info("ProgramEnrollmentService | ProgramEnrollmentServiceDAO | createUpdateDeleteEnrollment |  calling MF transaction K97A  " +
					"for modify enrollment for accountNumber=[{}], action=[{}]", json_inputarealog.getBillaccount(), json_inputarealog.getAction());
			enroll = payOnline.invokePOLRequest(request, headerUtility.getHeaders());
			logger.info(
					"ProgramEnrollmentService | ProgramEnrollmentServiceDAO | createUpdateDeleteEnrollment | MF_K97A_MODIFY_ENROLLMENT_SERVICE_SUCCESS - " +
							" for accountNumber=[{}], action=[{}]", json_inputarealog.getBillaccount(), json_inputarealog.getAction());
		} catch (ApiServiceException e) {
			logger.logError(
					"ProgramEnrollmentService | ProgramEnrollmentServiceDAO | createUpdateDeleteEnrollment | MF_K97A_MODIFY_ENROLLMENT_SERVICE_FAILED " +
							"  Error in modify enrollment for accountNumber=[{}], action=[{}], errorCode=[{}], Exception=[{}]", json_inputarealog.getBillaccount(),
					json_inputarealog.getAction(), e.getErrorCode(), e.getMessage());
			throw new ApiServiceException(e);
		} catch (Exception ex) {
			logger.logError(
					"ProgramEnrollmentService | ProgramEnrollmentServiceDAO | createUpdateDeleteEnrollment | MF_K97A_MODIFY_ENROLLMENT_SERVICE_FAILED_OTHER " +
							"  Error in modify enrollment for accountNumber=[{}], action=[{}], Exception=[{}]", json_inputarealog.getBillaccount(),
					json_inputarealog.getAction(), ex.getMessage());
			throw new ApiServiceException(ex);
		}
	}

	public OutputResponse retrieveEnrollment(JsonInputarealog json_inputarealog) {

		logger.info("ProgramEnrollmentServiceDAO | retrieveEnrollment Start for Account number - "
				+ json_inputarealog.getBillaccount());

		InputRequest request = new InputRequest();
		CUOBI105Operation CUOBI105Operation = new CUOBI105Operation();


		CUOBI105Operation.setJsonInputarealog(json_inputarealog);
		request.setCUOBI105Operation(CUOBI105Operation);
		OutputResponse outputResponse = null;
		try {
			logger.info("ProgramEnrollmentServiceDAO | retrieveEnrollment |  calling MF transaction K97A " +
					"to retrieve enrollment for accountNumber=[{}]", json_inputarealog.getBillaccount());

			outputResponse = payOnline.getOutputResponse(request, headerUtility.getHeaders());
			logger.info("ProgramEnrollmentServiceDAO | retrieveEnrollment | MF_K97A_GET_ENROLLMENT_SERVICE_SUCCESS " +
					" for accountNumber=[{}]", json_inputarealog.getBillaccount());
		} catch (ApiServiceException e) {
			logger.logError("retrieveEnrollment_error | ProgramEnrollmentServiceDAO | retrieveEnrollment | MF_K97A_GET_ENROLLMENT_SERVICE_FAILED " +
							"Error in retrieve enrollment for accountNumber=[{}], errorCode=[{}], errorMessage=[{}]", json_inputarealog.getBillaccount(), e.getErrorCode(),
					e.getMessage());
			throw new ApiServiceException(e);
		} catch (Exception e) {
			logger.logError("retrieveEnrollment_error | ProgramEnrollmentServiceDAO | retrieveEnrollment | MF_K97A_GET_ENROLLMENT_SERVICE_FAILED_OTHER " +
					"Error in retrieve enrollment for accountNumber=[{}]", json_inputarealog.getBillaccount());
			throw new ApiServiceException(e);
		}
		return outputResponse;
	}
}
