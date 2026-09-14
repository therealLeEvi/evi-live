# Draft architecture question for RuneLite maintainers

I am preparing EVI Live, a passive GE trade observation plugin, for possible Plugin Hub submission. Before submitting, I would like to clarify the rejected-features rule concerning plugins that expose player information over HTTP.

The plugin sends selected observations of the user's own GE offers to a fixed `127.0.0.1:51743` endpoint using outbound HTTP POST. It never starts an HTTP listener, accepts commands, clicks, changes offers, or reads chat/inventory/other players. It uses a separate local bridge token, no proxies or redirects, and salted account pseudonyms. The companion bridge stores those records and exposes an authenticated loopback API to its browser-based scanner.

Does the rejected-features rule exclude this complete architecture, including the separate bridge's local API? If so, would a user-enabled local trade-file export for the same companion application be an acceptable alternative, or is this integration outside the scope of the Hub regardless of transport?

I do not want to treat localhost or moving the HTTP listener into a companion process as a way around the rule. I can adjust the design openly before submission. The plugin source is at https://github.com/therealLeEvi/evi-live. It is a development candidate, not a submitted or approved Plugin Hub release. A clean public companion distribution is still being prepared; the complete bridge behavior is described above for the policy question.
