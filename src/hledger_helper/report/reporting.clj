(ns hledger-helper.report.reporting
  (:require [hledger-helper.report.get-info :as info]
            [hledger-helper.operations.operations :as op]
            [hledger-helper.processing-edn :as pedn]
            [cljc.java-time.local-date :as ld]
            [clojure.string :as string]
            [doric.core :refer [table raw]]
            [hledger-helper.operations.date :as date]
            [hledger-helper.operations.currency :as cur]))

(def non-budget-expenses
  "all expenses that will not be calculated in budgets "
  (pedn/get-budget-exclusive-expenses))

(defn show-bar
  "display bar like [###:::]"
  [flt total]
  (let [empty-symbol ":"
        non-empty-symbol "#"]
    (format "%.2f%%  [%s]"
            (float (* flt 100))
            (string/join (take total
                               (concat (take (Math/round (float (* flt total)))
                                             (repeat non-empty-symbol))
                                       (repeat empty-symbol)))))))

(defn check-dupes-by-date
  "return the duplicates on the `date` if there are only two arguments, otherwise print duplicates within `date-from` to `date-to`"
  ([assert date]
   (op/find-duplicates (info/get-tr-by-assert-and-date assert date)))
  ([assert date-from date-to]
   (doseq [date (map ld/to-string
                  (take-while #(not (ld/is-after % (ld/parse date-to)))
                              (iterate #(ld/plus-days % 1)
                                       (ld/parse date-from))))]
     (let [d (check-dupes-by-date assert date)]
       (when-not (empty? d) (println d))))))

(defn check-dupes
  []
  (doseq [assert (pedn/get-all-asserts)]
    (check-dupes-by-date assert "2021-06-29" (ld/to-string (ld/now)))))


(defn get-non-budget-cost
  "get summarize from all exclusive expenses by month"
  [month]
  (apply info/get-cost-by-month month non-budget-expenses))

(defn get-budget-mod-amount
  "get modifered amount by `date` and `key`"
  [modifier-key date]
  (let [modifier (modifier-key (pedn/get-budget-mod))
        repeat-list (:repeat modifier)
        start-date (:start-at modifier)]
    (when (date/eq-later? date start-date)
      (nth repeat-list
           (mod (date/month-between start-date date) (count repeat-list))))))

(defn get-non-empty-amount-budget-mods
  []
  (filter #(not= (:amount (second %)) 0) (pedn/get-budget-mod)))

(defn get-pure-budget-mods
  []
  (filter #(and (not (:end? (second %))) (= (:amount (second %)) 0))
    (pedn/get-budget-mod)))

(defn get-budget-mod-amounts
  [date mods]
  (reduce + (filter some? (map #(get-budget-mod-amount % date) (keys mods)))))

;; (defn get-budget-non-empty-mod-amounts
;;   "calculate total amounts in `date`"
;;   [date]
;;   (let [mods (get-non-empty-amount-budget-mods)]
;;     (reduce +
;;       (filter some? (map #(get-budget-mod-amount % date) (keys mods))))))

;; (defn get-budget-pure-mod-amounts
;;   "calculate total amounts in `date`"
;;   [date]
;;   (let [mods (get-pure-budget-mods)]
;;     (reduce +
;;       (filter some? (map #(get-budget-mod-amount % date) (keys mods))))))

(defn get-budget-revenues-total-amount
  ([] (get-budget-revenues-total-amount (date/current-month)))
  ([month] (info/get-cost-by-month month "revenues:job:part-time")))

(defn get-budget-total-amounts
  ([] (get-budget-total-amounts (date/current-month)))
  ([month]
   (+ (pedn/get-budget-amount)
      (get-budget-revenues-total-amount month)
      (:amount (pedn/get-budget-rollover))
      (get-budget-mod-amounts month (get-pure-budget-mods)))))

(defn update-budget-rollover
  "update budget rollover to current month"
  []
  (let [rollover-month (:month (pedn/get-budget-rollover))
        current-month (ld/get-month-value (ld/now))
        budget-amount (get-budget-total-amounts rollover-month)]
    (when-not (= rollover-month current-month)
      (pedn/update-rollover (- (+ budget-amount
                                  (get-non-budget-cost rollover-month))
                               (info/get-cost-by-month rollover-month)))
      (doseq [k (keys (get-non-empty-amount-budget-mods))]
        (pedn/budget-transfer k
                              (get-budget-mod-amount
                                k
                                (date/get-the-last-day-of-month
                                  rollover-month))))
      (update-budget-rollover))))

(defn budget-report
  "report budget"
  ([] (budget-report 3))
  ([depth]
   (update-budget-rollover)
   (let [current-month (date/current-month)
         length-of-month (ld/length-of-month (ld/now))
         day-of-month (ld/get-day-of-month (ld/now))
         days-in-month-flt (/ day-of-month length-of-month)
         used-budget (- (info/get-cost-by-month current-month)
                        (get-non-budget-cost current-month))
         expected-total-budget
           (+ (- (get-budget-total-amounts)
                 (get-non-budget-cost (ld/get-month-value
                                        (ld/minus-months (ld/now) 1))))
              (get-budget-mod-amounts current-month (get-non-empty-amount-budget-mods)))
         rest-budget (- expected-total-budget used-budget)]
     (print (info/get-costs-by-depth depth))
     (println
       (table
         {:format raw}
         [{:name :a, :title "", :align :left}
          {:name :b, :title "", :align :left}
          {:name :c, :title "", :align :left}]
         [{:a "Used/Total:",
           :b (format "[%s/%s] "
                      (cur/display-float used-budget)
                      (cur/display-float expected-total-budget)),
           :c (show-bar (/ used-budget expected-total-budget) length-of-month)}
          {:a "Days:",
           :b (format "[%s/%s]" day-of-month length-of-month),
           :c (show-bar days-in-month-flt length-of-month)}
          {:a "Rest Budget:", :b (cur/display-float rest-budget)}
          {:a "Rest Budget in days:",
           :b (cur/display-float (- (* expected-total-budget days-in-month-flt)
                                    used-budget))}])))))

(defn cost-report [& args] (println (apply info/get-costs-by-depth args)))


