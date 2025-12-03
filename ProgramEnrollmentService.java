package com.fplws.services.payment.service;


import com.fpl.webservices.data.transaction.ProgramEnrollmentRequest;
import com.fplws.common.framework.fplcommonframework.beans.UserInfo;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;

/**
 * @author nxm01hn
 *
 */
public interface ProgramEnrollmentService {

	@SuppressWarnings("rawtypes")
	FPLWSResponse createEnrollment(ProgramEnrollmentRequest<?> programEnrollmentRequest, String mediaChannel, UserInfo userInfo) throws ApiServiceException;

	@SuppressWarnings("rawtypes")
	FPLWSResponse retrieveEnrollment(String formatBANumber) throws ApiServiceException;

	@SuppressWarnings("rawtypes")
	FPLWSResponse updateEnrollment(ProgramEnrollmentRequest<?> programEnrollmentRequest, String  mediaChannel, UserInfo userInfo) throws ApiServiceException;

	@SuppressWarnings("rawtypes")
	FPLWSResponse deleteEnrollment(ProgramEnrollmentRequest<?> programEnrollmentRequest, UserInfo userInfo) throws ApiServiceException;

	@SuppressWarnings("rawtypes")
	FPLWSResponse validateEnrollment(String formatBANumber);


}
