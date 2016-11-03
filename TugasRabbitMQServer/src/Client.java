import com.rabbitmq.client.Channel;

import java.io.IOException;

/**
 * Created by nim_13512501 on 11/3/16.
 */
public class Client {
    Channel channel;
    String queue;
    Account account;

    public Client(Channel channel, String queue, Account account) throws IOException {
        this.channel = channel;
        this.queue = queue;
        this.account = account;
    }

    public void send(String message){
        try {
            channel.basicPublish("", queue, null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
