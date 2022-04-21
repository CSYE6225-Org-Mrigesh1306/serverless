package com.mrigesh.serverless.serverless;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;

public class EmailNotification implements RequestHandler<SNSEvent, String> {

    private DynamoDB dynamoDB;
    private static String EMAIL_SUBJECT;
    private static final String EMAIL_SENDER = "no-reply@demo.mrigeshdasgupta.me";

    @Override
    public String handleRequest(SNSEvent request, Context context) {

        String message = request.getRecords().get(0).getSNS().getMessage();
        context.getLogger().log("SNS: " + message);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("TokenTable");
        Table userEmailsTable = dynamoDB.getTable("UsernameTokenTable");
        if(table == null) {
            context.getLogger().log("'TokenTable' does not exist");
            return null;
        } else if (request.getRecords() == null) {
            context.getLogger().log("No records in the SNS Event.");
            return null;
        }

        if(userEmailsTable == null) {
            context.getLogger().log("'TokenTable' does not exist");
            return null;
        }

        // get SNS message
        String msgSNS =  request.getRecords().get(0).getSNS().getMessage();
        // requestType, recipientEmail, bookId, bookName, author, link
        List<String> msgInfo = Arrays.asList(msgSNS.split("\\|"));
        StringBuilder emailMsgSB = new StringBuilder();
        StringBuilder url = new StringBuilder();
        url.append(msgInfo.get(2));
        String userEmail = msgInfo.get(1);
        //create token
        String linktoSendUser="https://demo.mrigeshdasgupta.me/verifyUserEmail?email="+userEmail+"&token="+msgInfo.get(2);
        //append to url
        //url.append(token);
        emailMsgSB.append("Hello, Username: ").append(userEmail).append("\n");
        if (msgInfo.get(0).equals("POST")) {
            emailMsgSB.append("Click on this unique link to verify account ").append(linktoSendUser);
            emailMsgSB.insert(0, "Verify.\n\n");
            EMAIL_SUBJECT = "Account Verification Link";
        } else {
            emailMsgSB.insert(0, "Invalid Request.\n\n");
            EMAIL_SUBJECT = "Invalid Request";
        }

        // send email if no duplicate in dynamoDB
        String emailMsg = emailMsgSB.toString();
        Item item = table.getItem("emailID",userEmail);
        if (item == null) {

            PutItemOutcome outcome  = table
                    .putItem(new Item().withPrimaryKey("emailID", userEmail).with("emailmsg", emailMsg));

            long now = Instant.now().getEpochSecond(); // unix time
            long ttl = 60 * 2; // 2 mins in sec
            ttl=(ttl + now); // when object will be expired

            for(int i=0; i<msgInfo.size(); i++){
                context.getLogger().log(msgInfo.get(i));

            }

            outcome  = userEmailsTable
                    .putItem(new Item().withPrimaryKey("emailID", userEmail).with("TimeToLive", ttl).with("Token",Integer.parseInt(msgInfo.get(2))));

            Content content = new Content().withData(emailMsg);
            Body emailBody = new Body().withText(content);
            try {
                AmazonSimpleEmailService emailService =
                        AmazonSimpleEmailServiceClientBuilder.defaultClient();
                SendEmailRequest emailRequest = new SendEmailRequest()
                        .withDestination(new Destination().withToAddresses(msgInfo.get(1)))
                        .withMessage(new Message()
                                .withBody(emailBody)
                                .withSubject(new Content().withCharset("UTF-8").withData(EMAIL_SUBJECT)))
                        .withSource(EMAIL_SENDER);
                emailService.sendEmail(emailRequest);
            } catch (Exception ex) {
                System.out.println("Email not sent");
                context.getLogger().log(ex.getLocalizedMessage());
            }
        }
        else{
            System.out.println("Email has been sent");
        }

        return null;
    }
}
