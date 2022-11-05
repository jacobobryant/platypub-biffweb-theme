(ns com.biffweb.theme.site
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [babashka.tasks :refer [shell]]
            [clojure.java.shell :as shell]
            [babashka.fs :as fs]
            [com.platypub.themes.common :as common]
            [hiccup.util :refer [raw-string]]
            [cheshire.core :as cheshire]
            [markdown.core :as md]
            [clj-yaml.core :as yaml]
            [clojure.walk :as walk]))

(defn base-html* [{:base/keys [head path] :keys [site] :as opts} & body]
  (let [[title
         description
         image
         base-url] (for [k ["title" "description" "image" "url"]]
                     (or (get opts (keyword "base" k))
                         (get-in opts [:post (keyword k)])
                         (get-in opts [:page (keyword k)])
                         (get-in opts [:site (keyword k)])))
        title (or title (:title site))]
    [:html
     {:lang "en-US"
      :style {:min-height "100%"
              :height "auto"}}
     [:head
      [:title title]
      [:meta {:charset "UTF-8"}]
      [:meta {:name "description" :content description}]
      [:meta {:content title :property "og:title"}]
      [:meta {:content description :property "og:description"}]
      (when image
        (list
          [:meta {:content "summary_large_image" :name "twitter:card"}]
          [:meta {:content image :name "twitter:image"}]
          [:meta {:content image :property "og:image"}]))
      [:meta {:content (str base-url path) :property "og:url"}]
      [:link {:ref "canonical" :href (str base-url path)}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:meta {:charset "utf-8"}]
      [:link {:href "/feed.xml",
              :title (str "Feed for " (:title site)),
              :type "application/atom+xml",
              :rel "alternate"}]
      [:script {:src "https://unpkg.com/hyperscript.org@0.9.3"}]
      [:script {:src "https://www.google.com/recaptcha/api.js"
                :async "async"
                :defer "defer"}]
      common/favicon-settings
      (when-some [html (:embed-html site)]
        (raw-string html))
      head
      [:link {:rel "stylesheet" :href "/css/main.css"}]]
     [:body
      {:style {:position "absolute"
               :width "100%"
               :min-height "100%"
               :display "flex"
               :flex-direction "column"}}
      body]]))

(def css
  (list
   [:link {:rel "stylesheet" :href "/css/prism.css"}]
   [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/4.0.0/github-markdown.min.css"}]
   ;[:link {:rel "stylesheet" :href "/css/vs.css"}]
   [:script {:src "/js/prism.js"}]
   
   ))

(defn base-html [{:keys [dev] :as opts} & body]
  (base-html* (cond-> opts
                dev (update :base/head concat [[:script {:src "/js/live.js"}]])
                true (update :base/head concat css)
                )
              body))

(def logo
  [:div [:a {:href "/"}
         [:img {:src "https://cdn.findka.com/misc/biff-logo-transparent-cropped.png"
                :alt "Biff"
                :style {:max-width "110px"}}]]])

(def hamburger-icon
  [:div.sm:hidden.cursor-pointer
   {:_ "on click toggle .hidden on #nav-menu"}
   (for [_ (range 3)]
     [:div.bg-white
      {:class "h-[4px] w-[30px] my-[6px]"}])])

(defn nav-options [{:keys [docs-href]
                    :or {docs-href "/docs/"}}]
  [["Docs" docs-href]
   ["Blog" "/newsletter/"]
   ;["API" "/api/com.biffweb.html"]
   ;["Repo" "https://github.com/jacobobryant/biff"]
   ;["Community" "/community/"]
   ["Consulting" "/consulting/"]])

(defn navbar
  ([] (navbar {:class "max-w-screen-md"}))
  ([opts]
   (list
    [:div.bg-primary.py-2
     [:div.flex.mx-auto.items-center.text-white.gap-4.text-lg.flex-wrap.px-3
      (merge {:class "max-w-screen-md"} (select-keys opts [:class]))
      logo
      [:div.flex-grow]
      (for [[label href] (nav-options opts)]
        [:a.hover:underline.hidden.sm:block
         {:href href
          :target (when (= label "API")
                    "_blank")}
         label])
      hamburger-icon]]
    [:div#nav-menu.bg-primary.px-5.py-2.text-white.text-lg.hidden.transition-all.ease-in-out.sm:hidden
     (for [[label href] (nav-options opts)]
       [:div.my-2 [:a.hover:underline.text-lg {:href href} label]])])))

(defn byline [{:keys [published-at byline/card]}]
  [:div
   {:style {:display "flex"
            :align-items "center"}}
   [:img (if card
           {:src "https://cdn.findka.com/profile.jpg"
            :width "115px"
            :height "115px"
            :style {:border-radius "50%"}}
           {:src (common/cached-img-url {:url "https://cdn.findka.com/profile.jpg"
                                         :w 200 :h 200})
            :width "50px"
            :height "50px"
            :style {:border-radius "50%"}})]
   [:div {:style {:width "0.75rem"}}]
   [:div
    [:div {:style {:line-height "1.25"}}
     [:a.hover:underline
      {:class (if card
                "text-[2.5rem]"
                "text-blue-600")
       :href "https://jacobobryant.com/"
       :target "_blank"}
      "Jacob O'Bryant"]]
    [:div {:class (if card "text-[2.2rem]" "text-[90%]")
           :style {:line-height "1"
                   :color "#4b5563"}}
     (common/format-date "d MMM yyyy" published-at)]]])

(def errors
  {"invalid-email" "It looks like that email is invalid. Try a different one."
   "recaptcha-failed" "reCAPTCHA check failed. Try again."
   "unknown" "There was an unexpected error. Try again."})

(defn subscribe-form [{:keys [bg show-disclosure sitekey]}]
  [:div.flex.flex-col.items-center.text-center.px-3
   {:class (when (= bg :dark)
             '[text-white
               bg-primary])}
   [:div.h-20]
   [:div.font-bold.text-3xl.leading-none
    "Biff: The Newsletter"]
   [:div.h-5]
   [:div "Stay up-to-date about all things Biff"]
   [:div.h-5]
   [:script (raw-string "function onSubscribe(token) { document.getElementById('recaptcha-form').submit(); }")]
   [:form#recaptcha-form.w-full.max-w-md
    {:action "/.netlify/functions/subscribe"
     :method "POST"}
    [:input {:type "hidden"
             :name "href"
             :_ "on load set my value to window.location.href"}]
    [:input {:type "hidden"
             :name "referrer"
             :_ "on load set my value to document.referrer"}]
    [:div.flex.flex-col.sm:flex-row.gap-2
     [:input {:class '[rounded
                       shadow
                       border-gray-300
                       focus:ring-0
                       focus:ring-transparent
                       focus:border-gray-300
                       flex-grow
                       text-black]
              :type "email"
              :name "email"
              :placeholder "Enter your email"
              :_ (str "on load "
                      "make a URLSearchParams from window.location.search called p "
                      "then set my value to p.get('email')")}]
     [:button {:class '[bg-accent
                        hover:bg-accent-dark
                        text-white
                        py-2
                        px-4
                        rounded
                        shadow
                        g-recaptcha]
               :data-sitekey sitekey
               :data-callback "onSubscribe"
               :data-action "subscribe"
               :type "submit"}
      "Subscribe"]]
    (for [[code explanation] errors]
      [:div.text-red-600.hidden.text-left
       {:_ (str "on load if window.location.search.includes('error="
                code
                "') remove .hidden from me")}
       explanation])]
   [:div.h-20]
   (when show-disclosure
     [:div.sm:text-center.text-sm.leading-snug.opacity-75.w-full.px-3.mb-3
      (common/recaptcha-disclosure {:link-class "underline"})])])

(defn post-page [{:keys [site post account] :as opts}]
  (base-html
    opts
    (navbar (assoc opts :class (if ((:tags post) "video")
                                 "max-w-screen-lg"
                                 "max-w-screen-sm")))
    [:div.mx-auto.p-3.text-lg.flex-grow.w-full
     {:class (if ((:tags post) "video")
               "max-w-screen-lg"
               "max-w-screen-sm")}
     [:div.h-5]
     [:h1.font-bold.leading-tight.text-gray-900.text-4xl
      (:title post)]
     [:div.h-3]
     (byline post)
     [:div.h-5]
     [:div.post-content (raw-string (:html post))]
     [:div.h-5]]
    (subscribe-form {:bg :dark :sitekey (:recaptcha/site account)})
    [:div.bg-primary
     [:div.sm:text-center.text-sm.leading-snug.w-full.px-3.pb-3.text-white.opacity-75
      (common/recaptcha-disclosure {:link-class "underline"})]]))

(def sponsors
  [{:img "https://avatars.githubusercontent.com/u/19023?v=4"
    :url "https://github.com/tbrooke"}
   {:img "https://avatars.githubusercontent.com/u/53870456?v=4"
    :url "https://github.com/john-shaffer"}
   {:img "https://avatars.githubusercontent.com/u/4767299?v=4"
    :url "https://github.com/jeffp42ker"}
   {:img "https://avatars.githubusercontent.com/u/9289130?v=4"
    :url "https://github.com/wuuei"}])

(defn subscribed-page [opts]
  (base-html
    (assoc opts :base/title "You're subscribed to Biff: The Newsletter")
    (navbar opts)
    [:div.max-w-screen-lg.mx-auto.p-3.w-full
     [:h1.font-bold.text-2xl "You're subscribed"]
     [:div "Check your inbox for a welcome email."]]))

(defn newsletter-page [{:keys [posts] :as opts}]
  (base-html
    (assoc opts :base/title "Biff: The Newsletter")
    (navbar (assoc opts :class "max-w-screen-sm"))
    (subscribe-form {:bg :light})
    [:div.bg-gray-200.h-full.flex-grow
     [:div.h-10]
     [:div.text-center.text-2xl "Recent posts"]
     [:div.h-10]
     [:div.max-w-screen-sm.mx-auto.px-3
      (for [{:keys [title slug published-at description tags]} posts
            :when (not (tags "unlisted"))]
        [:a.block.mb-5.bg-white.rounded.p-3.hover:bg-gray-100.cursor-pointer
         {:href (str "/p/" slug "/")}
         [:div.text-sm.text-gray-800 (common/format-date "d MMM yyyy" published-at)]
         [:div.h-1]
         [:div.text-xl.font-bold title]
         [:div.h-1]
         [:div description]])]
     [:div.h-10]
     [:div.sm:text-center.text-sm.leading-snug.opacity-75.w-full.px-3
      (common/recaptcha-disclosure {:link-class "underline"})]
     [:div.h-3]]))

(defn landing-page [opts]
  (base-html
    opts
    (navbar opts)
    [:div.py-10.flex.flex-col.items-center.flex-grow.bg-center.px-3
     [:h1.font-bold.leading-tight.text-3xl.md:text-4xl.text-center
      {:class "max-w-[360px] sm:max-w-none leading-[1.15]"}
      "Biff helps solo developers move fast."]
     [:div.h-7]
     [:a.bg-accent.hover:bg-accent-dark.text-white.text-center.py-2.px-8.rounded.font-semibold.text-xl.md:text-2xl
      {:href (:docs-href opts "/docs/")}
      "Get Started"]
     [:div.h-7]
     [:div.mx-auto.text-xl.md:text-2xl.text-center.max-w-xl
      "Biff is a simple, easy, full-stack web framework for Clojure. "
      "Launch new projects quickly without getting bogged down in complexity later."]
     [:div.h-10]
     [:div.max-w-screen-sm.mx-auto.bg-primary.rounded.text-white.px-3.py-2.code
       [:span.text-blue-400 "# Create a new project:"] [:br]
       "bash <(curl -s https://biffweb.com/new-project.sh)"]]
    [:div.bg-gray-200.py-5
     [:div.max-w-screen-md.mx-auto.px-2
      [:div.text-lg.md:text-xl.text-center.mx-auto
       {:class "max-w-[520px]"}
       "Biff curates libraries and tools from across the ecosystem "
       "and composes them into one polished whole."]
      [:div.h-5]
      [:div.grid.sm:grid-cols-2.gap-4
       [:div.bg-white.p-3.rounded
        [:div.font-bold [:a.link {:href "https://xtdb.com/" :target "_blank"} "XTDB"]]
        [:div.h-1]
        [:div "Flexible data modeling, expressive Datalog queries, and immutable history. "
         "Biff adds schema enforcement with "
         [:a.link {:href "https://github.com/metosin/malli" :target "_blank"} "Malli"] "."]]
       [:div.bg-white.p-3.rounded
        [:div.font-bold [:a.link {:href "https://htmx.org/" :target "_blank"} "htmx"]]
        [:div.h-1]
        "Create interactive, real-time applications with HTML instead of JavaScript. Throw in a dash of "
        [:a.link {:href "https://hyperscript.org/" :target "_blank"} "hyperscript"]
        " for light frontend scripting."]
       [:div.bg-white.p-3.rounded
        [:div [:strong.text-blue-600 "Ready to deploy"]]
        [:div.h-1]
        [:div "Biff comes with code for setting up an Ubuntu VPS, including Git push-to-deploy, HTTPS certificates, "
         "and NGINX configuration."]]
       [:div.bg-white.p-3.rounded
        [:div [:strong.text-blue-600 "Develop in prod 😈"]]
        [:div.h-1]
        [:div "Whenever you save a file, any new code will be immediately hot swapped into the server."
         " (Don't worry, it's optional!)"]]]
      [:div.h-5]
      [:div.md:text-lg.text-center
       "...and several other carefully chosen pieces. Biff clocks in at under 2,000 lines of code. "
       "It's designed to be taken apart and modified, so it doesn't get in the "
       "way as your needs evolve."]
      [:div.h-5]]]
    [:div.bg-primary.py-5
     [:div.px-2.max-w-screen-md.mx-auto.w-full.text-white
      [:div.text-2xl.font-bold.text-center "Sponsors"]
      [:div.h-5]
      [:div.flex.items-center.justify-center.flex-col.sm:flex-row
       [:a {:href "https://juxt.pro/"
            :target "_blank"}
        [:img.bg-primary.sm:mt-4
         {:style {:height "60px"}
          :src "/img/juxt-logo.svg"}]]
       [:div.h-8.w-12]
       [:a {:href "https://www.clojuriststogether.org/"
            :target "_blank"}
        [:img.bg-primary
         {:style {:height "80px"}
          :src "/img/clj-together-logo.svg"}]]]
      [:div.h-8]
      [:div.flex.gap-4.mx-auto.justify-center
       (for [{:keys [img url]} sponsors]
         [:a {:href url :target "_blank"}
          [:img {:src (common/cached-img-url {:url img :w 160 :h 160})
                 :width "40px"
                 :height "40px"
                 :style {:border-radius "50%"}}]])]
      [:div.h-8]
      [:div.flex.justify-center
       [:a.bg-accent.hover:bg-accent-dark.text-white.text-center.py-2.px-4.rounded.md:text-lg
        {:href "https://github.com/sponsors/jacobobryant"}
        "Support Biff"]]
      [:div.h-8]]]))

(defn render-card [{:keys [site post] :as opts}]
  (base-html
    opts
    [:div.mx-auto.border.border-black
     {:style "width:1202px;height:620px"}
     [:div.flex.flex-col.justify-center.h-full.p-12
      [:div [:img {:src "/images/card-logo.png"
             :alt "Logo"
             :style {:max-height "60px"}}]]
      [:div {:class "h-[1.5rem]"}]
      [:h1.font-bold.leading-none
       {:class "text-[6rem]"}
       (str/replace (:title post) #"^\[draft\] " "")]
      [:div {:class "h-[2.5rem]"}]
      (byline (assoc post :byline/card true))]]))

(defn cards! [{:keys [posts] :as opts}]
  (doseq [post posts
          :let [path (str "/p/" (:slug post) "/card/")]]
    (common/render! path
                    "<!DOCTYPE html>"
                    (render-card (assoc opts :base/path path :post post)))))

(defn render-page [{:keys [site page account] :as opts}]
  (base-html
   opts
   (navbar opts)
   [:div.mx-auto.p-3.text-lg.flex-grow.w-full.max-w-screen-md
    [:div.post-content (raw-string (:html page))]]))

(def pages
  {"/" landing-page
   "/newsletter/" newsletter-page
   "/subscribed/" subscribed-page})

(defn assets!
  "Deprecated"
  []
  (->> (file-seq (io/file "assets"))
       (filter #(.isFile %))
       (run! #(io/copy % (doto (io/file "public" (subs (.getPath %) (count "assets/"))) io/make-parents)))))

(defn read-docs-markdoc []
  (let [files (->> (file-seq (io/file "docs"))
                   (filter #(.isFile %)))
        lines (->> files
                   (map (comp cheshire/generate-string slurp))
                   (str/join "\n"))
        result (shell/sh "node" "render-markdoc.js" :in lines)
        docs (->> (map (fn [file output]
                         (let [md-path (str/replace (.getPath file) "docs/" "")]
                           (assoc (cheshire/parse-string output true)
                                  :md-path md-path
                                  :path (-> md-path
                                            (str/replace #"\d\d-" "")
                                            (str/replace ".md" "")))))
                       files
                       (str/split-lines (:out result))))]
    docs))

(defn bold-code [text state]
  [(if (:codeblock state)
     (str/replace text
                  #"%%(.*)%%"
                  (fn [[_ text]]
                    (str "<span class=\"codeblock-highlight\">" text "</span>")))
     text)
   state])

(defn read-docs []
  (for [f (file-seq (io/file "docs"))
        :when (.isFile f)
        :let [content (slurp f)
              [front-matter content] (->> (str/split content #"---" 3)
                                          (keep (comp not-empty str/trim)))
              front-matter (yaml/parse-string front-matter)
              content (some-> content
                              (md/md-to-html-string
                               :heading-anchors true
                               :code-style #(str "class=\"language-" % "\"")
                               :custom-transformers [bold-code]))
              md-path (str/replace (.getPath f) "docs/" "")
              path (-> md-path
                       (str/replace #"\d\d-" "")
                       (str/replace ".md" ""))]]
    (merge front-matter
           {:md-path md-path
            :path path
            :html content})))

(defn join [sep xs]
  (rest (mapcat vector (repeat sep) xs)))

(defn assoc-some [m & kvs]
  (let [kvs (->> kvs
                 (partition 2)
                 (filter (comp some? second))
                 (apply concat))]
    (if (empty? kvs)
      m
      (apply assoc m kvs))))

(def pprint clojure.pprint/pprint)

(defn nav-href [doc]
  (if (nil? (:html doc))
    (:href (first (:children doc)))
    (str "/docs/" (:path doc) "/")))

(defn nav-doc [m]
  (->> m
       (sort-by (comp :md-path val))
       (map (fn [[_ doc]]
              (let [doc (update doc :children nav-doc)]
                (assoc-some
                 {:title (:title doc)
                  :href (nav-href doc)
                  :has-content (some? (:html doc))}
                 :children (not-empty (:children doc))))))))

(defn doc-nav-data [docs]
  (let [nav-data (->> docs
                      (map (fn [doc]
                             (assoc doc :segments (str/split (:path doc) #"/"))))
                      (reduce (fn [nav doc]
                                (update-in nav
                                           (join :children (:segments doc))
                                           merge
                                           doc))
                              {})
                      nav-doc)
        nodes (->> (tree-seq
                    :children
                    :children
                    {:children nav-data})
                   (filter :has-content))
        href->siblings (into {} (map (fn [prev cur next]
                                       [(:href cur) {:next next :prev prev}])
                                     (concat [nil nil] nodes)
                                     (concat [nil] nodes [nil])
                                     (concat nodes [nil nil])))
        nav-data (walk/postwalk
                  (fn [node]
                    (if-let [siblings (and (map? node) (href->siblings (:href node)))]
                      (merge node siblings)
                      node))
                  nav-data)]
    nav-data))

(defn assoc-nav-siblings [docs nav-data]
  (let [nodes (->> (tree-seq
                    :children
                    :children
                    {:children nav-data})
                   (filter :has-content))
        href->siblings (into {} (map (fn [prev cur next]
                                       [(:href cur) {:next next :prev prev}])
                                     (concat [nil nil] nodes)
                                     (concat [nil] nodes [nil])
                                     (concat nodes [nil nil])))]
    (map #(merge % (href->siblings (str "/docs/" (:path %) "/"))) docs)))

(defn sidebar-left [{:keys [doc-nav-data base/path]}]
  [:div
   [:div.w-fit.sticky.top-0.sm:whitespace-nowrap
    [:div.h-3]
    (for [{:keys [href title children has-content]} doc-nav-data
          :let [children (when (some #(= path (:href %)) children)
                           children)]]
      (list
       [:div.pb-3
        {:class (if (and has-content (= path href))
                  "text-accent-dark font-bold"
                  "font-semibold")}
        [:a.hover:underline {:href href} title]]
       (for [{:keys [href title]} children]
         [:div.border-l-2.px-3.py-1.text-sm
          {:class (if (= path href)
                    "border-accent text-accent-dark font-bold"
                    "font-medium")}
          [:a.hover:underline {:href href} title]])
       (when (not-empty children)
         [:div.h-3])))]])

(defn sidebar-right [opts]
  nil)

(def chevron-left
  [:svg.w-3.opacity-50 {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 384 512"}
   [:path {:d "M41.4 233.4c-12.5 12.5-12.5 32.8 0 45.3l192 192c12.5 12.5 32.8 12.5 45.3 0s12.5-32.8 0-45.3L109.3 256 278.6 86.6c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0l-192 192z"}]])

(def chevron-right
  [:svg.w-3.opacity-50 {:xmlns "http://www.w3.org/2000/svg", :viewbox "0 0 384 512"}
   [:path {:d "M342.6 233.4c12.5 12.5 12.5 32.8 0 45.3l-192 192c-12.5 12.5-32.8 12.5-45.3 0s-12.5-32.8 0-45.3L274.7 256 105.4 86.6c-12.5-12.5-12.5-32.8 0-45.3s32.8-12.5 45.3 0l192 192z"}]])

(defn render-api [{:keys [api-sections doc] :as opts}]
  (list
   [:p "All functions are located in the " [:code "com.biffweb"] " namespace."]
   (for [{:keys [arglists doc line name]} (api-sections (:section doc))
         :when (not-empty doc)]
     (list
      [:div.flex.items-baseline
       [:h3 {:id name} name]
       [:div.flex-grow]
       [:a.text-sm {:href (str "https://github.com/jacobobryant/biff/blob/master/src/com/biffweb.clj#L"
                               line)}
        "View source"]]
      [:pre.bg-white {:style {:padding "0"}}
       [:code
        (for [arglist arglists]
          (with-out-str
           (pprint (concat [name] arglist))))]]
      [:pre.text-sm
       [:code.language-plaintext (str/replace doc #"(?s)\n  " "\n")]]))))

(defn render-doc [{:keys [doc] :as opts}]
  (base-html
   (assoc opts :base/title (str (:title doc) " | Biff"))
   (navbar (assoc opts :class "max-w-[900px]"))
   [:div.mx-auto.p-3.flex-grow.w-full
    {:class "max-w-[900px]"}
    [:div.flex.gap-x-2.sm:gap-x-4
     (sidebar-left opts)
     [:div.min-w-0.w-full
      [:div.markdown-body
       [:h1 (:title doc)]
       (if-some [f (:render doc)]
         ((requiring-resolve (symbol f)) opts)
         (raw-string (:html doc)))
       [:div.h-10]
       [:div.text-center.italic.text-sm
        "Have a question? Join the #biff channel on "
        [:a {:href "http://clojurians.net" :target "_blank"} "Clojurians Slack"] "."]]
      [:div.h-2]
      [:hr]
      [:div.flex.my-5.items-end
       (when-some [prev (:prev doc)]
         (list chevron-left
               [:div.w-2]
               [:div.leading-none
                [:div.text-sm.text-gray-600 "Prev"]
                [:div.h-1]
                [:a.font-bold.text-lg.leading-none.hover:underline
                 {:href (:href prev)}
                 (:title prev)]]))
       [:div.flex-grow]
       (when-some [next (:next doc)]
         (list [:div.leading-none.text-right
                [:div.text-sm.text-gray-600 "Next"]
                [:div.h-1]
                [:a.font-bold.text-lg.leading-none.hover:underline
                 {:href (:href next)}
                 (:title next)]]
               [:div.w-2]
               chevron-right))]]
     (sidebar-right opts)]]))

(defn docs! [opts docs]
  (doseq [doc docs
          :when (some? (:html doc))
          :let [path (str "/docs/"
                          (-> (:path doc)
                              (str/replace #"\d\d-" "")
                              (str/replace ".md" ""))
                          "/")]]
    (common/render! path
                    "<!DOCTYPE html>"
                    (render-doc (assoc opts :base/path path :doc doc)))))

(defn -main []
  (let [opts (common/derive-opts (edn/read-string (slurp "input.edn")))
        docs (read-docs)
        doc-nav-data (doc-nav-data docs)
        docs (assoc-nav-siblings docs doc-nav-data)
        api-sections (->> (edn/read-string (slurp (io/resource "com/biffweb/theme/api.edn")))
                          (sort-by :line)
                          (group-by :section))
        opts (-> opts
                 (merge {:dev false
                         :doc-nav-data doc-nav-data
                         :api-sections api-sections
                         :docs-href (:href (first doc-nav-data))})
                 (update-in [:site :redirects] str "\n/docs/ " (:href (first doc-nav-data)) "\n"))]
    (common/redirects! opts)
    (common/netlify-subscribe-fn! opts)
    (common/pages! opts render-page pages)
    (common/posts! opts post-page)
    (common/atom-feed! (update opts :posts (fn [posts]
                                             (remove #(contains? (:tags %) "nofeed") posts))))
    (common/sitemap! {:exclude [#"/subscribed/" #".*/card/"]})
    (cards! opts)
    (assets!)
    (docs! opts docs)
    (when (fs/exists? "main.css")
      (io/make-parents "public/css/_")
      (common/safe-copy "main.css" "public/css/main.css"))
    (fs/copy-tree (io/file (io/resource "com/biffweb/theme/public"))
                  "public"
                  {:replace-existing true}))
  nil)
