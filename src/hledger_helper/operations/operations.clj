(ns hledger-helper.operations.operations
  (:require [clojure.string :as string]))

(defn remove-quote [s] (string/replace s #"\"" ""))
(defn remove-non-printable-char [s] (string/replace s #"\p{C}" ""))

(defn remove-map
  "the value of pred-map is a funtion that revceive the value of elem in map-list with same key, then return true or false"
  [pred-map-list map-list]
  (remove (fn [elem-map]
            (some (fn [pred-map]
                    (let [keylist (keys pred-map)]
                      (every? (fn [k] ((k pred-map) (k elem-map))) keylist)))
                  pred-map-list))
    map-list))

(defn find-duplicates
  [l]
  (map first (filter (fn [elem] (> (second elem) 1)) (frequencies l))))

(defn get-all-indices
  [l pred]
  (->> l
       (map-indexed vector)
       (filter (comp pred second))
       (map first)))


