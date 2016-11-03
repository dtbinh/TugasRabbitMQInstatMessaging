import java.util.LinkedList;
import java.util.List;

/**
 * Created by nim_13512501 on 11/3/16.
 */
public class GroupTable {
    List<Group> groupsList = new LinkedList<Group>();

    public GroupTable() {

    }

    public void addGroup(String name, Account admin, List<Account> members){
        addGroup(new Group(name, admin,members));
    }
    public void addGroup(Group group){
        groupsList.add(group);
        System.out.println(groupsList);
    }
    public boolean groupExists(String name){
        for (Group group : groupsList){
            if (group.name.equals(name))
                return true;
        }
        return false;
    }
    public Group getGroup(String name){
        for (Group group : groupsList){
            if (group.name.equals(name))
                return group;
        }
        return null;
    }

    @Override
    public String toString() {
        String str = "[";
        for (Group g : groupsList){
            str += g;
            str += ",";
        }
        str+="]";
        return str;
    }
}
