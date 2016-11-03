import sun.management.resources.agent_fr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nim_13512501 on 11/3/16.
 */
public class Account {
    String loginName;
    String password;

    private List<String> notifications;
    public List<Account> friends;
    private List<Group> groups;
    private List<Group> administeredGroups;

    public Account(String name, String password){
        this.loginName = name;
        this.password = password;
        notifications = new LinkedList<String> ();
        friends = new LinkedList<Account> ();
        groups = new LinkedList<Group> ();
        administeredGroups = new LinkedList<Group> ();
    }

    public void sendMessage(Account sender, String msg){
        String notification = "Message from " + sender.loginName + ":\n"+msg;
        notifications.add(notification);
    }

    public void sendMessage(Account sender, Group group, String msg){
        String notification = "Message from " + sender.loginName + " via "+ group.name + ":\n"+msg;
        notifications.add(notification);
    }

    public void notifyAddFriend(Account adder){
        String notification = adder.loginName + " added you as a friend";
        notifications.add(notification);
    }

    public void notify(String notification){
        notifications.add(notification);
    }

    public List<String> popNotifications(){
        List<String> ret = notifications;
        notifications = new LinkedList<String>();
        return ret;
    }

    public boolean passwordCorrect(String password){
        return this.password.equals(password);
    }

    @Override
    public String toString(){
        return "("+loginName+","+password+")";
    }

    public void addGroup(Group g){
        groups.add(g);
    }

    public void addAdministeredGroup(Group g){
        administeredGroups.add(g);
    }

    public void removeGroup(Group g){
        for(Group ig : groups){
            if (ig.name.equals(g.name)){
                groups.remove(ig);
            }
        }
        for (Group ig : administeredGroups){
            if (ig.name.equals(g.name)){
                administeredGroups.remove(ig);
            }
        }
    }

    public boolean hasFriend(Account newFriend){
        for (Account account : friends){
            if (account.loginName.equals(newFriend.loginName))
                return true;
        }
        return false;
    }
    public void addFriend(Account account){
        friends.add(account);
        account.notifyAddFriend(this);
    }
    public List<String> friendNameList(){
        List<String> retval = new ArrayList<String>(friends.size());
        for (Account account : friends){
            retval.add(account.loginName);
        }
        return retval;
    }

    public Object groupNameList() {
        List<String> retval = new ArrayList<String>(groups.size());
        for (Group group : groups){
            retval.add(group.name);
        }
        return retval;
    }

    public Object administeredGroupNameList() {
        List<String> retval = new ArrayList<String>(groups.size());
        for (Group group : administeredGroups){
            retval.add(group.name);
        }
        return retval;
    }
}
