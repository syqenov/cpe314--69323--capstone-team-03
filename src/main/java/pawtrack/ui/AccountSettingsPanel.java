package pawtrack.ui;

import pawtrack.controller.AuthController;
import pawtrack.model.Adopter;
import pawtrack.service.AccountSettingsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

/** Account profile and password controls using PawTrack's existing authorization. */
public final class AccountSettingsPanel extends JPanel {
    private final AuthController auth;
    private final AccountSettingsService accounts;
    private final JTextField name = new JTextField(25);
    private final JTextField email = new JTextField(25);
    private final JTextField phone = new JTextField(25);
    private final JPasswordField oldPassword = new JPasswordField(25);
    private final JPasswordField newPassword = new JPasswordField(25);
    private final JPasswordField confirmation = new JPasswordField(25);

    public AccountSettingsPanel(AuthController auth, AccountSettingsService accounts) {
        super(new BorderLayout(0,17));
        this.auth=Objects.requireNonNull(auth);this.accounts=Objects.requireNonNull(accounts);
        setBackground(PremiumUi.CANVAS);
        setBorder(new EmptyBorder(20,23,22,23));
        JPanel title=PremiumUi.clear(new GridLayout(2,1,0,3));
        title.add(PremiumUi.title("Account Settings",27));
        title.add(PremiumUi.caption("Manage your profile, contact information and account security."));
        add(title,BorderLayout.NORTH);
        JPanel rows=PremiumUi.clear(new GridLayout(1,auth.isAdopter()?2:1,16,0));
        if(auth.isAdopter()){
            JPanel profile=section("My Contact Details","Information linked to your adoption applications.");
            JPanel inputRows=PremiumUi.clear(new GridLayout(3,2,12,11));
            field(inputRows,"Full Name",name);field(inputRows,"Email Address",email);field(inputRows,"Phone (optional)",phone);
            profile.add(inputRows,BorderLayout.CENTER);
            profile.add(PremiumUi.button("Save Profile",this::saveProfile,true),BorderLayout.SOUTH);
            rows.add(profile);loadProfile();
        }
        JPanel security=section("Account Security","Update your password to protect your account.");
        JPanel inputs=PremiumUi.clear(new GridLayout(3,2,12,11));
        field(inputs,"Current Password",oldPassword);
        field(inputs,"New Password",newPassword);
        field(inputs,"Confirm Password",confirmation);
        security.add(inputs,BorderLayout.CENTER);
        security.add(PremiumUi.button("Update Password",this::changePassword,true),BorderLayout.SOUTH);
        rows.add(security);
        JPanel information=section("Account Information","Your PawTrack access and current workspace.");
        JPanel facts=PremiumUi.clear(new GridLayout(4,1,0,12));
        String username=auth.getCurrentUser()==null?"Not signed in":auth.getCurrentUser().getUsername();
        String role=auth.getCurrentUser()==null?"Guest":auth.getCurrentUser().getRole().name();
        facts.add(PremiumUi.title("User:  "+username,15));
        facts.add(PremiumUi.caption("Role:  "+role));
        facts.add(PremiumUi.caption("Database: Local SQLite storage"));
        facts.add(PremiumUi.caption("Keep your account details accurate for adoption updates."));
        information.add(facts,BorderLayout.CENTER);
        JPanel sections=PremiumUi.clear(new BorderLayout(0,16));
        sections.add(rows,BorderLayout.NORTH);
        sections.add(information,BorderLayout.CENTER);
        JScrollPane scroll=new JScrollPane(sections);
        scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll,BorderLayout.CENTER);
    }
    private JPanel section(String heading,String description){
        JPanel card=PremiumUi.card(new BorderLayout(0,16));
        JPanel top=PremiumUi.clear(new GridLayout(2,1,0,3));
        top.add(PremiumUi.title(heading,20));top.add(PremiumUi.caption(description));
        card.add(top,BorderLayout.NORTH);
        return card;
    }
    private static void field(JPanel panel,String caption,JComponent input){
        JLabel label=PremiumUi.title(caption,13);
        input.setFont(PremiumUi.font(13,false));
        input.setForeground(PremiumUi.NAVY);
        input.setPreferredSize(new Dimension(240,36));
        panel.add(label);panel.add(input);
    }
    private void loadProfile(){
        try{
            Adopter a=accounts.myProfile();
            name.setText(a.getName());email.setText(a.getEmail());phone.setText(a.getPhone());
        }catch(Exception ex){message("Could not load profile: "+ex.getMessage(),true);}
    }
    private void saveProfile(){
        try{
            accounts.updateMyContact(name.getText(),email.getText(),phone.getText());
            message("Profile saved.",false);
        }catch(Exception ex){message("Could not save profile: "+ex.getMessage(),true);}
    }
    private void changePassword(){
        char[] old=oldPassword.getPassword(), next=newPassword.getPassword(), confirm=confirmation.getPassword();
        try{
            if(!java.util.Arrays.equals(next,confirm))throw new IllegalArgumentException("New passwords do not match");
            auth.changeMyPassword(new String(old),new String(next));
            oldPassword.setText("");newPassword.setText("");confirmation.setText("");
            message("Password updated successfully.",false);
        }catch(Exception ex){message("Could not update password: "+ex.getMessage(),true);}
        finally{
            java.util.Arrays.fill(old,'\0');java.util.Arrays.fill(next,'\0');java.util.Arrays.fill(confirm,'\0');
        }
    }
    private void message(String text,boolean error){
        JOptionPane.showMessageDialog(this,text,error?"Settings error":"Account settings",
            error?JOptionPane.ERROR_MESSAGE:JOptionPane.INFORMATION_MESSAGE);
    }
}
