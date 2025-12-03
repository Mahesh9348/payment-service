/**
 * 
 */
package com.fplws.services.payment.exception;

import java.util.ArrayList;
import java.util.List;

import com.fpl.common.data.MessageType;
import com.fpl.common.data.ResponseMessage;
import com.fpl.common.exception.BaseException;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;

/**
 * @author NXM01HN
 *
 */
public class PaymentServiceException extends BaseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String errorCode;
	protected List<ResponseMessage> responseMessages;
	String errorMessage;

	public PaymentServiceException(String errorCode, String errorMessage, ResponseHttpStatus notFound) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;

	}

	public PaymentServiceException(String errorCode, String errorMessage, ResponseHttpStatus notFound, boolean b) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public PaymentServiceException(Throwable cause) {
		this(cause, getErrorCode(cause), getErrorMessage(cause), getResponseMessages(cause));
		
	}

	public PaymentServiceException(Throwable cause, String errorCode, String errorMessage,
			List<ResponseMessage> responseMessages) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.responseMessages = responseMessages;
	}

	public List<ResponseMessage> getResponseMessages() {
		// TODO Auto-generated method stub
		if (responseMessages == null || responseMessages.isEmpty()) {
			if (responseMessages == null) {
				responseMessages = new ArrayList<ResponseMessage>();
			}

			responseMessages.add(new ResponseMessage(errorCode, errorMessage, MessageType.ERROR));
		}

		return responseMessages;
	}

	
	protected static List<ResponseMessage> getResponseMessages(Throwable cause) {
		List<ResponseMessage> messages = null;
		
		if (cause instanceof BaseException) {
			BaseException ex = (BaseException)cause;
			messages = ex.getResponseMessages();
		}
		
		return messages;
	}
	
	protected static String getErrorCode(Throwable cause) {
		String errorCode = null;
		
		if (cause instanceof BaseException) {
			BaseException ex = (BaseException)cause;
			errorCode = ex.getErrorCode();
		}
		
		return errorCode;
	}
	
	protected static String getErrorMessage(Throwable cause) {
		String errorMessage = null;
		
		if (cause != null) {
			errorMessage = cause.getMessage();
		}
		
		return errorMessage;
	}
}
