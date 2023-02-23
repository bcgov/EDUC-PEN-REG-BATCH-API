envValue=$1
APP_NAME=$2
PEN_NAMESPACE=$3
COMMON_NAMESPACE=$4
APP_NAME_UPPER=${APP_NAME^^}
TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca

SOAM_KC_LOAD_USER_ADMIN=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)

DB_JDBC_CONNECT_STRING=$(oc -n $PEN_NAMESPACE-$envValue -o json get configmaps ${APP_NAME}-${envValue}-setup-config | sed -n 's/.*"DB_JDBC_CONNECT_STRING": "\(.*\)",/\1/p')
DB_PWD=$(oc -n $PEN_NAMESPACE-$envValue -o json get configmaps ${APP_NAME}-${envValue}-setup-config | sed -n "s/.*\"DB_PWD_${APP_NAME_UPPER}\": \"\(.*\)\",/\1/p")
DB_USER=$(oc -n $PEN_NAMESPACE-$envValue -o json get configmaps "${APP_NAME}"-"${envValue}"-setup-config | sed -n "s/.*\"DB_USER_${APP_NAME_UPPER}\": \"\(.*\)\",/\1/p")
SPLUNK_TOKEN=$(oc -n $PEN_NAMESPACE-$envValue -o json get configmaps "${APP_NAME}"-"${envValue}"-setup-config | sed -n "s/.*\"SPLUNK_TOKEN_${APP_NAME_UPPER}\": \"\(.*\)\"/\1/p")
NATS_CLUSTER=educ_nats_cluster
NATS_URL="nats://nats.${COMMON_NAMESPACE}-${envValue}.svc.cluster.local:4222"

echo Fetching SOAM token
TKN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=$SOAM_KC_LOAD_USER_ADMIN" \
  -d "password=$SOAM_KC_LOAD_USER_PASS" \
  -d "grant_type=password" \
  "https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" | jq -r '.access_token')

echo
echo Retrieving client ID for pen-reg-batch-api-service
PRB_CLIENT_ID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="pen-reg-batch-api-service")' | jq -r '.id')

echo
echo Removing PEN REG BATCH API client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$PRB_CLIENT_ID" \
  -H "Authorization: Bearer $TKN"

echo
echo Creating client pen-reg-batch-api-service
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"clientId\" : \"pen-reg-batch-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\",  \"WRITE_STUDENT\", \"role_list\", \"profile\", \"roles\", \"READ_STUDENT\",\"GET_NEXT_PEN_NUMBER\", \"READ_SCHOOL\", \"READ_PEN_COORDINATOR\", \"email\",\"READ_STUDENT_CODES\",\"READ_VALIDATION_CODES\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo
echo Retrieving client ID for pen-reg-batch-api-service
PRB_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="pen-reg-batch-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for pen-reg-batch-api-service
PRB_APIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$PRB_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')

echo
echo Writing scope READ_PEN_REQUEST_BATCH
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Pen Request Batch Data\",\"id\": \"READ_PEN_REQUEST_BATCH\",\"name\": \"READ_PEN_REQUEST_BATCH\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_PEN_REQUEST_BATCH
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Pen Request Batch Data\",\"id\": \"WRITE_PEN_REQUEST_BATCH\",\"name\": \"WRITE_PEN_REQUEST_BATCH\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope DELETE_PEN_REQUEST_BATCH
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Delete Pen Request Batch Data\",\"id\": \"DELETE_PEN_REQUEST_BATCH\",\"name\": \"DELETE_PEN_REQUEST_BATCH\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope READ_PEN_REQUEST_BATCH_BLOB
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Pen Request Batch Source data\",\"id\": \"READ_PEN_REQUEST_BATCH_BLOB\",\"name\": \"READ_PEN_REQUEST_BATCH_BLOB\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_PEN_REQUEST_BATCH_BLOB
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Update Pen Request Batch Source data\",\"id\": \"WRITE_PEN_REQUEST_BATCH_BLOB\",\"name\": \"WRITE_PEN_REQUEST_BATCH_BLOB\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope READ_PEN_REQUEST_BATCH_MACRO
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Pen Request Batch Macro Data\",\"id\": \"READ_PEN_REQUEST_BATCH_MACRO\",\"name\": \"READ_PEN_REQUEST_BATCH_MACRO\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_PEN_REQUEST_BATCH_MACRO
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Pen Request Batch Macro Data\",\"id\": \"WRITE_PEN_REQUEST_BATCH_MACRO\",\"name\": \"WRITE_PEN_REQUEST_BATCH_MACRO\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope PEN_REQUEST_BATCH_NEW_PEN_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Start Issue New Pen Saga\",\"id\": \"PEN_REQUEST_BATCH_NEW_PEN_SAGA\",\"name\": \"PEN_REQUEST_BATCH_NEW_PEN_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope PEN_REQUEST_BATCH_USER_MATCH_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Start Processing user match a student to a pen request from psi or school\",\"id\": \"PEN_REQUEST_BATCH_USER_MATCH_SAGA\",\"name\": \"PEN_REQUEST_BATCH_USER_MATCH_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope PEN_REQUEST_BATCH_READ_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Fetch Saga information based on saga id.\",\"id\": \"PEN_REQUEST_BATCH_READ_SAGA\",\"name\": \"PEN_REQUEST_BATCH_READ_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope PEN_REQUEST_BATCH_WRITE_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Saga information.\",\"id\": \"PEN_REQUEST_BATCH_WRITE_SAGA\",\"name\": \"PEN_REQUEST_BATCH_WRITE_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope PEN_REQUEST_BATCH_READ_HISTORY
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read PEN request batch history records.\",\"id\": \"PEN_REQUEST_BATCH_READ_HISTORY\",\"name\": \"PEN_REQUEST_BATCH_READ_HISTORY\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope PEN_REQUEST_BATCH_ARCHIVE_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Start archive and return saga.\",\"id\": \"PEN_REQUEST_BATCH_ARCHIVE_SAGA\",\"name\": \"PEN_REQUEST_BATCH_ARCHIVE_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope PEN_REQUEST_BATCH_REPOST_SAGA
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Start repost reports saga.\",\"id\": \"PEN_REQUEST_BATCH_REPOST_SAGA\",\"name\": \"PEN_REQUEST_BATCH_REPOST_SAGA\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
   Buffer_Chunk_Size 5MB
   Buffer_Max_Size 5MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"

THREADS_MIN_SUBSCRIBER=4
THREADS_MAX_SUBSCRIBER=8
SAGAS_MAX_PENDING=100
SAGAS_MAX_PARALLEL=100

SOFT_DELETED_RETENTION_DAYS=365
SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PENWEB_PEN_WEB_BLOBS_CRON="-"
SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON="0 0/10 * * * *"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="0 0/1 * * * *"
SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON="0/10 * * * * *"
SCHEDULED_JOBS_PURGE_SOFT_DELETED_RECORDS_CRON="@midnight"
SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON="@midnight"
SCHEDULED_JOBS_MARK_PROCESSED_BATCHES_ACTIVE_CRON="0 0/1 * * * *"
SCHEDULED_JOBS_PROCESS_LOADED_BATCHES_FOR_REPEATS_CRON="0 0/2 * * * *"
if [ "$envValue" = "tools" ]; then
  PEN_COORDINATOR_EMAIL=omprakashmishra7234@gmail.com
  SOFT_DELETED_RETENTION_DAYS=20
fi

if [ "$envValue" = "dev" ]; then
  PEN_COORDINATOR_EMAIL=aditya.sharma@gov.bc.ca
  SOFT_DELETED_RETENTION_DAYS=2
fi

if [ "$envValue" = "test" ]; then
  PEN_COORDINATOR_EMAIL=Gurvinder.J.Bhatia@gov.bc.ca
  SOFT_DELETED_RETENTION_DAYS=2
fi
# when it will be time for go live the value of SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON will be set to "0 0/10 18-23,00-05 * * *" so that it runs from 6PM to 6AM
if [ "$envValue" = "prod" ]; then
  PEN_COORDINATOR_EMAIL=pens.coordinator@gov.bc.ca
  SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PENWEB_PEN_WEB_BLOBS_CRON="0 0/10 7-17 * * *"
  SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON="0 0/10 18-23,00-05 * * *"
  SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="0 0/1 * * * *"
  SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON="0/10 * * * * *"
  SCHEDULED_JOBS_PURGE_SOFT_DELETED_RECORDS_CRON="@midnight"
  SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON="@midnight"
  SCHEDULED_JOBS_MARK_PROCESSED_BATCHES_ACTIVE_CRON="0 0/1 * * * *"
  SCHEDULED_JOBS_PROCESS_LOADED_BATCHES_FOR_REPEATS_CRON="0 0/2 * * * *"
fi

echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME="$DB_USER" --from-literal=ORACLE_PASSWORD="$DB_PWD" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON="$SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PENWEB_PEN_WEB_BLOBS_CRON="$SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PENWEB_PEN_WEB_BLOBS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON_LOCK_AT_LEAST_FOR="540s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON_LOCK_AT_MOST_FOR="580s" --from-literal=NATS_URL="$NATS_URL" --from-literal=NATS_CLUSTER="$NATS_CLUSTER" --from-literal=SPRING_JPA_SHOW_SQL="false" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="55s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="57s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON="$SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON_LOCK_AT_LEAST_FOR="8s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON_LOCK_AT_MOST_FOR="8s" --from-literal=CLIENT_ID="pen-reg-batch-api-service" --from-literal=CLIENT_SECRET="$PRB_APIServiceClientSecret" --from-literal=STUDENT_API_URL="http://student-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=SCHOOL_API_URL="http://school-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/schools" --from-literal=TOKEN_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" --from-literal=URL_REDIS="redis.$PEN_NAMESPACE-$envValue.svc.cluster.local:6379" --from-literal=REPEAT_TIME_WINDOW_K12=0 --from-literal=REPEAT_TIME_WINDOW_PSI=0 --from-literal=SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=16 --from-literal=SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=2 --from-literal=PEN_SERVICES_API_URL="http://pen-services-api-master.$PEN_NAMESPACE-$envValue.svc.cluster.local:8080" --from-literal=HIBERNATE_SQL_PARAM_LOG_LEVEL=INFO --from-literal=PURGE_RECORDS_SAGA_AFTER_DAYS=365 --from-literal=SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON="$SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON" --from-literal=SOFT_DELETED_RETENTION_DAYS="$SOFT_DELETED_RETENTION_DAYS" --from-literal=SCHEDULED_JOBS_PURGE_SOFT_DELETED_RECORDS_CRON="$SCHEDULED_JOBS_PURGE_SOFT_DELETED_RECORDS_CRON" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=HOLD_BATCHES_EQUAL_OR_LARGER_THAN=2500 --from-literal=PEN_COORDINATOR_EMAIL="$PEN_COORDINATOR_EMAIL" --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=PEN_COORDINATOR_MAILING_ADDRESS="Ministry of Education and Child Care, Data Collection Unit PO Box 9170, Stn. Prov. Govt, Victoria BC, V8W 9H7" --from-literal=PEN_COORDINATOR_TELEPHONE="(250)356-8020" --from-literal=PEN_COORDINATOR_FACSIMILE="(250)953-0450" --from-literal=SCHEDULED_JOBS_MARK_PROCESSED_BATCHES_ACTIVE_CRON="$SCHEDULED_JOBS_MARK_PROCESSED_BATCHES_ACTIVE_CRON" --from-literal=SCHEDULED_JOBS_MARK_PROCESSED_BATCHES_ACTIVE_CRON_LOCK_AT_LEAST_FOR="50s" --from-literal=SCHEDULED_JOBS_MARK_PROCESSED_BATCHES_ACTIVE_LOCK_AT_MOST_FOR="55s" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_BATCHES_FOR_REPEATS_CRON="$SCHEDULED_JOBS_PROCESS_LOADED_BATCHES_FOR_REPEATS_CRON" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_BATCHES_FOR_REPEATS_CRON_LOCK_AT_LEAST_FOR="100s" --from-literal=SCHEDULED_JOBS_PROCESS_LOADED_BATCHES_FOR_REPEATS_CRON_LOCK_AT_MOST_FOR="110s" --from-literal=SKIP_VALIDATION_FOR_DISTRICT_CODES="102,104" --from-literal=STUDENT_THRESHOLD_GENERATE_PDF="500" --from-literal=THREADS_MIN_SUBSCRIBER="$THREADS_MIN_SUBSCRIBER" --from-literal=THREADS_MAX_SUBSCRIBER="$THREADS_MAX_SUBSCRIBER" --from-literal=SAGAS_MAX_PENDING="$SAGAS_MAX_PENDING" --from-literal=SAGAS_MAX_PARALLEL="$SAGAS_MAX_PARALLEL" --dry-run -o yaml | oc apply -f -

echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc -n $PEN_NAMESPACE-$envValue set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -

echo Removing un-needed config entries
oc -n "$PEN_NAMESPACE"-"$envValue" set env dc/"$APP_NAME"-$SOAM_KC_REALM_ID KEYCLOAK_PUBLIC_KEY-
