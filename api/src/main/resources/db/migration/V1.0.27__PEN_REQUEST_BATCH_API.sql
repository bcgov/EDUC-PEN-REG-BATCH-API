INSERT INTO PEN_REQUEST_BATCH_STATUS_CODE (PEN_REQUEST_BATCH_STATUS_CODE, LABEL, DESCRIPTION,
                                           DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                           UPDATE_USER, UPDATE_DATE)
VALUES ('REARCHIVED',
        'Batch has been re-archived',
        'Batch was unarchived and then archived.',
        12,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/JINSUNG',
        to_date('2021-03-16', 'YYYY-MM-DD'),
        'IDIR/JINSUNG',
        to_date('2021-03-16', 'YYYY-MM-DD'));