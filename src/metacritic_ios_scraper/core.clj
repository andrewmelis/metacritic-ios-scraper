(ns metacritic-ios-scraper.core
  (:require [net.cgrand.enlive-html :as html]
            [cheshire.core :as json]
            [clj-http.client :as client])
  (:gen-class))

(def metacritic-base-url "http://www.metacritic.com/")
(def new-ios-releases-path "browse/games/release-date/new-releases/ios/metascore")
(def new-ios-releases-url (str metacritic-base-url new-ios-releases-path))

(def user-agent "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/45.0.2454.101 Chrome/45.0.2454.101 Safari/537.36")

(defn- fetch-url [url user-agent]
  (with-open [inputstream (-> (java.net.URL. url)
                              .openConnection
                              (doto (.setRequestProperty "User-Agent" user-agent))
                              .getContent)]
    (html/html-resource inputstream)))

(def app-store-base-url "https://itunes.apple.com/search")

(defn hydrate-game-details-with-app-store-info [m]
  (let [{:keys [name] :as game} m
        app-store-response (first (:results (:body (client/get app-store-base-url
                                                               {:query-params {:term name
                                                                               :entity "software"
                                                                               :limit 1} ; bad assumption 
                                                                :as :json}))))]
    (assoc m
           :app-store-link (:trackViewUrl app-store-response)
           :app-store-id (:trackId app-store-response)
           :app-store-rating-current-version (:averageUserRatingForCurrentVersion app-store-response)
           :price (:price app-store-response)
           :formatted-price (:formattedPrice app-store-response))))

(defn fetch-games-and-scores []
  (let [raw-response (fetch-url new-ios-releases-url user-agent)]
    (->> (html/select raw-response #{[:.basic_stat html/first-child]})
         (partition 2)
         (pmap (fn [game-and-score]
                (let [game (first (filter #(= :a (:tag %)) game-and-score))
                      score (first (filter #(= :div (:tag %)) game-and-score))]
                  (assoc {}
                         :name (clojure.string/trim (first (:content game)))
                         :metacritic-link (str metacritic-base-url (:href (:attrs game)))
                         :metascore (first (:content score)))))) ; errors out on (Integer/parseInt ..) sometimes. returns string.
         (remove #(nil? (:metascore %)))
         (pmap hydrate-game-details-with-app-store-info))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

