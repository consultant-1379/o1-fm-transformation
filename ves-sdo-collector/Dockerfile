FROM armdocker.rnd.ericsson.se/proj_oss_releases/enm/eap7config

ENV JBOSS_HOME /ericsson/3pp/jboss

ADD target/ves-sdo-collector.war $JBOSS_HOME/standalone/deployments
ADD standalone-enm.xml $JBOSS_HOME/standalone/configuration/

RUN $JBOSS_HOME/bin/add-user.sh -a guest guest -g guest --silent

EXPOSE 8080 9990
CMD $JBOSS_HOME/bin/standalone.sh -c standalone-enm.xml -b 0.0.0.0 -bmanagement 0.0.0.0 --debug