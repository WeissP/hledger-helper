(ns hledger-helper.core
  (:require [cli4clj.cli :as cli]
            [hledger-helper.report.reporting :as rpt]
            [hledger-helper.import.single-transaction.single-transaction :as st]
            [hledger-helper.processing-edn :as pedn]
            [hledger-helper.import.to-file :as tf]
            [hledger-helper.import.import-csv.aqbanking.auto-import :as ai]))

(defn -main
  [& args]
  (tf/init)
  (ai/init)
  (cli/start-cli {:cmds {:info-add {:fn pedn/add-assert,
                                    :short-info "Test Command",
                                    :long-infor "long long long",
                                    :completion-hint "hello world completion"},
                         :dups {:fn rpt/check-dupes,
                                :short-info "Test Command",
                                :long-infor "long long long",
                                :completion-hint "hello world completion"},
                         :add {:fn st/add-tr,
                               :short-info "Test Command",
                               :long-infor "long long long",
                               :completion-hint "hello world completion"},
                         :add- {:fn st/add-tr-,
                                :short-info "Test Command",
                                :long-infor "long long long",
                                :completion-hint "hello world completion"},
                         :add-- {:fn st/add-tr--,
                                 :short-info "Test Command",
                                 :long-infor "long long long",
                                 :completion-hint "hello world completion"},
                         :budget {:fn rpt/budget-report,
                                  :short-info "Test Command",
                                  :long-infor "long long long",
                                  :completion-hint "hello world completion"},
                         :import {:fn ai/import-all,
                                  :short-info "Test Command",
                                  :long-infor "long long long",
                                  :completion-hint "hello world completion"},
                         :cost {:fn rpt/cost-report,
                                :short-info "Test Command",
                                :long-infor "long long long",
                                :completion-hint "hello world completion"},
                         :transfer {:fn pedn/budget-transfer,
                                    :short-info "Test Command",
                                    :long-infor "long long long",
                                    :completion-hint "hello world completion"},
                         :tsf :transfer,
                         :bgt :budget,
                         :ipt :import},
                  :allow-eval true,
                  :prompt-string "hledger-helper>>>",
                  :alternate-scrolling true}))

