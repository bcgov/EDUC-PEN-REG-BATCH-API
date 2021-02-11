INSERT INTO PEN_REQUEST_BATCH_STATUS_CODE (PEN_REQUEST_BATCH_STATUS_CODE, LABEL, DESCRIPTION,
                                           DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                           UPDATE_USER, UPDATE_DATE)
VALUES ('HOLD_SIZE',
        'Batch on hold for size',
        'Batch file is being held for inspection due to its request volume',
        8,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));