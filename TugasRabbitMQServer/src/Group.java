import java.util.ArrayList;
import java.util.List;

/**
 * Created by nim_13512501 on 11/3/16.
 */
public class Group {
    private List<Account> members;
    private Account admin;
    String name;
    public Group(String name, Account admin, List<Account> members){
        this.admin = admin;
        this.name = name;
        this.members = members;
        for (Account member : members){
            member.addGroup(this);
        }
        if (!isMember(admin))
            addMember(admin);
        admin.addAdministeredGroup(this);
    }
    public boolean isAdmin(Account admin){
        return this.admin.loginName.equals(admin.loginName);
    }
    public boolean isMember(Account account){
        for (Account member : members){
            if (member.loginName.equals(account.loginName)){
                return true;
            }
        }
        return false;
    }
    public void addMember(Account member){
        members.add(member);
        member.addGroup(this);
    }
    public void kickMember(Account member){
        members.remove(member);
        member.removeGroup(this);
    }
    public List<String> getMemberNames(){
        List<String> memberNames = new ArrayList<String>(members.size());
        for (Account member : members){
            memberNames.add(member.loginName);
        }
        return memberNames;
    }
    public void sendMessage(Account sender, String message){
        for (Account member : members){
            member.sendMessage(sender, this, message);
        }
    }

    @Override
    public String toString(){
        String str = "{"+name+","+admin.loginName+",[";
        for (Account member : members){
            str+=member.loginName+",";
        }
        return str+"]}";
    }
}
