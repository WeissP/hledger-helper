(ns hledger-helper.operations.date
  (:require [cljc.java-time.format.date-time-formatter :as fmt]
            [cljc.java-time.local-date :as ld]
            [cljc.java-time.temporal.chrono-unit :as cu]))

(defn convert-date
  "convert date string to yyyy-MM-dd"
  [date]
  (ld/to-string
    (some (fn [e]
            (try (ld/parse date (fmt/of-pattern e))
                 (catch java.time.format.DateTimeParseException _e nil)))
          ["yyyy-MM-dd" "yyyy.MM.dd" "yyyy/MM/dd" "dd-MM-yyyy" "dd.MM.yyyy"
           "dd/MM/yyyy" "yyyyMMdd"])))

(defprotocol DateConversion
  (to-local-date [x] "to local date"))

(extend-protocol DateConversion
  java.lang.Integer
    (to-local-date [x] (to-local-date (ld/of (ld/get-year (ld/now)) x 1)))
  java.lang.Long
    (to-local-date [x] (to-local-date (ld/of (ld/get-year (ld/now)) x 1)))
  java.time.LocalDate
    (to-local-date [x] x)
  java.lang.String
    (to-local-date [x] (ld/parse (convert-date x))))

(defn to-local-date-time [x] (ld/at-start-of-day (to-local-date x)))

(defn later?
  "check if `x` is later than `y`, `x` and `y` will first be converted to local-date"
  [x y]
  (ld/is-after (to-local-date x) (to-local-date y)))

(defn eq-later?
  "check if `x` is later than or equal to `y`, `x` and `y` will first be converted to local-date"
  [x y]
  (not (ld/is-before (to-local-date x) (to-local-date y))))

(defn month-between
  [from to]
  (cu/between cu/months (to-local-date-time from) (to-local-date-time to)))

(defn inc-month [old] (if (= old 12) 1 (inc old)))
(defn dec-month [old] (if (= old 1) 12 (dec old)))

(defn plus-days-str
  [d i]
  (-> d
      to-local-date
      (ld/plus-days i)
      ld/to-string))

(defn current-month [] (ld/get-month-value (ld/now)))

(defn get-month-date
  "year is always current year e.g. 2  -> {:date-from 2021-02-01, :date-to 2021-03-01}"
  [month]
  (let [year (ld/get-year (ld/now))
        date-from (ld/to-string (ld/of year month 1))
        date-to (-> date-from
                    ld/parse
                    (ld/plus-months 1)
                    ld/to-string)]
    {:date-from date-from, :date-to date-to}))

(defn sort-maps-by-date
  [maps & {:keys [date-key], :or {date-key "date"}}]
  maps
  (sort-by #(-> %
                date-key
                first
                convert-date
                ld/parse)
           ld/compare-to
           maps))

