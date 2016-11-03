import com.rabbitmq.client.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Created by nim_13512501 on 11/3/16.
 */
public class TugasRabbitMQClient {
    public static void main(String[] argv) throws Exception {
        String RabbitMQServerHostName;
        int RabbitMQServerPort;
        String ServerQueueName;
        try{
            RabbitMQServerHostName = argv[0];
            RabbitMQServerPort = Integer.parseInt(argv[1]);
            ServerQueueName = argv[2];
        }catch(Exception e){
            System.out.println("please provide args: [RabbitMQServerHostName] [RabbitMQServerPort] [ServerQueueName]");
            return;
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMQServerHostName);
        factory.setPort(RabbitMQServerPort);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(ServerQueueName, false, false, false, null);
        AMQP.Queue.DeclareOk clientQueueInfo = channel.queueDeclare();
        String clientQueue = clientQueueInfo.getQueue();
        Scanner sc = new Scanner(System.in);

        TugasRabbitMQClient tugasRabbitMQClient = new TugasRabbitMQClient(clientQueue,ServerQueueName,channel, sc);

        tugasRabbitMQClient.mainLoop();

        String message = "";
        System.out.println(" [x] Sent '" + message + "'");
        channel.close();
        connection.close();
    }

    public TugasRabbitMQClient(String clientQueue, String serverQueue, Channel channel, Scanner sc) throws IOException {
        this.clientQueue = clientQueue;
        this.serverQueue = serverQueue;
        this.channel = channel;
        this.sc = sc;
        queueingConsumer = new QueueingConsumer(channel);
        channel.basicConsume(clientQueue,true,queueingConsumer);
    }

    String clientQueue;
    String serverQueue;
    Channel channel;
    Scanner sc;
    QueueingConsumer queueingConsumer;
    public void mainLoop() throws InterruptedException {
        while (userAction()){}
    }

    public boolean userAction() throws InterruptedException {
        System.out.println("MENU:\n" +
                "1. login\n" +
                "2. register\n" +
                "3. add friend\n" +
                "4. create group\n" +
                "5. add group member\n" +
                "6. kick group member\n" +
                "7. send message\n"+
                "0. exit\n" +
                "type in number:");
        if (sc.hasNextInt()){
            int choice = sc.nextInt();
            switch(choice){
                case 1:
                    login();
                    break;
                case 2:
                    register();
                    break;
                case 3:
                    addFriend();
                    break;
                case 4:
                    createGroup();
                    break;
                case 5:
                    addGroupMember();
                    break;
                case 6:
                    kickGroupMember();
                    break;
                case 7:
                    sendMessage();
                    break;
                case 0:
                    return false;
            }
        }
        return true;
    }

    private void addGroupMember() throws InterruptedException {
        System.out.println("ADD GROUP MEMBER");

        String groupName = pickAdministeredGroup();
        if (groupName==null) return;
        String accountName = askAccountName();
        if (accountName==null) return;

        addGroupMember(groupName,accountName);
    }

    private void kickGroupMember() throws InterruptedException {
        System.out.println("KICK GROUP MEMBER");

        String groupName = pickAdministeredGroup();
        if (groupName==null) return;
        String accountName = pickGroupMember(groupName);
        if (accountName==null) return;

        kickGroupMember(groupName,accountName);
    }

    private String pickGroup() throws InterruptedException {
        JSONObject op =createOpJSON(OpNum.GET_GROUPS);
        send(op.toJSONString());

        String response = receive();
        JSONObject responseJSON = createResponseJSON(response);

        if ((Long)responseJSON.get("status")==0){
            System.out.println(responseJSON.get("message"));
            return null;
        }else{
            List<String> groupNames = (List<String>) responseJSON.get("groups");
            return pickNameFromListWithCancel(groupNames);
        }
    }

    private String pickAdministeredGroup() throws InterruptedException {
        JSONObject op =createOpJSON(OpNum.GET_ADMINISTERED_GROUPS);
        send(op.toJSONString());

        String response = receive();
        JSONObject responseJSON = createResponseJSON(response);

        if ((Long)responseJSON.get("status")==0){
            System.out.println(responseJSON.get("message"));
            return null;
        }else{
            List<String> groupNames = (List<String>) responseJSON.get("groups");
            return pickNameFromListWithCancel(groupNames);
        }
    }

    private String pickGroupMember(String groupName) throws InterruptedException{
        JSONObject op = createOpJSON(OpNum.GET_GROUP_MEMBERS);
        op.put("groupName",groupName);
        send(op.toJSONString());

        String response = receive();
        JSONObject responseJSON = createResponseJSON(response);

        if ((Long)responseJSON.get("status")==0){
            System.out.println(responseJSON.get("message"));
            return null;
        }else{
            List<String> groupNames = (List<String>) responseJSON.get("members");
            return pickNameFromListWithCancel(groupNames);
        }
    }

    private void addGroupMember(String groupName, String accountName) throws InterruptedException {
        JSONObject op = createOpJSON(OpNum.ADD_MEMBER);
        op.put("groupName",groupName);
        op.put("accountName",accountName);
        send(op.toJSONString());

        receiveStandardResponse();
    }

    private void kickGroupMember(String groupName, String memberName) throws InterruptedException {
        JSONObject op = createOpJSON(OpNum.KICK_MEMBER);
        op.put("groupName",groupName);
        op.put("memberName",memberName);
        send(op.toJSONString());

        receiveStandardResponse();
    }

    private void createGroup() throws InterruptedException {
        System.out.println("CREATE GROUP\n" +
                "type in name (no spaces): ");
        String name = sc.next();

        JSONObject op = createOpJSON(OpNum.CREATE_GROUP);
        op.put("name",name);


        //members
        JSONArray members = new JSONArray();
        int choice;
        do{
            System.out.println("members: " + members);
            System.out.println("1. add member\n2. continue create group\n" +
                    "type in number");
            choice = sc.nextInt();

            if (choice==1){
                String memberName = askAccountName();
                if (memberName!=null)
                    members.add(memberName);
            }

        }while (choice!=2);
        op.put("members",members);

        send(op.toJSONString());

        //receive response
        receiveStandardResponse();
    }

    public void sendMessage() throws InterruptedException {
        System.out.println("SEND MESSAGE\n" +
                "1. Private\n" +
                "2. Group\n" +
                "0. cancel\n" +
                "type in number:");
        int choice = sc.nextInt();

        JSONObject op = createOpJSON(OpNum.SEND_MESSAGE);
        switch(choice){
            case 1:
                String destinationName = askAccountName();
                if (destinationName==null) return;
                op.put("type","private");
                op.put("destinationName",destinationName);
                break;
            case 2:
                String groupName = pickGroup();
                if (groupName==null) return;
                op.put("type","group");
                op.put("groupName",groupName);
                break;
            case 0:
                return;
            default:
                return;
        }
        String message = getMessage();
        op.put("message",message);

        send(op.toJSONString());

        //receive response
        receiveStandardResponse();
    }

    public String askAccountName() throws InterruptedException {
        JSONObject op = createOpJSON(OpNum.GET_FRIENDS);
        send(op.toJSONString());

        JSONObject response = createResponseJSON(receive());
        if ((Long)response.get("status")==1){
            List<String> nameList = (List<String>) response.get("friends");
            return pickNameFromList(nameList);
        }else{
            System.out.println(response.get("message"));
            return null;
        }
    }

    public String pickNameFromList(List<String> nameList){
        for (int i=0;i<nameList.size();i++){
            System.out.println(""+(i+1)+". "+nameList.get(i));
        }
        System.out.println("0. type in name");
        int choice = sc.nextInt();
        if (choice == 0){
            System.out.println("type in name (no spaces): ");
            return sc.next();
        }else if (choice >0 && choice<=nameList.size()){
            return nameList.get(choice-1);
        }else{
            return null;
        }
    }

    public String pickNameFromListWithCancel(List<String> nameList){
        for (int i=0;i<nameList.size();i++){
            System.out.println(""+(i+1)+". "+nameList.get(i));
        }
        System.out.println("0. cancel");
        int choice = sc.nextInt();
        if (choice >0 && choice<=nameList.size()){
            return nameList.get(choice-1);
        }else{
            return null;
        }
    }

    public String getMessage(){
        System.out.println("type in message (end with ENTER):");
        String message;
        do{
            message = sc.nextLine();
        }while (message.isEmpty());
        return message;
    }

    public void login() throws InterruptedException {
        System.out.println("login Name: ");
        String loginName = sc.next();
        System.out.println("password: ");
        String password = sc.next();

        //send command
        JSONObject opJSON = createOpJSON(OpNum.LOGIN);
        opJSON.put("loginName",loginName);
        opJSON.put("password",password);
        send(opJSON.toJSONString());

        //receive response
        String response = receive();
        JSONObject responseJSON = createResponseJSON(response);
        Long status = (Long) responseJSON.get("status");
        if (status==1){
            System.out.println("success");

            //display notifications
            JSONArray notif = (JSONArray) responseJSON.get("notif");
            System.out.println("NOTIFICATIONS: " + notif.size() + " new");
            for (Object str : notif){
                System.out.println(str);
            }
        }else{
            System.out.println(responseJSON.get("message"));
        }
    }

    public void register() throws InterruptedException {
        System.out.println("login Name (no spaces): ");
        String loginName = sc.next();
        System.out.println("password (no spaces): ");
        String password = sc.next();

        //send command
        JSONObject opJSON = createOpJSON(OpNum.REGISTER);
        opJSON.put("loginName",loginName);
        opJSON.put("password",password);
        send(opJSON.toJSONString());

        //receive response
        receiveStandardResponse();
    }

    public void addFriend() throws InterruptedException {
        System.out.println("friend name:");
        String friendName = sc.next();

        //send command
        JSONObject op = createOpJSON(OpNum.ADD_FRIEND);
        op.put("loginName",friendName);
        send(op.toJSONString());

        //receive response
        receiveStandardResponse();
    }

    public void receiveStandardResponse() throws InterruptedException {
        String response = receive();
        JSONObject responseJSON = createResponseJSON(response);
        Long status = (Long) responseJSON.get("status");
        if (status==1){
            System.out.println("success");
        }else{
            System.out.println(responseJSON.get("message"));
        }
    }

    public JSONObject createOpJSON(int opNum){
        JSONObject retval = new JSONObject();
        retval.put("clientQueue", clientQueue);
        retval.put("opNum",opNum);
        return retval;
    }

    public JSONObject createResponseJSON(String response){
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(response);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public void send(String message){
        try {
            channel.basicPublish("", serverQueue, null, message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receive() throws InterruptedException {
        String recv;
        recv = new String(queueingConsumer.nextDelivery().getBody());
        System.out.println("received: " + recv);
        return recv;
    }
}
