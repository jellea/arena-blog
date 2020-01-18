(ns blog.ssg.app
  (:require [clojure.string :as string]
            [cljs.reader :as reader]))

(def hrstart (js/process.hrtime))

(def dotenv (.config (js/require "dotenv")))
(def fetch (js/require "node-fetch"))
(def hb (js/require "handlebars"))
(def fs (js/require "fs"))
(def slugify (js/require "slugify"))

(def token js/process.env.ARENA_ACCESS_TOKEN)
(def channel-slug js/process.env.ARENA_CHANNEL_SLUG)

(assert channel-slug "You need to set the channel as a environment variable named ARENA_CHANNEL_SLUG. For example: words-e6vp8lael4m")

(defn parse-edn [s]
  (-> (string/replace-all s #"<.+>(.+)<.+>" (fn [m i] i)) ;; strip html tags around urls, because of are.na :(
      (reader/read-string)))

(def media-embed-codes 
  {":image" (fn [edn]
              (let [{:keys [url align style class]} (parse-edn (str edn "}"))]
                (str "<img src=\"" url "\" style=\"" style "\" class=\"" class "\" align=\"" align "\">")))
   ":bandcamp" (fn [id] (str "<iframe style=\"border: 0; width: 100%; height: 120px;\" src=\"https://bandcamp.com/EmbeddedPlayer/album=" id "/size=large/bgcol=333333/linkcol=3c3cff/tracklist=false/artwork=small/transparent=true/\" seamless><a></a></iframe>"))
   ":youtube" (fn [id] (str "<iframe width=\"100%\" height=\"335\" src=\"https://www.youtube-nocookie.com/embed/" id "\" frameborder=\"0\" allow=\"accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>"))})

(defn media-embed [_match source id]
  (let [embed-code (get media-embed-codes source)]
    (embed-code id)))

(defn media-parser [item]
  (update item :content_html string/replace-all #"<p>\{{3}(\S+) (.+?)\}{3}" media-embed))

(defn gen-index [items]
  (let [template (fs.readFileSync "./views/index.html" #js {"encoding" "utf8"})
        html ((hb.compile template) (clj->js {:items items}))]
    (fs.writeFileSync "./.data/static/index.html" html))
    items)

(defn gen-rss [items]
  (let [template (fs.readFileSync "./views/rss.xml" #js {"encoding" "utf8"})
        xml ((hb.compile template) (clj->js {:items items}))]
    (fs.writeFileSync "./.data/static/rss.xml" xml))
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
  (-> (fetch (str "https://api.are.na/v2/channels/" channel-slug)
             #js {"headers" #js {"Authorization" (str "Bearer " token)}})
      (.then (fn [res] (.json res)))
      (.then (fn [json] (js->clj json :keywordize-keys true)))
      (.then (fn [data] (->> data
                             :contents
                             (filter #(= "Text" (-> % :class)))
                             (reverse)
                             (map #(-> % (select-keys [:generated_title :content :content_html :id :updated_at :created_at])))
                             (map #(assoc % :created_at_rfc822 (-> (:created_at %) (js/Date.) (.toUTCString))))
                             (map media-parser)
                             (map item->slug))))
      (.then (fn [items] (doall (map gen-post items)) items))
      (.then gen-index)
      (.then gen-rss)
      (.then (fn [items]
	(let [[s ms] (js/process.hrtime hrstart)]
          (js/console.info "Succesfully generated blog in %ds %dms" s, (/ ms 1000000)))
        (js/process.exit)))))
        
(defn main [& cli-args]
  (gen-site))
