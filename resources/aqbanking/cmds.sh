#!/bin/bash
aqbanking-cli request --number=8 --aid=2 --transactions -c /home/weiss/clojure/hledger-helper/src/hledger_helper/import/import_csv/aqbanking/paypal/paypal-trans1.temp --fromdate=20210801 --todate=20210817

function genTemp() {

}
