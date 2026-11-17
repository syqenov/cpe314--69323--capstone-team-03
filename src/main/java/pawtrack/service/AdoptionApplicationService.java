package pawtrack.service;

import org.json.JSONObject;
import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.Database;
import pawtrack.model.Role;
import pawtrack.model.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Account-scoped applications, distinct from FIFO waitlist interest and completed adoption. */
public final class AdoptionApplicationService {
    public record Application(int id, int animalId, String animalName, int adopterId, String applicant,
                              String status, String submittedAt, String answers, String staffMessage) {}
    private final Database database;
    private final AuthController auth;
    public AdoptionApplicationService(Database database, AuthController auth) {
        this.database=Objects.requireNonNull(database); this.auth=Objects.requireNonNull(auth);
    }
    private Connection connection() { return database.connection(); }
    private int ownId() {
        User u=auth.getCurrentUser();
        if (u==null || u.getRole()!=Role.ADOPTER || u.getAdopterId()==null)
            throw new UnauthorizedException("Adopter account required");
        return u.getAdopterId();
    }
    public int submit(int animalId, Map<String,String> answers) throws SQLException {
        int adopterId=ownId();
        String name=required(answers,"full_name");
        String email=required(answers,"email");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Valid email required");
        for(String key:List.of("phone","address","residence","ownership","pets_allowed","pet_location",
            "secure_space","household","household_agreement","pets_before","pets_now","shelter_before",
            "experience","reason","caregiver","expenses","alone","vet_care","fallback")) required(answers,key);
        if(!"true".equals(answers.get("adult"))) throw new IllegalArgumentException("An eligible adult must apply");
        if(!"true".equals(answers.get("agreement"))) throw new IllegalArgumentException("Please acknowledge the application terms");
        int household;
        try {household=Integer.parseInt(answers.get("household"));} catch(NumberFormatException e) { throw new IllegalArgumentException("Enter a valid household size"); }
        if(household<1 || household>100)throw new IllegalArgumentException("Enter a valid household size");
        if(answers.toString().length()>20000) throw new IllegalArgumentException("Application answers are too long");
        try(PreparedStatement animal=connection().prepareStatement("SELECT status FROM animal WHERE id=?")){
            animal.setInt(1,animalId);
            try(ResultSet r=animal.executeQuery()){
                if(!r.next() || !Set.of("AVAILABLE","PENDING","RETURNED").contains(r.getString(1)))
                    throw new IllegalArgumentException("This animal is not accepting applications");
            }
        }
        if(hasActive(animalId))throw new IllegalArgumentException("You already have an active application for this pet");
        JSONObject json=new JSONObject(answers);
        try(PreparedStatement ps=connection().prepareStatement(
            "INSERT INTO adoption_application(animal_id,adopter_id,applicant_name,applicant_email,applicant_phone,applicant_address,answers,status,submitted_at,updated_at) VALUES(?,?,?,?,?,?,?,'PENDING_REVIEW',?,?)",
            Statement.RETURN_GENERATED_KEYS)){
            String now=LocalDateTime.now().toString();
            ps.setInt(1,animalId);ps.setInt(2,adopterId);ps.setString(3,name);ps.setString(4,email);
            ps.setString(5,answers.get("phone"));ps.setString(6,answers.get("address"));ps.setString(7,json.toString());
            ps.setString(8,now);ps.setString(9,now);ps.executeUpdate();
            try(ResultSet keys=ps.getGeneratedKeys()){if(keys.next())return keys.getInt(1);}
        }catch(SQLException e){if(e.getMessage()!=null && e.getMessage().contains("UNIQUE constraint"))
            throw new IllegalArgumentException("You already have an active application for this pet",e); throw e;}
        throw new SQLException("Failed to save application");
    }
    private static String required(Map<String,String> m,String key){
        String v=m.get(key); if(v==null || v.isBlank())throw new IllegalArgumentException("Please complete: "+key.replace('_',' '));
        return v.strip();
    }
    public boolean hasActive(int animalId)throws SQLException {
        int me=ownId();
        try(PreparedStatement ps=connection().prepareStatement("SELECT 1 FROM adoption_application WHERE adopter_id=? AND animal_id=? AND status IN ('PENDING_REVIEW','UNDER_REVIEW','MORE_INFO_NEEDED','APPROVED')")){
            ps.setInt(1,me);ps.setInt(2,animalId);try(ResultSet rs=ps.executeQuery()){return rs.next();}
        }
    }
    public List<Application> mine()throws SQLException {return query("WHERE ap.adopter_id=?",ownId());}
    public List<Application> staffList()throws SQLException {auth.requireStaff();return query("",null);}
    private List<Application> query(String filter,Integer id)throws SQLException{
        List<Application> list=new ArrayList<>();
        String sql="SELECT ap.id,ap.animal_id,a.name,ap.adopter_id,ap.applicant_name,ap.status,ap.submitted_at,ap.answers,ap.staff_message FROM adoption_application ap JOIN animal a ON a.id=ap.animal_id "+filter+" ORDER BY ap.submitted_at DESC";
        try(PreparedStatement ps=connection().prepareStatement(sql)){
            if(id!=null)ps.setInt(1,id);
            try(ResultSet rs=ps.executeQuery()){while(rs.next())list.add(new Application(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getInt(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9)));}
        }
        return list;
    }
    public void cancel(int appId)throws SQLException{
        int me=ownId();
        try(PreparedStatement ps=connection().prepareStatement("UPDATE adoption_application SET status='CANCELLED',updated_at=? WHERE id=? AND adopter_id=? AND status IN ('PENDING_REVIEW','UNDER_REVIEW','MORE_INFO_NEEDED')")){
            ps.setString(1,LocalDateTime.now().toString());ps.setInt(2,appId);ps.setInt(3,me);
            if(ps.executeUpdate()!=1)throw new IllegalStateException("This application cannot be cancelled");
        }
    }
    public void decide(int appId,String newStatus,String message)throws SQLException{
        auth.requireStaff();
        if(!Set.of("UNDER_REVIEW","MORE_INFO_NEEDED","APPROVED","REJECTED").contains(newStatus))throw new IllegalArgumentException("Invalid status");
        if(message!=null && message.length()>1000)throw new IllegalArgumentException("Note is too long");
        String current=null;
        try(PreparedStatement ps=connection().prepareStatement("SELECT status FROM adoption_application WHERE id=?")){
            ps.setInt(1,appId);try(ResultSet rs=ps.executeQuery()){if(rs.next())current=rs.getString(1);}
        }
        if(current==null || !Set.of("PENDING_REVIEW","UNDER_REVIEW","MORE_INFO_NEEDED").contains(current))
            throw new IllegalStateException("This application is no longer open for review");
        if(newStatus.equals("APPROVED") && current.equals("MORE_INFO_NEEDED"))throw new IllegalStateException("Review the requested information before approval");
        String now=LocalDateTime.now().toString();
        boolean ownTx=connection().getAutoCommit();
        if(ownTx)connection().setAutoCommit(false);
        try {
            try(PreparedStatement ps=connection().prepareStatement("UPDATE adoption_application SET status=?,staff_message=?,reviewed_by=?,reviewed_at=?,updated_at=? WHERE id=?")){
                ps.setString(1,newStatus);ps.setString(2,message==null?"":message.strip());ps.setInt(3,auth.getCurrentUser().getId());ps.setString(4,now);ps.setString(5,now);ps.setInt(6,appId);ps.executeUpdate();
            }
            try(PreparedStatement ps=connection().prepareStatement("INSERT INTO adoption_application_history(application_id,status,actor_id,note,occurred_at) VALUES(?,?,?,?,?)")){
                ps.setInt(1,appId);ps.setString(2,newStatus);ps.setInt(3,auth.getCurrentUser().getId());ps.setString(4,message==null?"":message.strip());ps.setString(5,now);ps.executeUpdate();
            }
            if(ownTx)connection().commit();
        }catch(SQLException|RuntimeException e){if(ownTx)connection().rollback();throw e;}
        finally{if(ownTx)connection().setAutoCommit(true);}
    }
}
