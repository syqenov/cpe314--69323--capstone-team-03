package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Staff CSV import workspace. Keeps the established SwingWorker import action. */
public final class PawTrackImportPanel extends JPanel {
    private final JProgressBar displayProgress=new JProgressBar(0,100);
    private final JLabel state=PremiumUi.title("Ready to import",19);
    private final JLabel detail=PremiumUi.caption("Select an intake CSV to begin a background import.");
    public PawTrackImportPanel(Runnable openChooser,JProgressBar applicationProgress){
        super(new BorderLayout(0,16));
        setBackground(PremiumUi.CANVAS);setBorder(new EmptyBorder(20,24,22,24));
        JPanel header=PremiumUi.clear(new GridLayout(2,1,0,3));
        header.add(PremiumUi.title("Import Records",27));
        header.add(PremiumUi.caption("Import historical intake and adoption data without freezing the interface."));
        add(header,BorderLayout.NORTH);
        JPanel content=PremiumUi.clear(new GridLayout(2,1,0,16));
        JPanel chooser=PremiumUi.card(new BorderLayout(12,12));
        JPanel title=PremiumUi.clear(new GridLayout(2,1,0,4));
        title.add(PremiumUi.title("Historical Intake CSV",21));
        title.add(PremiumUi.caption("Choose a UTF-8 CSV file containing animal intake and optional adoption data."));
        chooser.add(title,BorderLayout.NORTH);
        JPanel middle=PremiumUi.clear(new GridBagLayout());
        JPanel ctr=PremiumUi.clear(new GridLayout(3,1,0,12));
        JLabel symbol=PremiumUi.title("CSV  ·  ANIMAL INTAKE",30);symbol.setForeground(PremiumUi.BLUE);symbol.setHorizontalAlignment(SwingConstants.CENTER);
        ctr.add(symbol);
        JLabel note=PremiumUi.caption("Files are imported by a background worker. Existing permissions remain enforced.");
        note.setHorizontalAlignment(SwingConstants.CENTER);ctr.add(note);
        ctr.add(PremiumUi.button("Choose CSV File and Start Import",openChooser,true));
        middle.add(ctr);chooser.add(middle,BorderLayout.CENTER);
        content.add(chooser);
        JPanel bottom=PremiumUi.clear(new GridLayout(1,2,16,0));
        JPanel progress=PremiumUi.card(new BorderLayout(0,14));
        progress.add(PremiumUi.title("Import progress",19),BorderLayout.NORTH);
        JPanel values=PremiumUi.clear(new GridLayout(3,1,0,8));
        values.add(state);values.add(detail);
        displayProgress.setStringPainted(true);displayProgress.setForeground(PremiumUi.BLUE);
        displayProgress.setString("Ready");values.add(displayProgress);
        progress.add(values,BorderLayout.CENTER);
        bottom.add(progress);
        JPanel schema=PremiumUi.card(new BorderLayout(0,12));
        schema.add(PremiumUi.title("Supported CSV Format",19),BorderLayout.NORTH);
        JTextArea info=new JTextArea("Required headers:\nname, species, breed, age_months, intake_date, status\n\nOptional headers:\nadoption_date, adopter_name, adopter_email\n\nDates use YYYY-MM-DD. Animal species: Dog or Cat.");
        info.setEditable(false);info.setBackground(Color.WHITE);
        info.setForeground(PremiumUi.NAVY);info.setFont(PremiumUi.font(12,false));
        schema.add(info,BorderLayout.CENTER);bottom.add(schema);
        content.add(bottom);add(content,BorderLayout.CENTER);
        applicationProgress.addChangeListener(event->{
            int value=applicationProgress.getValue();displayProgress.setValue(value);
            String label=applicationProgress.getString();
            displayProgress.setString(label==null||label.isBlank()?value+"%":label);
            if("Done".equalsIgnoreCase(label)) {state.setText("Import complete");detail.setText("Refresh Animals or Analytics to inspect imported records.");}
            else if("Failed".equalsIgnoreCase(label)) {state.setText("Import failed");detail.setText("Please review the import error and CSV headers.");}
            else if(value>0){state.setText("Importing records...");detail.setText("Processing on a background thread.");}
        });
    }
}
