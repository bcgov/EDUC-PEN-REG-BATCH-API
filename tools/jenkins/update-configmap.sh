envValue=$1
APP_NAME=$2
PEN_NAMESPACE=$3
COMMON_NAMESPACE=$4
APP_NAME_UPPER=${APP_NAME^^}
TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
KCADM_FILE_BIN_FOLDER="/tmp/keycloak-9.0.3/bin"
SOAM_KC=$COMMON_NAMESPACE-$envValue.pathfinder.gov.bc.ca

oc project $COMMON_NAMESPACE-$envValue
SOAM_KC_LOAD_USER_ADMIN=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)

oc project $PEN_NAMESPACE-$envValue
DB_JDBC_CONNECT_STRING=$(oc -o json get configmaps ${APP_NAME}-${envValue}-setup-config | sed -n 's/.*"DB_JDBC_CONNECT_STRING": "\(.*\)",/\1/p')
DB_PWD=$(oc -o json get configmaps ${APP_NAME}-${envValue}-setup-config | sed -n "s/.*\"DB_PWD_${APP_NAME_UPPER}\": \"\(.*\)\",/\1/p")
DB_USER=$(oc -o json get configmaps "${APP_NAME}"-"${envValue}"-setup-config | sed -n "s/.*\"DB_USER_${APP_NAME_UPPER}\": \"\(.*\)\",/\1/p")
SPLUNK_TOKEN=$(oc -o json get configmaps "${APP_NAME}"-"${envValue}"-setup-config | sed -n "s/.*\"SPLUNK_TOKEN_${APP_NAME_UPPER}\": \"\(.*\)\"/\1/p")
NATS_CLUSTER=educ_nats_cluster
NATS_URL="nats://nats.${COMMON_NAMESPACE}-${envValue}.svc.cluster.local:4222"
oc project $PEN_NAMESPACE-tools
###########################################################
#Fetch the public key
###########################################################
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$SOAM_KC/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN --password $SOAM_KC_LOAD_USER_PASS
getPublicKey(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get keys -r $SOAM_KC_REALM_ID | grep -Po 'publicKey" : "\K([^"]*)'
}

echo Fetching public key from SOAM
soamFullPublicKey="-----BEGIN PUBLIC KEY----- $(getPublicKey) -----END PUBLIC KEY-----"

#READ_PEN_REQUEST_BATCH
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Read Pen Request Batch Data\",\"id\": \"READ_PEN_REQUEST_BATCH\",\"name\": \"READ_PEN_REQUEST_BATCH\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#WRITE_PEN_REQUEST_BATCH
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Write Pen Request Batch Data\",\"id\": \"WRITE_PEN_REQUEST_BATCH\",\"name\": \"WRITE_PEN_REQUEST_BATCH\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#DELETE_PEN_REQUEST_BATCH
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Delete Pen Request Batch Data\",\"id\": \"DELETE_PEN_REQUEST_BATCH\",\"name\": \"DELETE_PEN_REQUEST_BATCH\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"


#READ_PEN_REQUEST_BATCH_BLOB
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Read Pen Request Batch Source data\",\"id\": \"READ_PEN_REQUEST_BATCH_BLOB\",\"name\": \"READ_PEN_REQUEST_BATCH_BLOB\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

#WRITE_PEN_REQUEST_BATCH_BLOB
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"Update Pen Request Batch Source data\",\"id\": \"WRITE_PEN_REQUEST_BATCH_BLOB\",\"name\": \"WRITE_PEN_REQUEST_BATCH_BLOB\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL=""
if [ "$envValue" != "prod" ]
then
  SPLUNK_URL="dev.splunk.educ.gov.bc.ca"
  FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   HTTP_Port     2020
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Mem_Buf_Limit 20MB
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
else
  FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   HTTP_Port     2020
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
"
fi


echo Creating config map "$APP_NAME"-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE --from-literal=JDBC_URL=$DB_JDBC_CONNECT_STRING --from-literal=ORACLE_USERNAME="$DB_USER" --from-literal=ORACLE_PASSWORD="$DB_PWD" --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON="0 0/10 * * * *" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON_LOCK_AT_LEAST_FOR="540s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_PEN_WEB_BLOBS_CRON_LOCK_AT_MOST_FOR="580s" --from-literal=NATS_URL="$NATS_URL" --from-literal=NATS_CLUSTER="$NATS_CLUSTER" --from-literal=SPRING_JPA_SHOW_SQL="false" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="-" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="55s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="57s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON="-" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON_LOCK_AT_LEAST_FOR="250s" --from-literal=SCHEDULED_JOBS_EXTRACT_UNPROCESSED_STUDENTS_CRON_LOCK_AT_MOST_FOR="280s" --dry-run -o yaml | oc apply -f -


echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc project $PEN_NAMESPACE-$envValue
oc set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$PEN_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG"  --dry-run -o yaml | oc apply -f -
