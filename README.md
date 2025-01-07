# big data

if you have time.... there is stuff that needs to be done 😇:
- [ ] download 2000 books check if you can upload to github/ zip file
- [ ] Optimize the filePerWordInvertedIndex by using threads (Eduardo will take that part)
- [ ] index those 2000 books (Eduardo will take that part once he has the books)
- [ ] figure out git compose
- [ ] connnect hazelcast with API
- [ ] setup nginx (Eduardo will take that. Note: it should not take too much but it depends on the IP of the computers' lab and the port in which the API will be operating)
- [x] figure out automatic changing of IP adresses so computers would connect at the same network (fe parameter for docker image) <- wildcard works 192.168.186.*
- [ ] check if hazelcast handels 2000 books (should, but?)
- [ ] images for crawler, indexer
- [ ] imaage for API with Hazel
- [x] fix up hazel query engine to do same as search engine
- [ ] implement that first computer in network puts index into hazelcast (or decide on other way how&when to put stuff into hazelcast). (Eduardo is currently working on how to read the whole datamart and trying to "upload it" to hazelcast. This could be a lengthy task but I will not know until I have it finished)

#### also here is how Anna&Radka think it should work:
- crawling, indexing stays the same as stage 2 (just need to do 2000 books) 
- 1 computer uploads index from directory to hazelcast map (for now implemented in HazelQueryEngine)
- client sends a query to nginx which distributes the queries (make sure filtering works)

also we had questions about how its distributed and why we need both hazel & nginx sooo:
- hazelcast distributes the index data into all computers
- when a query comes nginx decides which computer is taking the query
