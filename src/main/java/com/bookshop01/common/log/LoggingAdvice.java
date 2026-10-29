package com.bookshop01.common.log;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class LoggingAdvice {
	private static final Logger logger = LoggerFactory.getLogger(LoggingAdvice.class);

	// Output target method parameter information
	@Before("execution(* com.bookshop01.*.service.*.*(..)) || execution(* com.bookshop01.*.dao.*.*(..))")
	public void startLog(JoinPoint jp) {
		logger.info("-------------------------------------");
		logger.info("-------------------------------------");

		// Retrieve all passed parameters as an Object array
		logger.info("1:" + Arrays.toString(jp.getArgs()));

		// Get the type of this Advice
		logger.info("2:" + jp.getKind());

		// Get information about the method of the target object being executed
		logger.info("3:" + jp.getSignature().getName());

		// Get the target object
		logger.info("4:" + jp.getTarget().toString());

		// Get the object executing the Advice
		logger.info("5:" + jp.getThis().toString());
	}

	@After("execution(* com.bookshop01.*.service.*.*(..)) || execution(* com.bookshop01.*.dao.*.*(..))")
	public void after(JoinPoint jp) {
		logger.info("-------------------------------------");
		logger.info("-------------------------------------");

		// Retrieve all passed parameters as an Object array
		logger.info("1:" + Arrays.toString(jp.getArgs()));

		// Get the type of this Advice
		logger.info("2:" + jp.getKind());

		// Get information about the method of the target object being executed
		logger.info("3:" + jp.getSignature().getName());

		// Get the target object
		logger.info("4:" + jp.getTarget().toString());

		// Get the object executing the Advice
		logger.info("5:" + jp.getThis().toString());
	}

	// Measure execution time of the target method
	@Around("execution(* com.bookshop01.*.service.*.*(..)) || execution(* com.bookshop01.*.dao.*.*(..))")
	public Object timeLog(ProceedingJoinPoint pjp) throws Throwable {
		long startTime = System.currentTimeMillis();
		logger.info(Arrays.toString(pjp.getArgs()));

		// Execute the actual target method
		Object result = pjp.proceed();

		long endTime = System.currentTimeMillis();
		// Output method name and execution duration
		logger.info(pjp.getSignature().getName() + " : " + (endTime - startTime));
		logger.info("=============================================");

		// Return the result of the target method
		return result;
	}
}