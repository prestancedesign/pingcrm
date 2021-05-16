(ns pingcrm.main
  (:require [hiccup.page :as page]
            [inertia.middleware :as inertia]
            [pingcrm.db :as db]
            [pingcrm.templates.404 :as error]
            [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :as params]
            [ring.adapter.jetty :as server]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

(def asset-version "1")

(defn template [data-page]
  (page/html5
   {:class "h-full bg-gray-100"}
   [:head
    [:meta {:charset "utf-8"}]
    [:link {:rel "icon" :type "image/svg+xml" :href "/favicon.svg"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:title "PingCRM Inertia Clojure"]
    (page/include-css "/css/app.css")
    [:script {:src "/js/ziggy.js"}]
    [:script {:src "js/app.js" :defer true}]]
   [:body.font-sans.leading-none.text-gray-700.antialiased
    [:div {:id "app"
           :data-page data-page}]]))

(def share
  (let [user (db/get-user-by-id 1)]
    {:errors {}
     :auth {:user user}
     :flash {:success nil
             :error nil}}))

(defn index [_]
  (inertia/render "Dashboard/Index"))

(defn reports [_]
  (inertia/render "Reports/Index"))

(defn users [{:keys [params]}]
  (let [filters (select-keys params [:search :role :trashed])
        props {:users (db/retrieve-and-filter-users filters)
               :filters filters}]
    (inertia/render "Users/Index" props)))

(defn error []
  "trigger error")

(def app
  (ring/ring-handler
   (ring/router
    [["/" {:get {:handler #'index}}]
     ["/users" {:get {:handler #'users
                      :parameters {:query {:search int?}}}}]
     ["/reports" {:get {:handler #'reports}}]])
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly {:status 404, :body error/not-found})}))
   {:middleware [params/parameters-middleware
                 wrap-keyword-params
                 [inertia/wrap-inertia template asset-version share]]}))

(defn -main []
  (server/run-jetty #'app {:port 3000
                           :join? false}))