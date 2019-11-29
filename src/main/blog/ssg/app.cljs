(ns blog.ssg.app)

(def fetch (js/require "node-fetch"))
(def hb (js/require "handlebars"))
  (def fs (js/require "fs"))

(def token js/process.env.ARENA_ACCESS_TOKEN)

(defn log [x]
  (prn x)
  x)

(defn hello []
    (-> (fetch "https://api.are.na/v2/channel/words-e6vp8lael4m/feed" #js {"headers" #js {"access_token" token}})
      (.then (fn [res] (.json res)))
      (.then (fn [json] (js->clj json :keywordize-keys true)))
      (.then (fn [data] (->> data 
                             :items
                             (filter #(= "Text" (-> % :item :class)))
                             (map #(-> % :item (select-keys [:generated_title :content])))
                             log)))
      (.then (fn [items] ((hb.compile (fs.readFileSync "./views/index.html" #js {"encoding" "utf8"})) (clj->js {:items items}))))
      (.then (fn [html] (prn [:html html])
        (fs.writeFileSync "./.data/static/index.html" html)))
      ;;(.catch (fn [] (js/process.exit 1)))
  (.then (fn [] (js/process.exit)))))
        
  (defn main [& cli-args]
  (println "Hello World2")
  (hello))
