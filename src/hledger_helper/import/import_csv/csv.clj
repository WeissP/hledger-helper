(ns hledger-helper.import.import-csv.csv
  (:require [hledger-helper.operations.operations :as op]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.java.shell :refer [sh]]))

(defn get-csv-key-list
  "get all keys in csv"
  [csv-data]
  (->> csv-data
       first
       (map #(-> %
                 op/remove-quote
                 op/remove-non-printable-char
                 string/trim
                 (string/replace #" " "-")
                 keyword))))

(defn csv->map-list
  [csv-data]
  (let [keylist (get-csv-key-list csv-data)]
    (map (fn [line] (zipmap keylist (map #(conj [] %) line))) (rest csv-data))))

(defn str->csv
  ([str] (str->csv str \,))
  ([str sep] (csv/read-csv str :separator sep)))

(defn file->csv
  ([path] (file->csv path \,))
  ([path sep] (csv/read-csv (io/reader path) :separator sep)))


(defn file->map-list
  [& args]
  (->> (apply file->csv args)
       csv->map-list))


(defn write-map-list
  [path map-list]
  (with-open [writer (io/writer path)] (csv/write-csv writer map-list)))

(defn get-diff [old new] (set/difference (set new) (set old)))

(defn dir->map-list
  [dir sep]
  (->> dir
       io/file
       file-seq
       (filter #(.isFile %))
       (filter #(string/includes? (-> %
                                      .toPath
                                      .getFileName
                                      string/lower-case)
                                  ".csv"))
       (map #(file->map-list % sep))
       flatten))


