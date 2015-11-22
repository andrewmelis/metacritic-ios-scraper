(ns metacritic-ios-scraper.core
  (:require [net.cgrand.enlive-html :as html]
            [cheshire.core :as json]
            [clj-http.client :as client])
  (:gen-class))

(def base-url "http://www.metacritic.com/")
(def new-ios-releases-path "browse/games/release-date/new-releases/ios/metascore")
(def new-ios-releases-url (str base-url new-ios-releases-path))

(def user-agent "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/45.0.2454.101 Chrome/45.0.2454.101 Safari/537.36")

(defn- fetch-url [url user-agent]
  (with-open [inputstream (-> (java.net.URL. url)
                              .openConnection
                              (doto (.setRequestProperty "User-Agent" user-agent))
                              .getContent)]
    (html/html-resource inputstream)))

(def example-game-products (game-products))

(defn game-products []
  (-> (fetch-url new-ios-releases-url user-agent)
      (html/select [:.product_wrap])))

;; (-> (fetch-url metacritic-url user-agent)
;;     (html/select #{[:.basic_stat html/first-child]}))

(defn fetch-games-and-scores []
  (let [raw-response (fetch-url new-ios-releases-url user-agent)]
    (->> (html/select raw-response #{[:.basic_stat html/first-child]})
         (partition 2)
         (map (fn [game-and-score]
                (let [game (first (filter #(= :a (:tag %)) game-and-score))
                      score (first (filter #(= :div (:tag %)) game-and-score))]
                  (assoc {}
                         :name (clojure.string/trim (first (:content game)))
                         :link (:href (:attrs game))
                         :metascore (Integer/parseInt (first (:content score))))))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

