package pawtrack.service;

import org.junit.jupiter.api.Test;
import pawtrack.controller.AuthController;
import pawtrack.controller.UnauthorizedException;
import pawtrack.dao.Database;
import pawtrack.dao.UserDao;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdoptionApplicationFeatureTest {
    private Map<String,String> answers(){
        Map<String,String> m=new HashMap<>();
        for(String key:List.of("full_name","email","phone","address","residence","ownership","pets_allowed","pet_location","secure_space","household","household_agreement","pets_before","pets_now","shelter_before","experience","reason","caregiver","expenses","alone","vet_care","fallback"))m.put(key,"Yes");
        m.put("email","applicant@example.com");m.put("household","2");m.put("adult","true");m.put("agreement","true");return m;
    }
    @Test void submissionReviewAndCancellation()throws Exception{
        try(Database db=Database.openInMemory()){
            AuthController auth=new AuthController(new UserDao(db));
            assertTrue(auth.login("user","user123").isPresent());
            try(var ps=db.connection().prepareStatement("INSERT INTO animal(name,species,breed,age_months,intake_date,status) VALUES('Milo','CAT','Mixed',14,'2026-01-01','AVAILABLE')")){ps.executeUpdate();}
            AdoptionApplicationService app=new AdoptionApplicationService(db,auth);
            int id=app.submit(1,answers());
            assertTrue(app.hasActive(1));
            assertThrows(IllegalArgumentException.class,()->app.submit(1,answers()));
            assertThrows(UnauthorizedException.class,()->app.decide(id,"APPROVED",""));
            auth.logout();assertTrue(auth.login("admin","admin123").isPresent());
            app.decide(id,"UNDER_REVIEW","Needs review");
            assertEquals("UNDER_REVIEW",app.staffList().get(0).status());
            auth.logout();assertTrue(auth.login("user","user123").isPresent());
            app.cancel(id);assertEquals("CANCELLED",app.mine().get(0).status());
            assertFalse(app.hasActive(1));
        }
    }
    @Test void validationDoesNotSaveInvalidApplication()throws Exception{
        try(Database db=Database.openInMemory()){
            AuthController auth=new AuthController(new UserDao(db));auth.login("user","user123");
            AdoptionApplicationService app=new AdoptionApplicationService(db,auth);
            Map<String,String> invalid=answers();invalid.put("email","invalid");
            assertThrows(IllegalArgumentException.class,()->app.submit(1,invalid));
            assertTrue(app.mine().isEmpty());
        }
    }
    @Test void persistedAccountPreferencesAreScoped()throws Exception{
        try(Database db=Database.openInMemory()){
            AuthController auth=new AuthController(new UserDao(db));auth.login("user","user123");
            AccountPreferencesService prefs=new AccountPreferencesService(db,auth);
            prefs.put("theme","Dark");assertEquals("Dark",prefs.get("theme","Light"));
            prefs.saveProfile("Test Person","test.person@example.com","09123456789","Cebu","Email");
            assertEquals("Cebu",prefs.profile().get("address"));
            auth.logout();auth.login("admin","admin123");
            assertEquals("Light",prefs.get("theme","Light"));
        }
    }
}
