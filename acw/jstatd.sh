cd /usr/java/jdk1.8.0_66/jre/lib/security
jstatd -J-Djava.security.policy=jstatd.all.policy -J-Djava.rmi.server.hostname=10.9.2.106 &
