---
title: Authentication
---

The authentication code is kept entirely within the template project at
`com.example.feat.auth`. Biff uses email sign-in links instead of passwords.
When you create a new project, a secret token is generated and stored in
`config.edn`, under the `:biff/jwt-secret` key. When a user wants to
authenticate, they enter their email address, and then your secret token is used
to sign a JWT which is then embedded in a link and sent to the user's email
address. When they click on the link, their user ID is added to their session
cookie. By default the link is valid for one hour and the session lasts for 60
days.

You can get the user's ID from the session like so:

```clojure
(defn whoami [{:keys [session biff/db]}]
  (let [user (xt/entity db (:uid session))]
    [:html
     [:body
      [:div "Signed in: " (some? user)]
      [:div "Email: " (:user/email user)]]]))

(def features
  {:routes [["/whoami" {:get whoami}]]})
```

In a new Biff project, the sign-in link will be printed to the console. To have
it get sent by email, you'll need to include an API key for
[MailerSend](https://www.mailersend.com/) under the `:mailersend/api-key` key
in `config.edn`. It's also pretty easy to use a different service like
[Mailgun](https://www.mailgun.com/) if you prefer.

Some applications that use email sign-in links are vulnerable to login CSRF,
wherein an attacker requests a sign-in link for their own account and then
sends it to the victim. If the victim clicks the link and doesn't notice
they've been signed into someone else's account, they might reveal private
information. Biff prevents login CSRF by checking that the link is clicked on
the same device it was requested from.

It is likely you will need to protect your sign-in form against bots. The
template project includes backend code for reCAPTCHA v3, which does invisible
bot detection (i.e. no need to click on pictures of cars; instead Google just
analyzes your mouse movements etc). See [this
page](https://developers.google.com/recaptcha/docs/v3) for instructions on
adding the necessary code to the frontend. You can enable the backend
verification code by setting `:recaptcha/secret-key` in `config.edn`.

For added protection (and to help catch incorrect user input), you can also use
an email verification API like
[Mailgun's](https://documentation.mailgun.com/en/latest/api-email-validation.html).

See also:

 - [`com.example.feat.auth`](https://github.com/jacobobryant/biff/blob/bdd1bd81d95ee36c615495a946c7c1aa92d19e2e/example/src/com/example/feat/auth.clj)
 - [`com.biffweb/mailersend`](https://github.com/jacobobryant/biff/blob/bdd1bd81d95ee36c615495a946c7c1aa92d19e2e/src/com/biffweb.clj#L213)
 - [Mailersend](https://www.mailersend.com/)
 - [Mailgun](https://www.mailgun.com/)
