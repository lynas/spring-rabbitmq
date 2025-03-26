# Spring boot with Rabbit mq

- Run Docker desktop
- Run the project
- Go to rabbit mq management dashboard
- http://localhost:15672
  - if you see queue name based error go to queue tab in dashboard and create a new topic with type classic
  - Similarly, if you see any exchange name based error go to exchange tab in dashboard and add exchange with type topic
- Make post call to publish a message


``` 
POST http://localhost:8080/testPost
Content-Type: application/json

{
  "name": "name1"
} 
```

- See consumed message in log