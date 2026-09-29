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