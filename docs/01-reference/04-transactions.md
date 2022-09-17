---
title: Transactions
---

*Biff uses [XTDB](https://xtdb.com/) for the database. It's OK if you haven't used XTDB before,
but you may want to peruse some of the [learning resources](https://xtdb.com/learn/) at least.*

The request map passed to HTTP handlers (and the scheduled tasks and
transaction listeners) includes a `:biff.xtdb/node` key which can be used to
submit transactions:

```clojure
(require '[xtdb.api :as xt])

(defn send-message [{:keys [biff.xtdb/node session params] :as req}]
  (xt/submit-tx node
    [[::xt/put {:xt/id (java.util.UUID/randomUUID)
                :msg/user (:uid session)
                :msg/text (:text params)
                :msg/sent-at (java.util.Date.)}]])
  ...)
```

Biff also provides a higher-level wrapper over `xtdb.api/submit-tx`. It lets
you specify document types from your schema. If the document you're trying to
write doesn't match its respective schema, the transaction will fail. In
addition, Biff will call `xt/await-tx` on the result, so you can read your
writes.

```clojure
(require '[com.biffweb :as biff])

(defn send-message [{:keys [session params] :as req}]
  (biff/submit-tx
    ;; select-keys is for illustration. Normally you would just pass in req.
    (select-keys req [:biff.xtdb/node :biff/malli-opts])
    [{:db/doc-type :message
      :msg/user (:uid session)
      :msg/text (:text params)
      :msg/sent-at (java.util.Date.)}])
  ...)
```

If you don't set `:xt/id`, Biff will use `(java.util.UUID/randomUUID)` as the default value.
The default operation is `:xtdb.api/put`.

You can delete a document by setting `:db/op :delete`:

```clojure
(defn delete-message [{:keys [params] :as req}]
  (biff/submit-tx req
    [{:xt/id (java.util.UUID/fromString (:msg-id params))
      :db/op :delete}])
  ...)
```

As a convenience, any occurrences of `:db/now` will be replaced with `(java.util.Date.)`:

```clojure
(defn send-message [{:keys [session params] :as req}]
  (biff/submit-tx req
    [{:db/doc-type :message
      :msg/user (:uid session)
      :msg/text (:text params)
      :msg/sent-at :db/now}])
  ...)
```

If you set `:db/op :update` or `:db/op :merge`, the document will be merged
into an existing document if it exists. The difference is that `:db/op :update` will
cause the transaction to fail if the document doesn't already exist.

```clojure
(defn set-foo [{:keys [session params] :as req}]
  (biff/submit-tx req
    [{:db/op :update
      :db/doc-type :user
      :xt/id (:uid session)
      :user/foo (:foo params)}])
  ...)
```

Biff uses `:xtdb.api/match` operations to ensure that concurrent
merge/update operations don't get overwritten. If the match fails, the
transaction will be retried up to three times.

When `:db/op` is set to `:merge` or `:update`, you can use special operations
on a per-attribute basis. These operations can use the attribute's previous
value, along with new values you provide, to determine what the final value
should be.

Use `:db/union` to coerce the previous value to a set and insert new values
with `clojure.set/union`:

```clojure
[{:db/op :update
  :db/doc-type :post
  :xt/id #uuid "..."
  :post/tags [:db/union "clojure" "almonds"]}]
```

Use `:db/difference` to do the opposite:

```clojure
[{:db/op :update
  :db/doc-type :post
  :xt/id #uuid "..."
  :post/tags [:db/difference "almonds"]}]
```

Add to or subtract from numbers with `:db/add`:

```clojure
[{:db/op :update
  :db/doc-type :account
  :xt/id #uuid "..."
  :account/balance [:db/add -50]}]
```

Use `:db/default` to set a value only if the existing document doesn't
already contain the attribute:

```clojure
[{:db/op :update
  :db/doc-type :user
  :xt/id #uuid "..."
  :user/favorite-color [:db/default :yellow]}]
```

Use `:db/dissoc` to remove an attribute:

```clojure
[{:db/op :update
  :db/doc-type :user
  :xt/id #uuid "..."
  :user/foo :db/dissoc}]
```

Finally, you can use `:db/lookup` to enforce uniqueness constraints on attributes
other than `:xt/id`:

```clojure
[{:db/doc-type :user
  :xt/id [:db/lookup {:user/email "hello@example.com"}]}]
```

This will use a separate "lookup document" that, if the user has been created already, will
look like this:

```clojure
{:xt/id {:user/email "hello@example.com"}
 :db/owned-by ...}
```

where `...` is a document ID. If the document doesn't exist, the ID will be `(java.util.UUID/randomUUID)`,
unless you pass in a different default ID with `:db/lookup`:

```clojure
[{:db/doc-type :user
  :xt/id [:db/lookup {:user/email "hello@example.com"} #uuid "..."]}]
```

If the first value passed along with `:db/lookup` is a map, it will get merged
in to the document. So our entire transaction would end up looking like this, assuming
the user document doesn't already exist:

```clojure
[{:db/doc-type :user
  :xt/id [:db/lookup {:user/email "hello@example.com"}]}]
;; =>
[[:xtdb.api/put {:xt/id #uuid "abc123"
                 :user/email "hello@example.com"}]
 [:xtdb.api/match {:user/email "hello@example.com"} nil]
 [:xtdb.api/put {:xt/id {:user/email "hello@example.com"}
                 :db/owned-by #uuid "abc123"}]]
```

If you need to do something that `biff/submit-tx` doesn't support (like setting
a custom valid time or using transaction functions), you can always drop down
to `xt/submit-tx`.

See also:

 - [XTDB learning resources](https://xtdb.com/learn/)
 - [XTDB transaction reference](https://docs.xtdb.com/language-reference/datalog-transactions/)
 - [`submit-tx`](https://github.com/jacobobryant/biff/blob/bdd1bd81d95ee36c615495a946c7c1aa92d19e2e/src/com/biffweb/impl/xtdb.clj#L247)
