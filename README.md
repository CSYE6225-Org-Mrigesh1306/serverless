# Amazon Simple Email Service (SES) Configuration

## Implement Lambda Function
* Lambda function will be invoked by the SNS notification. Lambda function is responsible for sending emails to the user.
* As a user, I should receive an email to verify my email address.
* As a user, I should NOT receive duplicate email messages.
* Track emails being sent in DynamoDB so that user does not get duplicate emails even if duplicate messages are posted to the SNS topic.

## Implement CI/CD Pipeline for Lambda Function
* Using the same principles as in previous assignments, implement continuous deployment for Lambda functions. Every commit to the serverless repository should trigger the build and deployment of your updates function to AWS Lambda.


