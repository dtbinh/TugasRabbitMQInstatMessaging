import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by nim_13512501 on 11/3/16.
 */
public class TugasRabbitMQServer {
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
        channel.queuePurge(ServerQueueName);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        TugasRabbitMQServer tugasRabbitMQServer = new TugasRabbitMQServer(ServerQueueName,channel);
        tugasRabbitMQServer.mainLoop();
    }



    public TugasRabbitMQServer(String serverQueue, Channel channel) throws IOException {
        this.serverQueue = serverQueue;
        this.channel = channel;
        consumer = new QueueingConsumer(channel);
        channel.basicConsume(serverQueue, true, consumer);
        clients = new HashMap<String,Client>();
        accountTable = new AccountTable();
        groupTable = new GroupTable();
    }

    String serverQueue;
    QueueingConsumer consumer;
    Channel channel;
    AccountTable accountTable;
    GroupTable groupTable;
    /**map of (clientQueue,loginName)*/
    Map<String,Client> clients;

    public void mainLoop() throws InterruptedException {
        while (true) {
            try{
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                String message = new String(delivery.getBody());
                System.out.println(" [x] Received '" + message + "'");
                JSONObject op = createOpJSON(message);
                Long opNum = (Long) op.get("opNum");
                switch(opNum.intValue()){
                    case OpNum.LOGIN:
                        login(op);
                        break;
                    case OpNum.REGISTER:
                        register(op);
                        break;
                    case OpNum.ADD_FRIEND:
                        addFriend(op);
                        break;
                    case OpNum.GET_FRIENDS:
                        getFriends(op);
                        break;
                    case OpNum.CREATE_GROUP:
                        createGroup(op);
                        break;
                    case OpNum.ADD_MEMBER:
                        addMember(op);
                        break;
                    case OpNum.KICK_MEMBER:
                        kickMember(op);
                        break;
                    case OpNum.SEND_MESSAGE:
                        sendMessage(op);
                        break;
                    case OpNum.GET_GROUPS:
                        getGroups(op);
                        break;
                    case OpNum.GET_ADMINISTERED_GROUPS:
                        getAdministeredGroups(op);
                        break;
                    case OpNum.GET_GROUP_MEMBERS:
                        getGroupMembers(op);
                        break;
                    case OpNum.POP_NOTIFS:
                        popNotifs(op);
                        break;
                    default:
                        invalidOp(op);
                        break;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void popNotifs(JSONObject op) throws IOException {
        Client client = prepareClient(op);

        Account account = client.account;
        JSONObject response;
        if (account==null){
            response = notLoggedInResponse;
            System.out.println(accountTable);
        }else{
            response = successResponse();
            response.put("notif",account.popNotifications());
        }
        client.send(response.toJSONString());
    }

    private void getGroupMembers(JSONObject op) throws IOException {
        Client client = prepareClient(op);
        JSONObject response;
        if (client.account==null){
            response=notLoggedInResponse;
        }else{

            String groupName = (String) op.get("groupName");

            Group group = groupTable.getGroup(groupName);
            if (group==null){
                response=createResponseJSON(0,"group does not exist");
            }else{
                List<String> memberNames = group.getMemberNames();
                response = successResponse();
                response.put("members",memberNames);
            }
        }
        client.send(response.toJSONString());
    }

    private void getAdministeredGroups(JSONObject op) throws IOException {
        Client client = prepareClient(op);

        JSONObject response;
        if (client.account==null){
            response = notLoggedInResponse;
        }else{
            response = successResponse();
            response.put("groups",client.account.administeredGroupNameList());
        }
        client.send(response.toJSONString());
    }

    private void getGroups(JSONObject op) throws IOException {
        Client client = prepareClient(op);

        JSONObject response;
        if (client.account==null){
            response = notLoggedInResponse;
        }else{
            response = successResponse();
            response.put("groups",client.account.groupNameList());
        }
        client.send(response.toJSONString());
    }

    private void addMember(JSONObject op) throws IOException {
        Client client = prepareClient(op);
        JSONObject response;
        if (client.account==null){
            response=notLoggedInResponse;
        }else{

            String groupName = (String) op.get("groupName");
            String accountName = (String) op.get("accountName");

            Group group = groupTable.getGroup(groupName);
            if (group==null){
                response=createResponseJSON(0,"group does not exist");
            }else{
                Account account = accountTable.getAccount(accountName);
                if (account==null){
                    response = noAccountResponse(accountName);
                }else if (group.isMember(account)){
                    response = createResponseJSON(0, "already a member");
                }else if (group.isAdmin(client.account)){
                    group.addMember(account);
                    response = successResponse();
                }else{
                    response = createResponseJSON(0, "not an admin");
                }
            }
        }
        client.send(response.toJSONString());
    }

    private void kickMember(JSONObject op) throws IOException {
        Client client = prepareClient(op);
        JSONObject response;
        if (client.account==null){
            response=notLoggedInResponse;
        }else{

            String groupName = (String) op.get("groupName");
            String memberName = (String) op.get("memberName");

            Group group = groupTable.getGroup(groupName);
            if (group==null){
                response=createResponseJSON(0,"group does not exist");
            }else{
                Account account = accountTable.getAccount(memberName);
                if (account==null){
                    response = noAccountResponse(memberName);
                }else if (!group.isMember(account)){
                    response = createResponseJSON(0, "not a member");
                }else if (group.isAdmin(client.account)){
                    if (group.isAdmin(account)){
                        response=createResponseJSON(0, "cannot kick admin");
                    }else{
                        group.kickMember(account);
                        response = successResponse();
                    }
                }else{
                    response = createResponseJSON(0, "not an admin");
                }
            }
        }
        client.send(response.toJSONString());
    }

    private void createGroup(JSONObject op) throws IOException {
        Client client = prepareClient(op);
        if (client.account==null){
            client.send(notLoggedInResponse.toJSONString());
        }else{
            String name = (String) op.get("name");
            if (groupTable.groupExists(name)){
                client.send(createResponseJSON(0,"group with name " + name + " exists").toJSONString());
            }else{
                List<String> membersName = (List<String>) op.get("members");
                List<Account> members = accountList(membersName);
                groupTable.addGroup(name,client.account,members);
                client.send(successResponse().toJSONString());
                System.out.println(groupTable);
            }
        }

    }
    List<Account> accountList(List<String> accountNameList){
        List<Account> accounts = new LinkedList<Account>();
        for (String name : accountNameList){
            Account account = accountTable.getAccount(name);
            if (account != null){
                accounts.add(account);
            }
        }
        return accounts;
    }

    JSONObject notLoggedInResponse = createResponseJSON(0,"not logged in");
    JSONObject notImplementedResponse = createResponseJSON(0,"not implemented");
    JSONObject successResponse(){return createResponseJSON(1, "");}
    JSONObject noAccountResponse(String accountName){
        return createResponseJSON(0,"no account named "+accountName);
    }

    private void sendMessage(JSONObject op) throws IOException {
        Client client = prepareClient(op);

        JSONObject response;
        if (client.account==null){
            response = notLoggedInResponse;
        }else{
            String type = (String) op.get("type");
            if (type.equals("private")){
                response = sendPrivateMessage(client.account, op);
            }else{
                response = sendGroupMessage(client.account, op);
            }
        }
        client.send(response.toJSONString());
    }

    private JSONObject sendPrivateMessage(Account sender, JSONObject op){
        String destinationName = (String) op.get("destinationName");
        Account destinationAccount = accountTable.getAccount(destinationName);
        if (destinationAccount==null){
            return noAccountResponse(destinationName);
        }else{
            destinationAccount.sendMessage(sender, (String) op.get("message"));
            return successResponse();
        }
    }

    private JSONObject sendGroupMessage(Account sender, JSONObject op){
        String groupName = (String) op.get("groupName");
        Group group = groupTable.getGroup(groupName);
        if (group==null){
            return createResponseJSON(0, "no group with the name");
        }else{
            group.sendMessage(sender, (String) op.get("message"));
            return successResponse();
        }

    }

    void login(JSONObject op) throws IOException {
        Client client = prepareClient(op);

        String loginName = (String) op.get("loginName");
        String password = (String) op.get("password");

        Account account = accountTable.getAccount(loginName);
        JSONObject response;
        if (account==null){
            response = createResponseJSON(0,"loginName incorrect");
            System.out.println(accountTable);
        }else if (account.passwordCorrect(password)){
            response = successResponse();
            client.associate(account);
            response.put("notif",account.popNotifications());
        }else{
            response = createResponseJSON(0,"password incorrect");
        }
        client.send(response.toJSONString());

    }

    void register(JSONObject op) throws IOException {
        Client client = prepareClient(op);

        String loginName = (String) op.get("loginName");
        String password = (String) op.get("password");
        JSONObject response;
        if (accountTable.accountExists(loginName)){
            response = createResponseJSON(0,"account " + loginName + " exists");
        }else{
            accountTable.addAccount(loginName, password);
            response = successResponse();
        }

        client.send(response.toJSONString());
    }

    void addFriend(JSONObject op) throws IOException {
        String friendName = (String) op.get("loginName");
        Client client = prepareClient(op);
        JSONObject response;

        if (client.account==null){
            response = notLoggedInResponse;
        }else{

            Account account = accountTable.getAccount(friendName);
            if (account==null){
                response = noAccountResponse(friendName);
            }else if (client.account.hasFriend(account)){
                response = createResponseJSON(0,friendName + "is already a friend");
            }else{
                client.account.addFriend(account);
                response = successResponse();
            }
        }
        client.send(response.toJSONString());
    }

    void getFriends(JSONObject op) throws IOException {
        Client client = prepareClient(op);

        JSONObject response;
        if (client.account==null){
            response = notLoggedInResponse;
        }else{
            response = successResponse();
            response.put("friends",client.account.friendNameList());
        }
        client.send(response.toJSONString());
    }

    void invalidOp(JSONObject op) throws IOException {
        JSONObject response = createResponseJSON(0,"operation invalid or not implemented");
        Client client = putNewClient(op);
        client.send(response.toJSONString());
    }

    Client prepareClient(JSONObject op) throws IOException {
        Client client = getClient((String) op.get("clientQueue"));
        if (client==null){
            client = putNewClient(op);
        }
        return client;
    }

    Client getClient(String queue){
        if (!clients.containsKey(queue))
            return null;
        else
            return clients.get(queue);
    }

    Client putNewClient(String queue, String notifQueue) throws IOException {
        Client client = new Client(channel,queue,null,notifQueue);
        clients.put(queue, client);
        return client;
    }

    Client putNewClient(String queue, Account account, String notifQueue) throws IOException {
        Client client = new Client(channel,queue,account, notifQueue);
        clients.put(queue, client);
        return client;
    }

    Client putNewClient(JSONObject op) throws IOException {
        String clientQueueName = (String) op.get("clientQueue");
        String notifQueue = (String) op.get("notifQueue");
        return putNewClient(clientQueueName,notifQueue);
    }

    JSONObject createResponseJSON(int status, String message){
        JSONObject retval = new JSONObject();
        retval.put("status", status);
        retval.put("message",message);
        return retval;
    }

    public JSONObject createOpJSON(String response){
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(response);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}