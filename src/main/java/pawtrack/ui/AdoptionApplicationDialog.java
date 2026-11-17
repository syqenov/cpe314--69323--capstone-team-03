package pawtrack.ui;

import pawtrack.model.Animal;
import pawtrack.model.Adopter;
import pawtrack.service.AdoptionApplicationService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;

/** Five-step form: collected values stay in the form until successfully submitted. */
public final class AdoptionApplicationDialog extends JDialog {
    private final AdoptionApplicationService applications;
    private final Animal animal;
    private final Map<String,JComponent> inputs=new LinkedHashMap<>();
    private final JPanel pages=new JPanel(new CardLayout());
    private final JLabel stepLabel=PremiumUi.caption("Step 1 of 5");
    private final JButton back=new JButton("Back");
    private final JButton next=new JButton("Continue");
    private final JTextArea review=new JTextArea();
    private int step;
    private final String[] headings={"Personal Information","Home & Living Environment","Pet Experience","Adoption Readiness","Review & Submit"};
    private AdoptionApplicationDialog(Window owner,Animal animal,Adopter adopter,AdoptionApplicationService applications){
        super(owner,"Adoption Application",ModalityType.APPLICATION_MODAL);
        this.animal=animal;this.applications=applications;
        setSize(760,700);setMinimumSize(new Dimension(590,520));setLocationRelativeTo(owner);
        JPanel root=new JPanel(new BorderLayout(0,15));root.setBackground(PremiumUi.CANVAS);
        root.setBorder(new EmptyBorder(20,22,16,22));setContentPane(root);
        JPanel header=new JPanel(new GridLayout(3,1,0,4));header.setOpaque(false);
        header.add(PremiumUi.title("Apply to adopt " + animal.getName(),25));
        header.add(PremiumUi.caption(animal.species()+"  •  "+animal.getBreed()+"  •  "+animal.getAgeMonths()+" months  •  Fee: "+animal.adoptionFee()));
        header.add(stepLabel);root.add(header,BorderLayout.NORTH);
        JPanel p1=page();
        field(p1,"full_name","Full Name",adopter.getName(),false);
        field(p1,"email","Email Address",adopter.getEmail().endsWith(".invalid")?"":adopter.getEmail(),false);
        field(p1,"phone","Contact Number",adopter.getPhone(),false);
        field(p1,"address","Complete Home Address","",true);
        field(p1,"occupation","Occupation (optional)","",false);
        choice(p1,"contact","Preferred Contact", "Email","Phone");
        check(p1,"adult","I confirm I am 18 or older and eligible to apply");
        pages.add(scroll(p1),"0");
        JPanel p2=page();
        choice(p2,"residence","Residence Type","House","Apartment","Condominium","Other");
        choice(p2,"ownership","Housing Arrangement","Own","Rent");
        choice(p2,"pets_allowed","Are pets allowed?","Yes","No","Not sure");
        choice(p2,"landlord","Landlord approval / housing rules","Not applicable","Yes","No","Not sure");
        choice(p2,"pet_location","Where will the pet stay?","Indoors","Outdoors","Both");
        choice(p2,"secure_space","Secure space for the pet?","Yes","No","Needs preparation");
        field(p2,"household","People in household","1",false);
        choice(p2,"household_agreement","Does everyone agree?","Yes","No","Not yet discussed");
        pages.add(scroll(p2),"1");
        JPanel p3=page();
        choice(p3,"pets_before","Owned pets before?","No","Yes");
        choice(p3,"pets_now","Currently have pets?","No","Yes");
        field(p3,"current_pet_details","Current pets: number, species, vaccinations (if applicable)","None",true);
        choice(p3,"shelter_before","Adopted from a shelter before?","No","Yes");
        choice(p3,"experience","Experience with dogs or cats","None","Dogs","Cats","Both");
        pages.add(scroll(p3),"2");
        JPanel p4=page();
        field(p4,"reason","Why do you want to adopt this pet?","",true);
        field(p4,"caregiver","Who will be the primary caregiver?","",false);
        choice(p4,"expenses","Prepared for regular expenses?","Yes","No","Unsure");
        choice(p4,"alone","Typical time the pet is alone","Less than 2 hours","2–4 hours","4–6 hours","More than 6 hours","Varies");
        choice(p4,"vet_care","Willing to provide veterinary care?","Yes","No","Need more information");
        field(p4,"fallback","What if you can no longer care for the pet?","",true);
        pages.add(scroll(p4),"3");
        JPanel p5=page();review.setEditable(false);review.setLineWrap(true);review.setWrapStyleWord(true);
        review.setFont(PremiumUi.font(13,false));review.setBackground(Color.WHITE);p5.add(review);
        check(p5,"agreement","I confirm the answers are accurate, accept shelter policies, and acknowledge the privacy notice. Submission does not guarantee approval.");
        pages.add(scroll(p5),"4");root.add(pages,BorderLayout.CENTER);
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,2));footer.setOpaque(false);
        JButton cancel=PremiumUi.button("Cancel",this::dispose,false);
        back.addActionListener(e->showStep(step-1));next.addActionListener(e->advance());
        footer.add(cancel);footer.add(back);footer.add(next);root.add(footer,BorderLayout.SOUTH);
        showStep(0);
    }
    public static void show(Window owner,Animal animal,Adopter adopter,AdoptionApplicationService applications){
        new AdoptionApplicationDialog(owner,animal,adopter,applications).setVisible(true);
    }
    private JPanel page(){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(12,12,12,12));return p;}
    private JScrollPane scroll(JPanel p){JScrollPane s=new JScrollPane(p);s.setBorder(BorderFactory.createLineBorder(PremiumUi.LINE));s.getVerticalScrollBar().setUnitIncrement(18);return s;}
    private void field(JPanel p,String key,String label,String initial,boolean multi){
        JLabel l=PremiumUi.title(label,13);l.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(l);
        JComponent c;
        if(multi){JTextArea a=new JTextArea(initial==null?"":initial,3,30);a.setLineWrap(true);a.setWrapStyleWord(true);c=a;
            JScrollPane s=new JScrollPane(a);s.setAlignmentX(Component.LEFT_ALIGNMENT);s.setMaximumSize(new Dimension(2000,76));p.add(s);
        }else{JTextField f=new JTextField(initial==null?"":initial);c=f;f.setMaximumSize(new Dimension(2000,36));f.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(f);}
        inputs.put(key,c);p.add(Box.createVerticalStrut(12));
    }
    private void choice(JPanel p,String key,String label,String... options){
        JLabel l=PremiumUi.title(label,13);l.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(l);
        JComboBox<String> c=new JComboBox<>(options);c.setMaximumSize(new Dimension(2000,35));c.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(c);inputs.put(key,c);p.add(Box.createVerticalStrut(12));
    }
    private void check(JPanel p,String key,String caption){JCheckBox c=new JCheckBox(caption);c.setOpaque(false);c.setAlignmentX(Component.LEFT_ALIGNMENT);inputs.put(key,c);p.add(c);p.add(Box.createVerticalStrut(12));}
    private Map<String,String> collect(){Map<String,String> map=new LinkedHashMap<>();
        for(var item:inputs.entrySet()){
            JComponent c=item.getValue();String v=c instanceof JTextField f?f.getText():c instanceof JTextArea a?a.getText():c instanceof JComboBox<?> b?Objects.toString(b.getSelectedItem(),""):c instanceof JCheckBox b?Boolean.toString(b.isSelected()):"";
            map.put(item.getKey(),v==null?"":v.strip());
        }return map;
    }
    private void showStep(int n){step=n;((CardLayout)pages.getLayout()).show(pages,Integer.toString(n));
        stepLabel.setText("Step "+(n+1)+" of 5  ·  "+headings[n]);back.setEnabled(n>0);next.setText(n==4?"Submit Application":"Continue");
        if(n==4){StringBuilder s=new StringBuilder();collect().forEach((k,v)->{if(!k.equals("agreement"))s.append(k.replace('_',' ').toUpperCase()).append("\n").append(v).append("\n\n");});review.setText(s.toString());review.setCaretPosition(0);}
    }
    private void advance(){if(step<4){showStep(step+1);return;}
        next.setEnabled(false);Map<String,String> answers=collect();
        new SwingWorker<Integer,Void>(){
            @Override protected Integer doInBackground()throws Exception{return applications.submit(animal.getId(),answers);}
            @Override protected void done(){next.setEnabled(true);try{int id=get();JOptionPane.showMessageDialog(AdoptionApplicationDialog.this,"Application #"+id+" submitted successfully! See My Applications for its status.");dispose();}
                catch(Exception e){Throwable root=e.getCause()==null?e:e.getCause();JOptionPane.showMessageDialog(AdoptionApplicationDialog.this,root.getMessage(),"Check Application",JOptionPane.WARNING_MESSAGE);}}
        }.execute();
    }
}
