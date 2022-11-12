---
title: Channels and messages
---

Now that users can create and join communities, we're ready to:

 - Let community admins create and delete channels
 - Let community members create messages

We'll start by adding a "New channel" button. But first, let's update
`com.eelchat.feat.app/wrap-community` so it adds the current user's roles to the
incoming request:

```diff
diff --git a/src/com/eelchat/feat/app.clj b/src/com/eelchat/feat/app.clj
index 6bb846f..69f3a37 100644
--- a/src/com/eelchat/feat/app.clj
+++ b/src/com/eelchat/feat/app.clj
@@ -55,9 +55,14 @@
         [:div {:class "grow-[1.75]"}]]))))

 (defn wrap-community [handler]
-  (fn [{:keys [biff/db path-params] :as req}]
+  (fn [{:keys [biff/db user path-params] :as req}]
     (if-some [community (xt/entity db (parse-uuid (:id path-params)))]
-      (handler (assoc req :community community))
+      (let [roles (->> (:user/mems user)
+                       (filter (fn [mem]
+                                 (= (:xt/id community) (get-in mem [:mem/comm :xt/id]))))
+                       first
+                       :mem/roles)]
+        (handler (assoc req :community community :roles roles)))
       {:status 303
        :headers {"location" "/app"}})))
```

Now in `com.eelchat.ui`, we can check if the user has the `:admin` role and show the button
if so:

```diff
diff --git a/src/com/eelchat/ui.clj b/src/com/eelchat/ui.clj
index 541d35f..be2dec2 100644
--- a/src/com/eelchat/ui.clj
+++ b/src/com/eelchat/ui.clj
@@ -38,7 +38,7 @@
      body]
     [:div {:class "grow-[2]"}]]))

-(defn app-page [{:keys [uri user] :as opts} & body]
+(defn app-page [{:keys [uri user community roles] :as opts} & body]
   (base
    opts
    [:.flex.bg-orange-50
@@ -60,6 +60,12 @@
                       url)}
          (:comm/title comm)])]
      [:.grow]
+     (when (contains? roles :admin)
+       [:<>
+        (biff/form
+         {:action (str "/community/" (:xt/id community) "/channel")}
+         [:button.btn.w-full {:type "submit"} "New channel"])
+        [:.h-3]])
      (biff/form
       {:action "/community"}
       [:button.btn.w-full {:type "submit"} "New community"])
```

![Screenshot of the "New channel" button](/img/tutorial/new-channel-button.png)

Next we'll add a handler so that the button actually does something. We'll also add a dummy
`channel-page` handler:

```diff
diff --git a/src/com/eelchat/feat/app.clj b/src/com/eelchat/feat/app.clj
index 69f3a37..a184841 100644
--- a/src/com/eelchat/feat/app.clj
+++ b/src/com/eelchat/feat/app.clj
@@ -31,6 +31,21 @@
   {:status 303
    :headers {"Location" (str "/community/" (:xt/id community))}})
 
+(defn new-channel [{:keys [community roles] :as req}]
+  (if (and community (contains? roles :admin))
+    (let [chan-id (random-uuid)]
+     (biff/submit-tx req
+       [{:db/doc-type :channel
+         :xt/id chan-id
+         :chan/title (str "Channel #" (rand-int 1000))
+         :chan/comm (:xt/id community)
+         :chan/type :chat
+         :chan/access :private}])
+     {:status 303
+      :headers {"Location" (str "/community/" (:xt/id community) "/channel/" chan-id)}})
+    {:status 403
+     :body "Forbidden."}))
+
 (defn community [{:keys [biff/db user community] :as req}]
   (let [member (some (fn [mem]
                        (= (:xt/id community) (get-in mem [:mem/comm :xt/id])))
@@ -54,6 +69,10 @@
          [:button.btn {:type "submit"} "Join this community"])
         [:div {:class "grow-[1.75]"}]]))))
 
+(defn channel-page [req]
+  ;; We'll update this soon
+  (community req))
+
 (defn wrap-community [handler]
   (fn [{:keys [biff/db user path-params] :as req}]
     (if-some [community (xt/entity db (parse-uuid (:id path-params)))]
@@ -66,10 +85,21 @@
       {:status 303
        :headers {"location" "/app"}})))
 
+(defn wrap-channel [handler]
+  (fn [{:keys [biff/db user community path-params] :as req}]
+    (let [channel (xt/entity db (parse-uuid (:chan-id path-params)))]
+      (if (= (:chan/comm channel) (:xt/id community))
+        (handler (assoc req :channel channel))
+        {:status 303
+         :headers {"Location" (str "/community/" (:xt/id community))}}))))
+
 (def features
   {:routes ["" {:middleware [mid/wrap-signed-in]}
             ["/app"           {:get app}]
             ["/community"     {:post new-community}]
             ["/community/:id" {:middleware [wrap-community]}
              [""      {:get community}]
-             ["/join" {:post join-community}]]]})
+             ["/join" {:post join-community}]
+             ["/channel" {:post new-channel}]
+             ["/channel/:chan-id" {:middleware [wrap-channel]}
+              ["" {:get channel-page}]]]]})
```

Now let's update `com.eelchat.ui/app-page` so that it displays the channels
in the sidebar if you're a member of the community:

```diff
diff --git a/src/com/eelchat/ui.clj b/src/com/eelchat/ui.clj
index be2dec2..2276dc3 100644
--- a/src/com/eelchat/ui.clj
+++ b/src/com/eelchat/ui.clj
@@ -1,6 +1,6 @@
 (ns com.eelchat.ui
   (:require [clojure.java.io :as io]
-            [com.biffweb :as biff]))
+            [com.biffweb :as biff :refer [q]]))
 
 (defn css-path []
   (if-some [f (io/file (io/resource "public/css/main.css"))]
@@ -38,7 +38,17 @@
      body]
     [:div {:class "grow-[2]"}]]))
 
-(defn app-page [{:keys [uri user community roles] :as opts} & body]
+(defn channels [{:keys [biff/db community roles]}]
+  (when (some? roles)
+    (sort-by
+     :chan/title
+     (q db
+        '{:find (pull channel [*])
+          :in [comm]
+          :where [[channel :chan/comm comm]]}
+        (:xt/id community)))))
+
+(defn app-page [{:keys [biff/db uri user community roles channel] :as opts} & body]
   (base
    opts
    [:.flex.bg-orange-50
@@ -59,6 +69,14 @@
           :selected (when (= url uri)
                       url)}
          (:comm/title comm)])]
+     [:.h-4]
+     (for [chan (channels opts)
+           :let [active (= (:xt/id chan) (:xt/id channel))]]
+       [:.mt-3 (if active
+                 [:span.font-bold (:chan/title chan)]
+                 [:a.link {:href (str "/community/" (:xt/id community)
+                                      "/channel/" (:xt/id chan))}
+                  (:chan/title chan)])])
      [:.grow]
      (when (contains? roles :admin)
        [:<>
```

If you create multiple channels, you should be able to navigate between them:

![Screenshot with several channels in the navigation sidebar](/img/tutorial/channels.png)

## Work in progress

That's all for now! I've just started writing this tutorial as of
28 October 2022. I'm planning to have the first draft of it complete by the end of
November. After the tutorial is complete, I'll make videos to go along with it.
