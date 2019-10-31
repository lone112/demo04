Signed JWT Auth Example

login request.  

    $ curl -v -X POST -H "Content-Type: application/json" -d '{"username": "admin", "password": "secret"}' http://localhost:3000/login
    * Connected to localhost (::1) port 3000 (#0)
    > POST /login HTTP/1.1
    > Host: localhost:3000
    > User-Agent: curl/7.60.0
    > Accept: */*
    > Content-Type: application/json
    > Content-Length: 43
    >
    * upload completely sent off: 43 out of 43 bytes
    < HTTP/1.1 200 OK
    < Date: Thu, 31 Oct 2019 03:26:11 GMT
    < Content-Type: application/json;charset=utf-8
    < Content-Length: 164
    < Server: Jetty(9.4.12.v20180830)
    <
    {"token":"eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoiYWRtaW4iLCJleHAiOjE1NzI0OTU5NzF9.EkbTvdUw71uGI54GY8Ia2o5JjC3aaw8K4eIedW_2qgxumUENDiipw40sDeDb7r7IzZH9r-Qmb1MyBeh3MJ6AmQ"}
    * Connection #0 to host localhost left intact
    
Authenticated request.

    $ curl -v -X GET -H "Content-Type: application/json" -H "Authorization: Token eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXUyJ9.eyJ1c2VyIjoiYWRtaW4iLCJleHAiOjE0NTE5MTg5NzB9.Kvpr1jW7JBCZYUlFjAf7xnqMZSTpSVggAgiZ6_RGZuTi1wUuP_-E8MJff23GuCwpT9bbbHNTk84uV2cdg7rKTw" http://localhost:3000/api/tag
    * Connected to localhost (::1) port 3000 (#0)
    > GET /api/tag HTTP/1.1
    > Host: localhost:3000
    > User-Agent: curl/7.60.0
    > Accept: */*
    > Content-Type: application/json
    > Authorization: Token eyJhbGciOiJIUzUxMiJ9.eyJ1c2VyIjoiYWRtaW4iLCJleHAiOjE1NzI0OTU5NzF9.EkbTvdUw71uGI54GY8Ia2o5JjC3aaw8K4eIedW_2qgxumUENDiipw40sDeDb7r7IzZH9r-Qmb1MyBeh3MJ6AmQ
    >
    < HTTP/1.1 200 OK
    < Date: Thu, 31 Oct 2019 03:27:45 GMT
    < Content-Type: application/json;charset=utf-8
    < Content-Length: 411
    < Server: Jetty(9.4.12.v20180830)
    <
    [{"name":"Wechat","icon":"wechat.png","id":"5db1192dbe9cfa37b0cfedca"},{"name":"R7D","id":"5db119d8be9cfa37b0cfedcb"},{"name":"R30D","id":"5db11a36b85458ef26ead598"},{"name":"R24H","id":"5db11a36b85458ef26ead599"},{"name":"Level1","id":"5db11a36b85458ef26ead59a"},{"name":"Level2","id":"5db11a36b85458ef26ead59b"},{"name":"Level3","id":"5db11a36b85458ef26ead59c"},{"name":"VIP","id":"5db11a36b85458ef26ead59d"}]
    * Connection #0 to host localhost left intact