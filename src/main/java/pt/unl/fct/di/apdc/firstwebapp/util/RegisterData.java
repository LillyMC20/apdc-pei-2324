package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

    public String username;
    public String password;
    public String name;
    public String email;

    public RegisterData() {}

    public RegisterData(String username, String password, String name, String email) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
    }

    public boolean isRegValid() {
      return username != null && password != null;
    }

    public boolean isRegValid2() {
      return username != null && password != null && name !=null && email != null ;
    }
    
}
