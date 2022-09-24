---
title: Create a project
---

Requirements:

 - JDK 11 or higher
 - [clj](https://clojure.org/guides/getting_started)

Run this command to create a new Biff project (and if you run into any
problems, see [Troubleshooting](/docs/reference/troubleshooting/)):

```bash
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

When you're ready to deploy, see [Production](/docs/reference/production/).

## Jacking in

`cider-jack-in` and similar commands will start up a JVM and an nREPL server
for you. However, `./task dev` already does that. Instead of running
`cider-jack-in`, you should run `cider-connect` (or the equivalent) so that you
can connect to the nREPL server started by `./task dev`. See
[Connecting to a Running nREPL Server](https://docs.cider.mx/cider/basics/up_and_running.html#connect-to-a-running-nrepl-server)
in the CIDER docs.

This does mean that CIDER will not be able to decide what version of the nREPL
server dependencies to use. If you run into problems, you'll need to set the
versions manually in `deps.edn`:

```clojure
{:deps {nrepl/nrepl       {:mvn/version "..."}
        cider/cider-nrepl {:mvn/version "..."}
...
```
