(ns vaccinealert.core
  (:require [clojure.java.shell :as sh]
            [clj-http.client :as client]
            [cheshire.core :as cc]
            [clj-time.core :as ctf]
            [clojure.string :as cs]
            [chime.core :as chime])
  (:import [java.time Instant Duration]))

(def pins [411006, 411014,41105,411005,411016,411026])

;; All slot with 411*
(defn find-centers
  [min_age]
  (let [dt (.format (java.text.SimpleDateFormat. "dd-MM-yyyy") (new java.util.Date))
        vdata (:body (client/get (str "https://cdn-api.co-vin.in/api/v2/appointment/sessions/calendarByDistrict?district_id=363&date=" dt)
                                 {:headers {"Authorization" "Bearer <Token>"
                                            "user-agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36"}
                                  }))
        jdata (cc/parse-string vdata keyword)
        centers (:centers jdata)
        nearby-centers (filter (fn [center] (cs/starts-with? (str (:pincode center)) "411")) centers)
        _ (def xx nearby-centers)
        all-sessions (mapcat :sessions nearby-centers)
        age18 (filter (fn [center]
                        (some (fn [session]
                                (= (:min_age_limit session) min_age)) (:sessions center))) nearby-centers)
        avalialble-centers (filter (fn [center]
                                     (some (fn [slot] (and
                                                      (> (:available_capacity slot) 0)
                                                      (= (:min_age_limit slot) min_age)))
                                           (:sessions center))) age18)]
    (when (> (count avalialble-centers) 0)
      (println "slot available ")
      (sh/sh "say" "book" "book" "book")
      (clojure.pprint/pprint avalialble-centers)
      (count avalialble-centers))))



(defn find-centers-pin
  [min_age pin]
  (let [dt (.format (java.text.SimpleDateFormat. "dd-MM-yyyy") (new java.util.Date))
        vdata (:body (client/get (str "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=363&date=" dt)))
        jdata (cc/parse-string vdata keyword)
        centers (:centers jdata)
        nearby-centers (filter (fn [center] (= pin (:pincode center))) centers)
        all-sessions (mapcat :sessions nearby-centers)
        age18 (filter (fn [session] (= (:min_age_limit session) min_age)) all-sessions)]
    (when (seq (filter (fn [slot] (> (:available_capacity slot) 0)) age18))
      (println "slot available")
      (sh/sh "say" "book" "book" "book"))
    age18))


(defn find-and-alert
  []
  (let [now (Instant/now)
        _ (def intervals (-> (chime/periodic-seq (Instant/now) (Duration/ofSeconds 5))
                             rest))
        _ (def sch (chime/chime-at intervals
                                   (fn [time]
                                     (println "Chiming at " time)
                                     (find-centers 18))
                                   {:on-finished (fn []
                                                   (println "Schedule finished."))}))]))


(defn -main
  [& args]
  (println "Hello, World!")
  (find-and-alert))
