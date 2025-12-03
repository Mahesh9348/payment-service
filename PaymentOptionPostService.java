package com.fplws.services.payment.service;

import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;

/**
 * @author nxm01hn
 *
 */
public interface PaymentOptionPostService {

	@SuppressWarnings("rawtypes")
	FPLWSResponse getPaymentOption(String accountNumber);

	
}
