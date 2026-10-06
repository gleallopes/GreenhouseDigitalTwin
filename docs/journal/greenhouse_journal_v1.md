# Greenhouse Digital Twin version 1.0

**Company:** Autonomous <br>
**Name:** Gabriel Lopes <br>
**Age:** 24 <br>
**Email:** gleallopes17@gmail.com <br>
**Country/State:** Brazil-RS <br>
**Github:** <a a href=https://github.com/gleallopes>gleallopes</a> <br>
**Linkedin:** <a a href=https://www.linkedin.com/in/gabriel-lopes-b706373a2/>My Profile </a><br>
**Creation Date:** 2026/09/29 UTC

## Update 2026.09.29

Today I started implementing all the designed and projected architecure, following all the defined ADRs.<br>
I'm using ChatGPT as my "mentor" and pair-programming to guiding me through the entire project <br> that we established. I am using AI to learn more efficiently how the things are done, <br> connected and why. I'm not using AI to replace my own skills, but to extend it. <br>

### What was done so far
- Created all the project directories
- Created all Spring Boot dependencies using Maven
- Written a brief README description of the project
- Built a docker-compose file for postgresql and mosquitto
- Ran and tested the container
- Used maven to test and run the spring boot application
- Created java class for telemetry contracts established by our ADRs and DTs
- The contracts was tested by junit

### What was learned

Today I learned how to make the firsts steps on the application. This is very useful step because these steps are the same made on every application:
- structure directories accordingly of architecture;
- create pom file;
- configure environment for datasource, jpa;
- handle some errors like
- configure docker-compose
- test docker containers
- test and run application

## Update 2026.09.30

Today I moved on telemetry and implemented 3 validation layers:
- Structure Validation;
- Semantic Validation;
- Source Validation;

and also tested these layers using a mock for inject dependency.

### What Was Learned
- Use Mock to inject dependency;
- Write classes to validate telemetry;
- Treat Incoming telemetry as untrusted before parse into domain;
- Write tests to test scenarios;

## Update 2026.10.01

Today I finished implementing the validations for incoming telemetry and was very productive.<br>
I could understand the system's flow and dependencies injection practically.

### What was learned
- Handle errors by myself
- Implement the concepts of Plausibility Validation by myself
- Test these Plausibility Validation by myself

## Update 2026.10.05

Today I created the first postgres table using flyway, learned JPA and created a mapper to convert a <br>
domain entity into postgres data. I had a CVE alert when was adding flyway to maven, so I searched how to track <br>
dependencies to se which one was related to the alert and to take the appropriate action to fix it instead of just <br>
surppressing the alert.

### What was learned
- Use flyway to create tables
- how JPA is used to persist data
- How to track dependency error and make the appropriate choice to fix it.