package com.fplws.services.payment.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fpl.common.data.IdentifiableBuilder;
import com.fpl.webservices.data.PaymentOption;
import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.mf.payonlinek97a.model.JsonInputarealog;
import com.fplws.services.payment.dao.PaymentServiceDAO;
import com.fplws.services.payment.service.PaymentOptionPostService;
import com.fplws.services.payment.utility.UpdateBlankInput;

/**
 * @author nxm01hn
 *
 */
@Service("paymentOptionService")
public class PaymentOptionPostServiceImpl implements PaymentOptionPostService {

	@Autowired
	private PaymentServiceDAO paymentServiceDao;
	
	@Autowired
	UpdateBlankInput updateBlankInput;

	private static Logger logger = LogFactory.getLogger(PaymentOptionPostServiceImpl.class);

	@SuppressWarnings("rawtypes")
	@Override
	public FPLWSResponse getPaymentOption(String accountNumber) {
		// TODO Auto-generated method stub
		PaymentOption paymentOption = null;

		logger.info("PaymentOption | PaymentOptionPostServiceImpl Start | Account number - " + accountNumber);
		paymentOption = getPaymentOptions(accountNumber);		
		logger.info("PaymentOption | PaymentOptionPostServiceImpl End | Account number - " + accountNumber);
		return FPLWSResponse.assemble().build(ResponseHttpStatus.OK, paymentOption);
	}
	
	
	private PaymentOption getPaymentOptions(String accountNumber) {
		
		JsonInputarealog json_inputarealog = new JsonInputarealog();
		json_inputarealog = updateBlankInput.updateBlankInput(json_inputarealog);
		json_inputarealog.setBillaccount(accountNumber);
		json_inputarealog.setAction("VALIDATE");
		json_inputarealog.setRequest("PAYMENT");
		
		return paymentServiceDao.getPaymentOption(json_inputarealog);
	}

}
