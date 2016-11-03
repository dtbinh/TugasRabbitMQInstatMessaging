import com.rabbitmq.client.Channel;

import java.io.IOException;

/**
 * Created by nim_13512501 on 11/3/16.
 */
public class Client {
    Channel channel;
    String queue;
    String notifQueue;
    Account account;

    public Client(Channel channel, String queue, Account account, String notifQueue) throws IOException {
        this.channel = channel;
        this.queue = queue;
        this.account = account;
        this.notifQueue = notifQueue;
        if (account != null){
            account.client = this;
        }
    }

    public void send(String message){
        try {
            channel.basicPublish("", queue, null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notify(String notification) {
        try {
            System.out.println("notify client");
            channel.basicPublish("", notifQueue, null, notification.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void associate(Account account){
        this.account = account;
        account.client = this;
    }
}
