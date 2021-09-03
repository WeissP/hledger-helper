(ns hledger-helper.operations.currency
  (:require [clojure.string :as string]
            [clojure.core.match :refer [match]])
  (:import [java.util.regex Pattern]))

;; `currency-name` the name of currency(one of the keys in currency-info)
;; `amount`,`amt` the number without currency name, like "1,100.23"
;; `display` `amt` with `currency-name`
;; `amt-dec` 4510005
;; `list` ["45" "100" "05"]
;; `amt-list` [["45" "100"] ("05")] means $ 45,100.05

(def currency-info
  "`sep`:  [thousands separator,decimal separator] 
  `display`: [prefix, sufix]"
  {"USD" {:sep ["," "."], :display ["$ " ""]},
   "TWD" {:sep ["," "."], :display ["" " TWD"]},
   "JPY" {:sep ["," "."], :display ["" " JPY"]},
   "RMB" {:sep ["," "."], :display ["" " RMB"]},
   "EURO" {:sep ["." ","], :display ["" " €"]}})

(defn normalize-currency-name
  "normalize `cur` to one of the key from `currency-info`, throw error if no found"
  [cur]
  (let [normalize-str (fn [s] (string/lower-case (subs s 0 1)))
        res (some (fn [e] (when (= (normalize-str e) (normalize-str cur)) e))
                  (keys currency-info))]
    (when (nil? res) (throw (Exception. (str "currency is not valid: " cur))))
    res))

(defn normalize-currency-name!
  "normalize `cur` to one of the key from `currency-info`, return `default` if no found"
  ([cur] (normalize-currency-name! cur "EURO"))
  ([cur default]
   (try (normalize-currency-name cur)
        (catch Exception _e (normalize-currency-name default)))))

(defn display
  [amt cur-name]
  (let [cur-name (normalize-currency-name cur-name)
        dis (get-in currency-info [cur-name :display])]
    (str (first dis) amt (second dis))))

(defn parse-display
  "parse display to [amount cur-name]"
  [cur-dis]
  (let [cur-name (some (fn [e]
                         (let [[pre su] (get-in currency-info [e :display])]
                           (when (and (clojure.string/includes? cur-dis pre)
                                      (clojure.string/includes? cur-dis su))
                             e)))
                       (keys currency-info))
        [pre su] (get-in currency-info [cur-name :display])]
    [(-> cur-dis
         (string/replace pre "")
         (string/replace su "")) cur-name]))

(defn to-literal-regex [s] (re-pattern (Pattern/quote s)))

(defn amt-dec->list
  [amt-dec & [split-number]]
  (let [split-number (or split-number 3)
        amt-str (str amt-dec)
        number (count amt-str)]
    (if (<= number 2)
      ["0" amt-str]
      (let [split-times (int (/ (- number 3) 3))
            split-indices (->> split-number
                               (iterate #(+ split-number %))
                               (take split-times)
                               (map #(+ 2 %))
                               (concat [2])
                               (map #(- number %))
                               reverse)]
        (into []
              (map #(subs amt-str %1 %2)
                (concat [0] split-indices)
                (concat split-indices [number])))))))

(defn cur->amt-list
  [amt cur-name]
  (let [cur-name (normalize-currency-name cur-name)
        [tsd-sep dec-sep] (get-in currency-info [cur-name :sep])
        [int-part & flt-part] (string/split amt (to-literal-regex dec-sep))
        int-vec (string/split int-part (to-literal-regex tsd-sep))]
    [int-vec flt-part]))

(defn amt-list->amt
  [amt-list cur-name]
  (let [cur-name (normalize-currency-name cur-name)
        [tsd-sep dec-sep] (get-in currency-info [cur-name :sep])
        [int-vec flt-part] amt-list
        flt-part (if (seqable? flt-part) flt-part [flt-part])]
    (string/join dec-sep (into [(string/join tsd-sep int-vec)] flt-part))))

(defn list->amt-list
  [l]
  (let [l (if (= 1 (count l)) (concat l [0]) l)]
    (conj [(drop-last l)] [(last l)])))

(defn amt-list->float
  [amt-list]
  (let [[int-vec flt-part] amt-list]
    (->> (into [(string/join int-vec)] flt-part)
         (string/join ".")
         Float/parseFloat)))

(defn to-float
  "convert display or [amt cur-name] to float"
  ([amt cur-name]
   (->> [amt cur-name]
        (apply cur->amt-list)
        amt-list->float))
  ([cur-dis]
   (if (float? cur-dis) cur-dis (apply to-float (parse-display cur-dis)))))

(defn flt->list
  [flt]
  (-> flt
      bigdec
      (* 100)
      int
      amt-dec->list))

(defn to-float! [& args] (if (nil? (first args)) 0 (apply to-float args)))

(defn display-float
  ([flt] (display-float flt "EURO"))
  ([flt cur-name]
  (-> flt
      flt->list
      list->amt-list
      (amt-list->amt cur-name)
      (display cur-name))))

(defn convert-amount
  "normalize number with sign and number notation"
  [number &
   {:keys [operation number-notation-from number-notation-to],
    :or {operation "reverse",
         number-notation-from "EURO",
         number-notation-to "EURO"}}]
  (let [contains-minus (string/starts-with? number "-")
        n (-> number
              (cur->amt-list number-notation-from)
              (amt-list->amt number-notation-to))]
    (match [operation contains-minus]
      ["keep" _] n
      ["positiv" true] (subs n 1)
      ["positiv" false] n
      ["negativ" true] n
      ["negativ" false] (str "-" n)
      ["reverse" true] (subs n 1)
      ["reverse" false] (str "-" n))))



