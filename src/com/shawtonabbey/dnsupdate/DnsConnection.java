package com.shawtonabbey.dnsupdate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeInfo;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.GetHostedZoneRequest;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

public class DnsConnection {

	
	public static void main(String args[]) throws Exception {
		
		if (args.length != 5 || !"update".equals(args[0]))
		{
			showUsage();
			return;
		}
		
		String action = args[0];
		String dnsZoneId = args[1];
		String type = args[2];
		String dnsName = args[3];
		String ipAddress = args[4];
		
		
		updateDns(dnsZoneId, type, dnsName, ipAddress);
	}
	
	private static void showUsage() {
		System.out.println("update zoneId type dnsName ipAddress");
	}
	
	private static void updateDns(String dnsZoneId, String type, String dnsName, String value) throws IOException {
		
		Properties prop = new Properties();
		
		FileInputStream input = new FileInputStream("config.prop");
		
		prop.load(input);

		String accessKey = prop.getProperty("accessKey");
		String secretKey = prop.getProperty("secretKey");
		
		BasicAWSCredentials awsCredentials;

	    String ROUT53_HOSTED_ZONE_ID = dnsZoneId;
		
		awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

		
		AmazonRoute53 client;
		client = AmazonRoute53ClientBuilder
			.standard()
			.withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
			.withRegion(Regions.DEFAULT_REGION)
			.build();
		
		
		HostedZone hostedZone = client.getHostedZone(new GetHostedZoneRequest(ROUT53_HOSTED_ZONE_ID)).getHostedZone();

	    ListResourceRecordSetsRequest requestResourceList = new ListResourceRecordSetsRequest()
                .withHostedZoneId(hostedZone.getId());
	    
        ListResourceRecordSetsResult resourceResults = client.listResourceRecordSets(requestResourceList);
        List<ResourceRecordSet>   rs = resourceResults.getResourceRecordSets();
        List<Change> changes = new ArrayList<Change>();
        
        for(ResourceRecordSet record : rs)
        {
        	
            if(record.getType().equals(type) 
            		&& dnsName.equals(record.getName()))
            {
            	System.out.println("Found match: " + record.getName()); 
            	
                List<ResourceRecord> resourceRecords = new ArrayList<ResourceRecord>();
                ResourceRecord resourceRecord = new ResourceRecord();

                if ("TXT".equals(type)) 
                	resourceRecord.setValue("\"" + value + "\"");
                else 
                	resourceRecord.setValue(value);
                resourceRecords.add(resourceRecord);
                record.setResourceRecords(resourceRecords);
                Change change = new Change(ChangeAction.UPSERT, record);
                changes.add(change);
            }
        }

        if(changes.size()>0)
        {
            ChangeBatch changeBatch = new ChangeBatch(changes);
            ChangeResourceRecordSetsRequest changeResourceRecordSetsRequest = new ChangeResourceRecordSetsRequest()
                    .withHostedZoneId(ROUT53_HOSTED_ZONE_ID)
                    .withChangeBatch(changeBatch);
            ChangeResourceRecordSetsResult re = client.changeResourceRecordSets(changeResourceRecordSetsRequest);
            
            ChangeInfo ci = re.getChangeInfo();
            System.out.println("Requesting update: " + ci.getId() + " " + ci.getStatus() + " " + ci.getComment());
        }
        else {
        	System.out.println("Record not found.");
        }
	}
	
}
