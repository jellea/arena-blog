(ns blog.ssg.app)

(def fetch (js/require "node-fetch"))
(def hb (js/require "handlebars"))
(def fs (js/require "fs"))
(def md (js/require "marked"))
(def slugify (js/require "slugify"))

(def token js/process.env.ARENA_ACCESS_TOKEN)

(defn log [x]
  (prn x)
  x)

(defn gen-index [items]
  (let [template (fs.readFileSync "./views/index.html" #js {"encoding" "utf8"})
        html ((hb.compile template) (clj->js {:items items}))]
    (fs.writeFileSync "./.data/static/index.html" html))
    items)

(def post-template (-> "./views/post.html" 
                       (fs.readFileSync #js {"encoding" "utf8"})
                       (hb.compile)))

(defn gen-post [{:keys [slug] :as post}]
  (let [html (post-template (clj->js post))]
    (fs.writeFileSync (str "./.data/static/" slug ".html") html)))

(defn item->slug [{:keys [generated_title id] :as item}]
  (let [slug (slugify (str (.toLowerCase generated_title) " " id))]
    (assoc item :slug slug)))

(defn gen-site []
  (-> (fetch "https://api.are.na/v2/channel/words-e6vp8lael4m/feed" #js {"headers" #js {"access_token" token}})
      (.then (fn [res] (.json res)))
      (.then (fn [json] (js->clj json :keywordize-keys true)))
      (.then (fn [data] (->> data 
                             :items
                             (filter #(= "Text" (-> % :item :class)))
                             (map #(-> % :item (select-keys [:generated_title :content :id :updated_at])))
                             (map #(update % :content md))
                             (map item->slug))))
      (.then (fn [items] (doall (map gen-post items)) items))
      (.then gen-index)
      (.then (fn [] (js/process.exit)))))
        
(defn main [& cli-args]
  (gen-site))
