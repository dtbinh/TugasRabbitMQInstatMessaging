import java.util.LinkedList;
import java.util.List;

/**
 * Created by nim_13512501 on 11/3/16.
 * non-persistent account table
 */
public class AccountTable {
    List<Account> accountList = new LinkedList<Account>();

    public AccountTable() {

    }

    public void addAccount(String name, String password){
        addAccount(new Account(name,password));
    }
    public void addAccount(Account account){
        accountList.add(account);
        System.out.println(accountList);
    }
    public boolean accountExists(String name){
        for (Account account : accountList){
            if (account.loginName.equals(name))
                return true;
        }
        return false;
    }
    public Account getAccount(String name){
        for (Account account : accountList){
            if (account.loginName.equals(name))
                return account;
        }
        return null;
    }

    @Override
    public String toString() {
        String str = "[";
        for (Account a : accountList){
            str += a;
            str += ",";
        }
        str+="]";
        return str;
    }
}
