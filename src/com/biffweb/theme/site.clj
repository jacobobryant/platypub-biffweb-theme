(ns com.biffweb.theme.site
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [com.platypub.themes.common :as common]
            [hiccup.util :refer [raw-string]]))

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

(def nav-options
  [["Newsletter" "/newsletter/"]
   ["Docs" "/docs/"]
   ["API" "/api/com.biffweb.html"]
   ["Repo" "https://github.com/jacobobryant/biff"]
   ["Community" "/community/"]
   ["Consulting" "/consulting/"]])

(def navbar
  (list
    [:div.bg-primary.py-2
     [:div.flex.max-w-screen-lg.mx-auto.items-center.text-white.gap-4.text-lg.flex-wrap.px-3
      logo
      [:div.flex-grow]
      (for [[label href] nav-options]
        [:a.hover:underline.hidden.sm:block
         {:href href
          :target (when (= label "API")
                    "_blank")}
         label])
      hamburger-icon]]
    [:div#nav-menu.bg-primary.px-5.py-2.text-white.text-lg.hidden.transition-all.ease-in-out.sm:hidden
     (for [[label href] nav-options]
       [:div.my-2 [:a.hover:underline.text-lg {:href href} label]])]))

(defn byline [{:keys [published-at]}]
  [:div
   {:style {:display "flex"
            :align-items "center"}}
   [:img {:src (common/cached-img-url {:url "https://cdn.findka.com/profile.jpg"
                                       :w 200 :h 200})
          :width "50px"
          :height "50px"
          :style {:border-radius "50%"}}]
   [:div {:style {:width "0.75rem"}}]
   [:div
    [:div {:style {:line-height "1.25"}}
     [:a.text-blue-600.hover:underline
      {:href "https://jacobobryant.com/"
       :target "_blank"}
      "Jacob O'Bryant"]]
    [:div {:style {:line-height "1"
                   :font-size "90%"
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
  (common/base-html
    opts
    navbar
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
  (common/base-html
    (assoc opts :base/title "You're subscribed to Biff: The Newsletter")
    navbar
    [:div.max-w-screen-lg.mx-auto.p-3.w-full
     [:h1.font-bold.text-2xl "You're subscribed"]
     [:div "Check your inbox for a welcome email."]]))

(defn newsletter-page [{:keys [posts] :as opts}]
  (common/base-html
    (assoc opts :base/title "Biff: The Newsletter")
    navbar
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
  (common/base-html
    opts
    navbar
    [:div.py-10.flex.flex-col.items-center.flex-grow.bg-center.px-3
     [:h1.font-bold.leading-tight.text-4xl.text-center
      {:class "max-w-[300px] sm:max-w-none"}
      "Make your ideas come to life."]
     [:div.h-7]
     [:a.bg-accent.hover:bg-accent-dark.text-white.text-center.py-2.px-8.rounded.font-semibold.text-xl.sm:text-2xl
      {:href "/docs/"}
      "Get Started"]
     [:div.h-7]
     [:div.mx-auto.text-xl.sm:text-2xl.text-center.max-w-xl
      "Biff is a simple, easy, full-stack web framework for Clojure. "
      "Launch new projects quickly without getting bogged down in complexity later."]
     [:div.h-10]
     [:div.max-w-screen-sm.mx-auto.bg-primary.rounded.text-white.px-3.py-2.code
       [:span.text-blue-400 "# Create a new project:"] [:br]
       "bash <(curl -s https://biffweb.com/new-project.sh)"]]
    [:div.bg-gray-200.py-5
     [:div.max-w-screen-md.mx-auto.px-2
      [:div.text-lg.sm:text-xl.text-center.mx-auto
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
      [:div.sm:text-lg.text-center
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
       [:a.bg-accent.hover:bg-accent-dark.text-white.text-center.py-2.px-4.rounded.sm:text-lg
        {:href "https://github.com/sponsors/jacobobryant"}
        "Support Biff"]]
      [:div.h-8]]]))

(defn render-card [{:keys [site post] :as opts}]
  (common/base-html
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
      (byline post)]]))

(defn cards! [{:keys [posts] :as opts}]
  (doseq [post posts
          :let [path (str "/p/" (:slug post) "/card/")]]
    (common/render! path
                    "<!DOCTYPE html>"
                    (render-card (assoc opts :base/path path :post post)))))

(defn render-page [{:keys [site post account] :as opts}]
  (common/base-html
   opts
   navbar
   [:div.mx-auto.p-3.text-lg.flex-grow.w-full.max-w-screen-md
    [:div.post-content (raw-string (:html post))]]))

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

(defn -main []
  (let [opts (common/derive-opts (edn/read-string (slurp "input.edn")))]
    (common/redirects! opts)
    (common/netlify-subscribe-fn! opts)
    (common/pages! opts render-page pages)
    (common/posts! opts post-page)
    (common/atom-feed! opts)
    (common/sitemap! {:exclude [#"/subscribed/" #".*/card/"]})
    (cards! opts)
    (assets!)
    (when (fs/exists? "main.css")
      (io/make-parents "public/css/_")
      (common/safe-copy "main.css" "public/css/main.css")))
  nil)

(comment
 (-main))
