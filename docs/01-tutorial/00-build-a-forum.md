---
title: Build a forum
---

To help you learn the ropes, we'll build a discussion application similar to
Discord, Slack, and the like. We'll call it *eelchat*.

In eelchat, users can create communities, each of which has a collection of
channels. There will be different types of channels:

- Chat, where all the messages are displayed in the same chronological feed.
- Forum, where messages are organized into separate topics.
- Threaded, where messages are organized into topics *and* topic replies are
  organized into a hierarchy.

Channels can be either public (anyone can view the messages, even if they're
not signed in) or private (only members of the community can view the
messages). There will some moderation tools; for example, you can require new
members' posts to be manually approved, you can set the community to be
invite-only or publicly accessible, etc. There will be RSS feeds for various
things like channels, communities, and notifications.

All the code for this tutorial can be found at
[github.com/jacobobryant/eelchat](https://github.com/jacobobryant/eelchat).
