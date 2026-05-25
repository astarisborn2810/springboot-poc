FROM public.ecr.aws/lambda/java:25

COPY target/*-aws.jar ${LAMBDA_TASK_ROOT}/lib/app.jar

CMD ["com.pearl.payroll.lambda.datatyperouter.DatatypeRouterHandler::handleRequest"]
