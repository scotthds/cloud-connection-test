# Java Integration Test

## Example usage

- java -cp target/cloud-load-test-1.0-SNAPSHOT.jar::./target/dependency/* -Duser=user -Dpassword=password -Dkeyspace=keyspace -DpathToCreds=/secure-connect-bundle.zip -Ddbwriter=false ConnectDatabaseTest

## Variables Consumed
* user - user for the CaaS database
* password - password for the CaaS database
* keyspace - keyspace of the CaaS database
* pathToCreds - path to downloaded creds.zip
* dbwriter - write new pet ids only or read 5 records and add more heart rates to those pet ids
