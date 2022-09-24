---
title: Project Structure
---

A new Biff project will look like this:

(Throughout these docs, we'll assume you selected `com.example` for the main
namespace when creating your project.)

```text
├── README.md
├── config.edn
├── config.sh
├── deps.edn
├── resources
│   ├── public
│   │   └── img
│   │       └── glider.png
│   ├── tailwind.config.js
│   └── tailwind.css
├── setup.sh
├── src
│   └── com
│       ├── example
│       │   ├── feat
│       │   │   ├── app.clj
│       │   │   ├── auth.clj
│       │   │   ├── home.clj
│       │   │   └── worker.clj
│       │   ├── repl.clj
│       │   ├── schema.clj
│       │   ├── test.clj
│       │   └── ui.clj
│       └── example.clj
└── task
```

`task` is a shell script that contains project commands. For example,
`./task dev` starts the app locally, and `./task deploy` pushes your most recent commit
to the server. See `README.md` for a list of all the commands.

`config.sh` contains configuration for `task`, such as the project's main
namespace (`com.example` in this case) and the domain name of the server to
deploy to. `config.edn` contains configuration for the application.

`deps.edn` by default defines a single dependency: `com.biffweb/biff`. This
library is aliased as `biff` in most namespaces.

`setup.sh` is a script for provisioning an Ubuntu server. See [Production](/docs/reference/production/).

## Code organization

The example project is separated into three layers.

![code structure](/images/code-structure.svg)

We'll start with the middle layer. A feature namespace contains the routes,
static pages, scheduled tasks, and/or transaction listeners that pertain to a
particular feature. Each namespace exposes these things via a `features` map:

```clojure
(ns com.example.feat.some-feature
  ...)

(def features
  {:routes [...]
   :api-routes [...]
   :static {...}
   :tasks [...]
   :on-tx (fn ...)})
```

For example, the `com.example.feat.home` namespace defines a single route for
the landing page:

```clojure
(ns com.example.feat.home
  (:require [com.biffweb :as biff]
            [com.example.ui :as ui]))

(defn signin-form []
  ...)

(defn home [_]
  (ui/page
    {}
    (signin-form)))

(def features
  {:routes [["/" {:get home}]]})
```

The schema namespace defines the types of documents that are allowed to be
written to the database. Whenever you submit a transaction, it will
be checked against your schema first.

Here we define a `:user` document type which includes an email field and a
couple other string fields:

```clojure
(def schema
  {:user/id :uuid
   :user/email :string
   :user/foo :string
   :user/bar :string
   :user [:map {:closed true}
          [:xt/id :user/id]
          :user/email
          [:user/foo {:optional true}]
          [:user/bar {:optional true}]]
  ...})
```

The main namespace is the app's entry point. It bundles your schema and
features together. For example, here we combine all the routes and apply some
middleware:

```clojure
(def features
  [app/features
   auth/features
   home/features
   worker/features])

(def routes [["" {:middleware [anti-forgery/wrap-anti-forgery
                               biff/wrap-anti-forgery-websockets
                               biff/wrap-render-rum]}
              (map :routes features)]
             (map :api-routes features)])
```

Finally, "shared" namespaces contain code that's needed by multiple feature namespaces. The example
app has a single shared namespace, `com.example.ui`, which contains helper functions for
rendering HTML.
