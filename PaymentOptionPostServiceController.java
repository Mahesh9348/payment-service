package com.fplws.services.payment.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fpl.common.data.ResponseMessage;
import com.fplws.common.framework.fplcommonframework.exceptions.ApiServiceException;
import com.fplws.common.framework.fplcommonframework.response.ResponseHttpStatus;
import com.fplws.services.payment.utility.ExceptionMessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.fplws.common.framework.fplcommonframework.response.FPLWSResponse;
import com.fplws.common.framework.logging.LogFactory;
import com.fplws.common.framework.logging.Logger;
import com.fplws.services.payment.service.PaymentOptionPostService;
import java.util.List;

/**
 * @author nxm01hn Rest Service to retrieve payment option for Account number
 * provided
 *
 */

@RestController
@CrossOrigin
@RequestMapping("/resources/account/{parentId}/payment-postoption")
public class PaymentOptionPostServiceController {

    private static Logger logger = LogFactory.getLogger(PaymentOptionPostServiceController.class);

    @Autowired
    PaymentOptionPostService paymentOptionService;

    @Autowired
    ExceptionMessageHelper exceptionHelper;

    @SuppressWarnings("rawtypes")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public FPLWSResponse paymentOptionPostServiceController(HttpServletRequest request, HttpServletResponse response,
                                                            @PathVariable("parentId") String accountNumber) {

        logger.info("PaymentOption_post | paymentOptionPostServiceController Start | Account number - " + accountNumber);
        String mediaChannel = request.getParameter("mediaChannel");
        try {
            return paymentOptionService.getPaymentOption(accountNumber);
        } catch (ApiServiceException exp) {
            logger.error("PaymentOption_post | PaymentOptionPostServiceController | getPaymentOption | PaymentServiceException for " + accountNumber);
            List<ResponseMessage> responseMessages = exp.getResponseMessages();
            exceptionHelper.getMessage(responseMessages, ExceptionMessageHelper.PYOPTIONMSG);
            return exceptionHelper.returnCustomResponse.apply(mediaChannel, responseMessages);
        }
    }

}
