(ns com.biffweb.theme.config
  (:require [com.platypub.themes.default.config :refer [config]]))

(def site-fields
  [:com.platypub.site/description
   :com.platypub.site/image
   :com.platypub.site/redirects
   :com.platypub.site/author-name
   :com.platypub.site/author-url
   :com.platypub.site/author-image
   :com.platypub.site/embed-html])

(defn -main []
  (-> config
      (assoc :site-fields site-fields)
      prn))
