// CREATE tables


DROP TABLE IF EXISTS transactions;

CREATE TABLE transactions (
  txn_id varchar(36) PRIMARY KEY,
  cc_no varchar(32),
  year int,
  month int,
  day int,
  txn_time timestamp,
  amount double,
  cc_provider varchar(32),
  country varchar(32),
  date_text varchar(32),
  hour int,
  location varchar(32),
  merchant varchar(32),
  min int,
  notes varchar(32),
  status varchar(16),
  tag varchar(16),
  user_id varchar(16)
) ;

CREATE INDEX transactions_ccno_txntime ON transactions (cc_no, txn_time);


DROP TABLE IF EXISTS hourlyaggregates_bycc;

CREATE TABLE hourlyaggregates_bycc(
  cc_no varchar(32),
  hour int,
  total_amount double,
  max_amount double,
  min_amount double,
  total_count bigint,
    PRIMARY KEY (cc_no, hour)
);

CREATE INDEX hourlyaggregates_bycc_ccno_hour ON transactions (cc_no, hour); 


DROP TABLE IF EXISTS dailyaggregates_bycc;

CREATE TABLE dailyaggregates_bycc(
  cc_no varchar(32),
  day int,
  total_amount double,
  max_amount double,
  min_amount double,
  total_count bigint,
    PRIMARY KEY (cc_no, day)
) ;


CREATE INDEX dailyaggregates_bycc_ccno_day ON transactions (cc_no, day);


DROP TABLE  IF EXISTS monthlyaggregates_bycc;

CREATE TABLE monthlyaggregates_bycc(
  cc_no varchar(32),
  month int,
  total_amount double,
  max_amount double,
  min_amount double,
  total_count bigint,
    PRIMARY KEY (cc_no, month)
) ;

CREATE INDEX dailyaggregates_bycc_ccno_day ON transactions (cc_no, day); 


DROP TABLE IF EXISTS yearlyaggregates_bycc;

CREATE TABLE yearlyaggregates_bycc(
  cc_no varchar(32),
  year int,
  total_amount double,
  max_amount double,
  min_amount double,
  total_count bigint,
    PRIMARY KEY (cc_no, year)
) ;

DROP TABLE IF EXISTS dailytxns_bymerchant;

CREATE TABLE dailytxns_bymerchant (
    merchant varchar(32),
    day int,
    max_amount double,
    min_amount double,
    total_amount double,
    total_count bigint,
    cc_no varchar(32),
    amount double,
    cc_provider varchar(32),
    txn_time timestamp,
    txn_id varchar(36),
    location varchar(32),
    notes varchar(32),
    status varchar(16),
    tag varchar(16),
    user_id varchar(16),
    PRIMARY KEY (merchant, day, cc_no)
);

DROP TABLE IF EXISTS txn_count_min;

CREATE TABLE txn_count_min (
  year int,
  month int,
  day int,
  hour int,
  minute int,
  time timestamp,
  approved_rate_hr double,
  approved_rate_min double,
  approved_txn_hr int,
  approved_txn_min int,
  ttl_txn_hr int,
  ttl_txn_min int,
  PRIMARY KEY (year, month, day, hour, minute )
) ;

// Performance test table

DROP TABLE IF EXISTS txn_by_cc;

CREATE TABLE txn_by_cc (
cc_no varchar(32),
txn_year int,
txn_month int,
txn_day int,
txn_time timestamp,
amount double,
cc_provider varchar(32),
location varchar(32),
merchant varchar(32),
notes varchar(32),
status varchar(16),
tag varchar(16),
txn_id varchar(36),
user_id varchar(16),
  PRIMARY KEY ( cc_no, txn_time)
) ;


// Sample rtfap.transaction inserts

insert into transactions (year, month, day, hour, min, txn_time, cc_no, amount, cc_provider, location, merchant, notes, status, txn_id, user_id, tag)
VALUES ( 2020, 01, 02, 11, 04, '2020-01-02 11:04:19', '1234123412341234', 200.0, 'VISA', 'London', 'Ted Baker', 'pretty good clothing', 'Approved', '62d1be5d-15f3-4d04-88ad-a937a3b49e95', 'tomdavis', 'Suspicious');

insert into transactions (year, month, day, hour, min, txn_time, cc_no, amount, cc_provider, location, merchant, notes, status, txn_id, user_id, tag)
VALUES ( 2020, 01, 02, 11, 04, '2020-01-02 11:04:24', '1234123412341235', 400.0, 'VISA', 'Manchester', 'Macy', 'cool stuff-good customer', 'Approved', '63d1be5d-15f3-4d04-88ad-a937a3b49e95', 'simonanbridge', 'HighValue');

insert into transactions (year, month, day, hour, min, txn_time, cc_no, amount, cc_provider, location, merchant, notes, status, txn_id, user_id, tag)
VALUES ( 2020, 01, 02, 11, 04, '2020-01-02 11:04:53', '1234123412341235', 800.0, 'VISA','London', 'Harrods', 'customer likes electronics', 'Approved', '64d1be5d-15f3-4d04-88ad-a937a3b49e95', 'simonanbridge', 'HighValue');

insert into transactions (year, month, day, hour, min, txn_time, cc_no, amount, cc_provider, location, merchant, notes, status, txn_id, user_id, tag)
VALUES ( 2020, 01, 03, 11, 04, '2020-01-03 11:04:59', '1234123412341236', 750.0, 'MASTERCARD',  'San Jose', 'GAP', 'customer likes electronics', 'Approved', '65d1be5d-15f3-4d04-88ad-a937a3b49e95', 'mikestewart', 'HighValue');

insert into transactions (year, month, day, hour, min, txn_time, cc_no, amount, cc_provider, location, merchant, notes, status, txn_id, user_id, tag)
VALUES ( 2020, 01, 03, 12, 30, '2020-01-03 12:30:00', '1234123412341237', 1500.0, 'AMEX', 'New York', 'Ann Taylor', 'frequent customer', 'Approved', '66d1be5d-15f3-4d04-88ad-a937a3b49e95', 'caroline', 'HighValue');
   
insert into transactions (year, month, day, hour, min, txn_time, cc_no, amount, cc_provider, location, merchant, notes, status, txn_id, user_id, tag)
VALUES ( 2020, 01, 03, 21, 04, '2020-01-04 21:04:19', '1234123412341234', 200.0, 'VISA','San Francisco', 'Nordstrom', 'asked for discounts', 'Approved', '67d1be5d-15f3-4d04-88ad-a937a3b49e95', 'tomdavis', 'Fraudulent');

select * from transactions;

       