ALTER TABLE PEN_REQUEST_BATCH
    DROP COLUMN UNARCHIVED_BATCH_STATUS_CODE;

ALTER TABLE PEN_REQUEST_BATCH
    DROP COLUMN UNARCHIVED_BATCH_CHANGED_FLAG;

ALTER TABLE PEN_REQUEST_BATCH
    DROP COLUMN UNARCHIVED_USER;

INSERT INTO PEN_REQUEST_BATCH_STATUS_CODE (PEN_REQUEST_BATCH_STATUS_CODE, LABEL, DESCRIPTION,
                                           DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                           UPDATE_USER, UPDATE_DATE)
VALUES ('UNARCH_CHG',
        'Batch changed while unarchived',
        'Batch was unarchived and then edited/modified while unarchived.',
        9,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/JINSUNG',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/JINSUNG',
        to_date('2019-11-07', 'YYYY-MM-DD'));

DROP TABLE UNARCHIVED_BATCH_STATUS_CODE;