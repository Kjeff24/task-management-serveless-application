# spring-boot-serveless serverless API

### Get Meta-data on your cognito user pool
```
aws cognito-idp describe-user-pool-client --user-pool-id <your-user-pool-id> --client-id <your-client-id>
```

### Authentication your for ID_TOKEN
```
aws cognito-idp initiate-auth --auth-flow USER_PASSWORD_AUTH --client-id <your-client-id> --auth-parameters USERNAME=<your-username>,PASSWORD=<your-password> --query 'AuthenticationResult.IdToken' --output text
```

### Get your basic credentials
```
echo -n "<your-client-id>:<your-client-secret>" | base64
```

```
curl --location --request POST 'https://<your-user-pool-domain>.auth.<your-aws-region>.amazoncognito.com/oauth2/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header 'Authorization: Basic <your-basic-credentials>' \
--data-urlencode 'grant_type=authorization_code' \
--data-urlencode 'client_id=<your-client-id>' \
--data-urlencode 'code=<your-code>' \
--data-urlencode 'redirect_uri=http://localhost:4200' | jq .

```


