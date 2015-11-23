# Best iOS Games

I frequently want to check out the newest games available on iOS for train rides and bumming around on the couch, and I trust Metacritic ratings more than I trust "Editor's Choice" labels on the App Store. Metacritic has links to the App Store on the /:game#show pages, but they do not list the game's price.

This application scrapes Metacritic for the iOS "New Releases" section and joins it with App Store data returned by the iTunes Search API.

##ToDos
- turn this into a proper json api + SPA instead of the hacky html wrapping going on here. add styling
- do a better job caching results + artwork
- allow users to see all releases, not just new ones
- clean up the code and remove a couple of the hacks
