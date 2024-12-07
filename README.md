# big data

if you have time.... there is stuff that needs to be done 😇:
- [ ] download 2000 books check if you can upload to github/ zip file
- [ ] index those 2000 books
- [ ] figure out git compose
- [ ] connnect hazelcast with API
- [ ] setup nginx
- [ ] figure out automatic changing of IP adresses so computers would connect at the same network (fe parameter for docker image)
- [ ] check if hazelcast handels 2000 books (should, but?)
- [ ] images for crawler, indexer
- [ ] imaage for API with Hazel
- [ ] fix up hazel query engine to do same as search engine (**Radka** will try, for now we concentrated on making hazelcast work...)
- [ ] implement that first computer in network puts index into hazelcast (or decide on other way how&when to put stuff into hazelcast)

#### also here is how Anna&Radka think it should work:
- crawling, indexing stays the same as stage 2 (just need to do 2000 books) 
- 1 computer uploads index from directory to hazelcast map (for now implemented in HazelQueryEngine)
- client sends a query to nginx which distributes the queries (make sure filtering works)

also we had questions about how its distributed and why we need both hazel & nginx sooo:
- hazelcast distributes the index data into all computers
- when a query comes nginx decides which computer is taking the query
