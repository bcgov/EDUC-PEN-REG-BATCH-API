INSERT INTO PEN_REQUEST_BATCH_STATUS_CODE (PEN_REQUEST_BATCH_STATUS_CODE, LABEL, DESCRIPTION,
                                           DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                           UPDATE_USER, UPDATE_DATE)
VALUES ('DELETED',
        'Batch has been deleted',
        'Batch has been deleted',
        10,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/JINSUNG',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/JINSUNG',
        to_date('2019-11-07', 'YYYY-MM-DD'));