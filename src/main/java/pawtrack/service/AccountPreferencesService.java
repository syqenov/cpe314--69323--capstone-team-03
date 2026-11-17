package pawtrack.service;

import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.Database;
import java.sql.*;
import java.util.*;

/** Persisted, authenticated preferences and optional staff profile details. */
public final class AccountPreferencesService {
    private final Database db;
    private final AuthController auth;
    public AccountPreferencesService(Database db,AuthController auth){this.db=db;this.auth=auth;}
    private int userId(){if(auth.getCurrentUser()==null)throw new UnauthorizedException("Login required");return auth.getCurrentUser().getId();}
    public String get(String key,String fallback)throws SQLException{
        synchronized (db) {
        try(PreparedStatement ps=db.connection().prepareStatement("SELECT pref_value FROM account_preferences WHERE user_id=? AND pref_key=?")){
            ps.setInt(1,userId());ps.setString(2,key);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getString(1):fallback;}
        }
    
        }
    }
    public void put(String key,String value)throws SQLException{
        synchronized (db) {
        if(!Set.of("theme","notifications","application_updates","waitlist_updates","request_alerts").contains(key))throw new IllegalArgumentException("Unsupported preference");
        try(PreparedStatement ps=db.connection().prepareStatement("INSERT INTO account_preferences(user_id,pref_key,pref_value) VALUES(?,?,?) ON CONFLICT(user_id,pref_key) DO UPDATE SET pref_value=excluded.pref_value")){
            ps.setInt(1,userId());ps.setString(2,key);ps.setString(3,value);ps.executeUpdate();
        }
    
        }
    }
    public Map<String,String> profile()throws SQLException{
        synchronized (db) {
        Map<String,String> result=new HashMap<>();
        try(PreparedStatement ps=db.connection().prepareStatement("SELECT full_name,email,phone,address,preferred_contact FROM account_profile WHERE user_id=?")){
            ps.setInt(1,userId());try(ResultSet rs=ps.executeQuery()){if(rs.next()){
                String[] names={"full_name","email","phone","address","preferred_contact"};
                for(int i=0;i<names.length;i++)result.put(names[i],Objects.toString(rs.getString(i+1),""));
            }}
        }return result;
    
        }
    }
    public void saveProfile(String name,String email,String phone,String address,String contact)throws SQLException{
        synchronized (db) {
        if(name==null || name.isBlank() || name.length()>100)throw new IllegalArgumentException("Enter a valid name");
        if(email==null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException("Enter a valid email");
        if(phone!=null && phone.length()>40)throw new IllegalArgumentException("Phone is too long");
        if(address!=null && address.length()>500)throw new IllegalArgumentException("Address is too long");
        try(PreparedStatement ps=db.connection().prepareStatement("INSERT INTO account_profile(user_id,full_name,email,phone,address,preferred_contact) VALUES(?,?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET full_name=excluded.full_name,email=excluded.email,phone=excluded.phone,address=excluded.address,preferred_contact=excluded.preferred_contact")){
            ps.setInt(1,userId());ps.setString(2,name.strip());ps.setString(3,email.strip());ps.setString(4,phone==null?"":phone.strip());ps.setString(5,address==null?"":address.strip());ps.setString(6,contact);ps.executeUpdate();
        }
    
        }
    }
}
