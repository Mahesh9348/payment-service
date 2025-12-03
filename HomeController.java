package com.fplws.services.payment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* WebController 
* Base Controller for /
*
* @author  Ritesh Choudhary
* @version 1.0
* @ Auto Generated Project Kickstart
*/

@RestController
public class HomeController {

    Logger logger = LoggerFactory.getLogger(HomeController.class);


    @GetMapping("/")
    public String testService() {
		String successOutput = "Service working for Project - fpl-payment-services.";
        return successOutput;
    }

}
