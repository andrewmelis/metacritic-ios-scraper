(ns metacritic-ios-scraper.core
  (:require [net.cgrand.enlive-html :as html]
            [cheshire.core :as json]
            [hiccup.core :refer :all]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring]
            [clj-http.client :as client])
  (:gen-class))

(def metacritic-base-url "http://www.metacritic.com")
(def new-ios-releases-path "/browse/games/release-date/new-releases/ios/metascore")
(def new-ios-releases-url (str metacritic-base-url new-ios-releases-path))

(def user-agent "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/45.0.2454.101 Chrome/45.0.2454.101 Safari/537.36")

(defn- fetch-url [url user-agent]
  (with-open [inputstream (-> (java.net.URL. url)
                              .openConnection
                              (doto (.setRequestProperty "User-Agent" user-agent)) ; HACK
                              .getContent)]
    (html/html-resource inputstream)))

(def app-store-base-url "https://itunes.apple.com/search")

(defn hydrate-game-details-with-app-store-info [m]
  (let [{:keys [name] :as game} m
        app-store-response (first (:results (:body (client/get app-store-base-url
                                                               {:query-params {:term name
                                                                               :entity "software"
                                                                               :limit 1} ; bad assumption that first result is correct
                                                                :as :json}))))]
    (assoc m
           :app-store-link (:trackViewUrl app-store-response)
           :app-store-id (:trackId app-store-response)
           :app-store-artwork-60 (:artworkUrl60 app-store-response)
           :app-store-rating-current-version (:averageUserRatingForCurrentVersion app-store-response)
           :price (:price app-store-response)
           :formatted-price (:formattedPrice app-store-response))))

(defn fetch-games-and-scores []
  (let [raw-response (fetch-url new-ios-releases-url user-agent)]
    (->> (html/select raw-response #{[:.basic_stat html/first-child]})
         (partition 2)
         (map (fn [game-and-score]
                (let [game (first (filter #(= :a (:tag %)) game-and-score))
                      score (first (filter #(= :div (:tag %)) game-and-score))]
                  (assoc {}
                         :name (clojure.string/trim (first (:content game)))
                         :metacritic-link (str metacritic-base-url (:href (:attrs game)))
                         :metascore (when-let [raw-score (first (:content score))]
                                      (Integer/parseInt raw-score)))))) ; occasionally grabs an unintended "preview" game. defensive code
         (remove #(nil? (:metascore %)))
         (pmap hydrate-game-details-with-app-store-info)))) ; pmap to parallelize web calls

(def app-store-badge "display:inline-block;overflow:hidden;background:url(http://linkmaker.itunes.apple.com/images/badges/en-us/badge_appstore-lrg.svg) no-repeat;width:165px;height:40px;")

(defn games->table-markup [xs]
  (html [:table
         [:tr
          [:th]
          [:th "Name"]
          [:th "Metascore"]
          [:th "Price"]
          [:th "App Store Rating"]
          [:th]]
         (map (fn [m]
                (let [{:keys [name
                              metacritic-link
                              metascore
                              app-store-link
                              app-store-artwork-60
                              app-store-rating-current-version
                              formatted-price]} m]
                  [:tr
                   [:td [:img {:src app-store-artwork-60}]]
                   [:td name]
                   [:td [:a {:href metacritic-link} metascore]]
                   [:td formatted-price]
                   [:td app-store-rating-current-version]
                   [:td [:a {:href app-store-link :style app-store-badge}]]])) xs)]))

(defroutes app-routes
  (GET "/" [] (games->table-markup (fetch-games-and-scores))))

(defn -main []
  (let [port (Integer. "4111")]
    (ring/run-jetty #'app-routes {:port port :join? false})))
