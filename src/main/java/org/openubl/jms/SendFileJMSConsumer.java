package org.openubl.jms;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openubl.factories.ModelFactory;
import org.openubl.models.SendFileModel;
import org.openubl.providers.SendFileWSProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class SendFileJMSConsumer implements Runnable {

    @Inject
    SendFileWSProvider sunatWSProvider;

    @Inject
    ConnectionFactory connectionFactory;

    @ConfigProperty(name = "openubl.sendFileQueue")
    String sendFileQueue;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer jmsConsumer = context.createConsumer(context.createQueue(sendFileQueue));
            while (true) {
                Message message = jmsConsumer.receive();
                if (message == null) {
                    return;
                }

                SendFileModel model = ModelFactory.getSendFilePropertiesModel(message);
                sunatWSProvider.sendFile(model, message.getBody(byte[].class));
            }
        } catch (JMSException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}