package pawtrack.ui;

import org.json.JSONObject;
import pawtrack.service.AdoptionApplicationService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Staff review or adopter history; access is checked in the service on every operation. */
public final class AdoptionApplicationsPanel extends JPanel {
    private final AdoptionApplicationService service;
    private final boolean staff;
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"ID","Pet","Applicant","Submitted","Status"},0){
        @Override public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable table=new JTable(model);
    private List<AdoptionApplicationService.Application> rows=List.of();
    public AdoptionApplicationsPanel(AdoptionApplicationService service,boolean staff){
        super(new BorderLayout(0,15));this.service=service;this.staff=staff;
        setBackground(PremiumUi.CANVAS);setBorder(new EmptyBorder(20,22,20,22));
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);
        header.add(PremiumUi.title(staff?"Adoption Requests":"My Applications",26),BorderLayout.NORTH);
        header.add(PremiumUi.caption(staff?"Review submitted adoption forms":"Track your submitted adoption applications"),BorderLayout.SOUTH);
        add(header,BorderLayout.NORTH);
        PremiumUi.styleTable(table);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.setOpaque(false);
        buttons.add(PremiumUi.button("Refresh",this::refresh,false));
        buttons.add(PremiumUi.button(staff?"Review Application":"View Details",this::openSelected,true));
        if(!staff)buttons.add(PremiumUi.button("Cancel Application",this::cancel,false));
        add(buttons,BorderLayout.SOUTH);refresh();
    }
    public void refresh(){
        try{rows=staff?service.staffList():service.mine();model.setRowCount(0);
            for(var app:rows)model.addRow(new Object[]{app.id(),app.animalName(),app.applicant(),app.submittedAt().replace('T',' '),app.status().replace('_',' ')});
        }catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Application Error",JOptionPane.ERROR_MESSAGE);}
    }
    private AdoptionApplicationService.Application selected(){int r=table.getSelectedRow();
        if(r<0){JOptionPane.showMessageDialog(this,"Select an application first");return null;}
        return rows.get(table.convertRowIndexToModel(r));
    }
    private void openSelected(){var app=selected();if(app==null)return;
        JSONObject data=new JSONObject(app.answers());StringBuilder s=new StringBuilder();
        for(String key:data.keySet())if(!key.equals("agreement"))s.append(key.replace('_',' ')).append(": ").append(data.optString(key)).append("\n\n");
        s.append("Status: ").append(app.status()).append("\nMessage: ").append(app.staffMessage());
        JTextArea details=new JTextArea(s.toString(),20,50);details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);
        JScrollPane scroll=new JScrollPane(details);
        if(!staff){JOptionPane.showMessageDialog(this,scroll,"Application #"+app.id(),JOptionPane.PLAIN_MESSAGE);return;}
        String[] statuses={"UNDER_REVIEW","MORE_INFO_NEEDED","APPROVED","REJECTED"};
        JComboBox<String> status=new JComboBox<>(statuses);status.setSelectedItem(app.status());
        JTextArea note=new JTextArea(3,30);note.setText(app.staffMessage());
        JPanel area=new JPanel(new BorderLayout(5,8));area.add(scroll,BorderLayout.CENTER);
        JPanel decision=new JPanel(new BorderLayout(4,4));decision.add(status,BorderLayout.NORTH);
        decision.add(new JScrollPane(note),BorderLayout.CENTER);area.add(decision,BorderLayout.SOUTH);
        int result=JOptionPane.showConfirmDialog(this,area,"Review Application #"+app.id(),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result==JOptionPane.OK_OPTION){
            try{if(JOptionPane.showConfirmDialog(this,"Set status to "+status.getSelectedItem()+"?","Confirm Review",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                service.decide(app.id(),Objects.toString(status.getSelectedItem()),note.getText());refresh();}
            catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Review Error",JOptionPane.ERROR_MESSAGE);}
        }
    }
    private void cancel(){var app=selected();if(app==null)return;
        if(JOptionPane.showConfirmDialog(this,"Cancel application #"+app.id()+"?","Confirm Cancellation",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{service.cancel(app.id());refresh();}catch(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"Cancellation Error",JOptionPane.ERROR_MESSAGE);}
    }
}
