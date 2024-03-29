--PEN Request Batch Status Codes
DELETE FROM PEN_REQUEST_BATCH_STATUS_CODE WHERE PEN_REQUEST_BATCH_STATUS_CODE='NEW';

UPDATE PEN_REQUEST_BATCH_STATUS_CODE SET DISPLAY_ORDER = 1 WHERE PEN_REQUEST_BATCH_STATUS_CODE='LOADED';
UPDATE PEN_REQUEST_BATCH_STATUS_CODE SET DISPLAY_ORDER = 2 WHERE PEN_REQUEST_BATCH_STATUS_CODE='VALIDATED';
UPDATE PEN_REQUEST_BATCH_STATUS_CODE SET DISPLAY_ORDER = 3 WHERE PEN_REQUEST_BATCH_STATUS_CODE='ACTIVE';
UPDATE PEN_REQUEST_BATCH_STATUS_CODE SET DISPLAY_ORDER = 4 WHERE PEN_REQUEST_BATCH_STATUS_CODE='ARCHIVED';
UPDATE PEN_REQUEST_BATCH_STATUS_CODE SET DISPLAY_ORDER = 5 WHERE PEN_REQUEST_BATCH_STATUS_CODE='UNARCHIVED';
UPDATE PEN_REQUEST_BATCH_STATUS_CODE SET DISPLAY_ORDER = 6 WHERE PEN_REQUEST_BATCH_STATUS_CODE='LOADFAIL';
