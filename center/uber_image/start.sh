# center
nohup java -jar /center.jar --spring.profiles.active=docker &

# agent
java -jar /agent.jar --spring.profiles.active=docker