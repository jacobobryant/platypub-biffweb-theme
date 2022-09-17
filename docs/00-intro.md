---
title: Introduction
---

Biff is designed to make web development with Clojure fast and easy without
compromising on simplicity. It prioritizes small-to-medium sized projects.

Biff has two parts: a library and a template project. As much code as
possible is written as library code, exposed under the `com.biffweb` namespace.
This includes a lot of high-level helper functions for other libraries.

The template project contains the framework code&mdash;the stuff that glues all
the libraries together. When you start a new Biff project, the template project code is
copied directly into your project directory, and the library is added as a regular
dependency.

Some of Biff's most distinctive features:

- Built on [XTDB](https://xtdb.com/), the world's finest database. It has
  flexible data modeling, Datalog queries, and immutable history. You can use
  the filesystem for the storage backend in dev and switch to Postgres for
  production.
- Uses [htmx](https://htmx.org/) (and [hyperscript](https://hyperscript.org/))
  for the frontend. Htmx lets you create interactive, real-time applications by
  sending HTML snippets from the server instead of using
  JavaScript/ClojureScript/React.
- Ready to deploy. The template project comes with a script for provisioning an
  Ubuntu server, including Git push-to-deploy, HTTPS certificates, and NGINX
  configuration.
- Develop in prod. If you choose to enable this, you can develop your entire
  application without ever starting up a JVM on your local machine. Whenever
  you hit save, files get rsynced to the server and evaluated.

Other things that Biff wraps/includes:

- [Rum](https://github.com/tonsky/rum) and [Tailwind CSS](https://tailwindcss.com/) for rendering.
- [Jetty](https://github.com/sunng87/ring-jetty9-adapter) for the web server
  and [Reitit](https://github.com/metosin/reitit) for routing.
- [Malli](https://github.com/metosin/malli) for enforcing schema when submitting XTDB transactions.
- [Buddy](https://funcool.github.io/buddy-sign/latest/) for email link authentication (JWTs).
- [Chime](https://github.com/jarohen/chime) for scheduling tasks.
- A minimalist, 15-line dependency injection framework, similar in spirit to Component.

We use Biff over at [The Sample](https://thesample.ai/), a relatively young
two-person business. It has about 13k lines of code.

## Getting Started

Requirements:

 - JDK 11 or higher
 - [clj](https://clojure.org/guides/getting_started)

Run this command to create a new Biff project (and if you run into any
problems, see [Troubleshooting](#troubleshooting)):

```
bash <(curl -s https://biffweb.com/new-project.sh)
```

This will create a minimal CRUD app which demonstrates most of Biff's features.
Run `./task dev` to start the app on `localhost:8080`. Whenever you save a file,
Biff will:

 - Evaluate any changed Clojure files (and any files which depend on them)
 - Regenerate static HTML and CSS files
 - Run tests

You can connect your editor to nREPL port 7888. There's also a `repl.clj` file
which you can use as a scratch space.

When you're ready to deploy, see [Production](#production).

### Jacking in

`cider-jack-in` and similar commands will start up a JVM and an nREPL server
for you. However, `./task dev` already does that. Instead of running
`cider-jack-in`, you should run `cider-connect` (or the equivalent) so that you
can connect to the nREPL server started by `./task dev`. See [Connecting to a
Running nREPL
Server](https://docs.cider.mx/cider/basics/up_and_running.html#connect-to-a-running-nrepl-server)
in the CIDER docs.

This does mean that CIDER will not be able to decide what version of the nREPL
server dependencies to use. If you run into problems, you'll need to set the
versions manually in `deps.edn`:

```clojure
{:deps {nrepl/nrepl       {:mvn/version "..."}
        cider/cider-nrepl {:mvn/version "..."}
...
```
