(ns riemann.stackdriver
  "Forwards events to Stackdriver."
  (:require [clj-http.client :as client]
            [cheshire.core :refer [generate-string]])
  (:use [clojure.string :only [join split]]))

(def gateway-url "https://custom-gateway.stackdriver.com/v1/custom")

(defn metric-name
  "Constructs a metric-name for an event."
  [opts event]
  (let [service ((:name opts) event)
        split-service (if service (split service #" ") [])]
     (join "." split-service)))

(defn generate-datapoint
  "Generate datapoint from an event."
  [opts event]
  (let [host (:host event)
        value (:metric event)  
        service (metric-name opts event)]
    {:name service
     :value value
     :collected_at (long (:time event))}))

(defn post-datapoint
  "Post the riemann metrics datapoints."
  [api-key uri data]
  (let [http-options {:body data
                      :content-type :json
                      :headers {"x-stackdriver-apikey" api-key}}]
    (client/post uri http-options)))

(defn stackdriver
  "Returns a function which accepts an event and sends it to Stackdriver."
  [opts]
  (let [opts (merge { :api-key "stackdriver-api-key"
                      :name :service } opts)]
    (fn [event]
      (when (:metric event))
      (let [data-point {:timestamp (-> (System/currentTimeMillis) (quot 1000))
                        :proto_version 1
                        :data (generate-datapoint opts event)}
            json-data (generate-string data-point)]
        (when (:metric event)
          (post-datapoint (:api-key opts) gateway-url json-data))))))
