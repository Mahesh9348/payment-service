package com.fplws.services.payment.service;

import com.fpl.webservices.data.transaction.PaymentRequest;
import com.fplws.common.framework.fplcommonframework.beans.UserInfo;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;

/**
 * @author nxm01hn
 *
 */
public interface PaymentService {

	@SuppressWarnings("rawtypes")
	FPLWSResponse getScheduledPayment(String accountNumber) throws ApiServiceException ;

	@SuppressWarnings("rawtypes")
	FPLWSResponse updateScheduledPayment(String accountNumber, PaymentRequest paymentRequest, UserInfo userInfo) throws ApiServiceException ;

	@SuppressWarnings("rawtypes")
	FPLWSResponse createScheduledPayment(String accountNumber, PaymentRequest paymentRequest, UserInfo userInfo) throws ApiServiceException ;

	@SuppressWarnings("rawtypes")
	FPLWSResponse deleteScheduledPayment(String accountNumber, PaymentRequest paymentRequest, UserInfo userInfo) throws ApiServiceException ;

}
