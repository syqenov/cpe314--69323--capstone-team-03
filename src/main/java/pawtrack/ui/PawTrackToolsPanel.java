package pawtrack.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Modern tools hub; all actions delegate to the original, permission-checked buttons. */
public final class PawTrackToolsPanel extends JPanel {
    private final JScrollPane legacyActions;
    public PawTrackToolsPanel(JScrollPane actions) {
        this(actions, null);
    }
    public PawTrackToolsPanel(JScrollPane actions, Runnable openAccountSettings) {
        super(new BorderLayout());
        if(actions==null) throw new IllegalArgumentException("Actions are required");
        this.legacyActions=actions;
        setBackground(PremiumUi.CANVAS);
        JPanel content=new PremiumUi.Background(new BorderLayout(0,17));
        content.setBorder(new EmptyBorder(20,24,22,24));
        JPanel header=PremiumUi.clear(new GridLayout(2,1,0,4));
        header.add(PremiumUi.title("Tools & Settings",27));
        header.add(PremiumUi.caption("Configure your workspace, import or export records, and manage application tools."));
        content.add(header,BorderLayout.NORTH);
        JPanel cards=PremiumUi.clear(new GridLayout(3,2,16,16));
        cards.add(toolCard("Quick Tools","Everyday animal management actions.",
                shortcut("Refresh Animals", "List / Refresh Animals"),
                shortcut("Fetch Sample Pet Image", "Fetch Sample Pet Image"),
                shortcut("Record Interest", "Record Interest"),
                shortcut("Add Animal", "Add Animal")));
        cards.add(toolCard("Import & Export","CSV file utilities and background intake.",
                shortcut("Import Intake History (CSV)", "Import Intake History (CSV)"),
                shortcut("Export Animals CSV", "Export CSV")));
        cards.add(toolCard("Appearance & Columns","Use your existing view preferences.",
                shortcut("Toggle Light / Dark", "Dark", "Light"),
                shortcut("Column Visibility", "Columns"),
                shortcut("Filter by Classification", "Filter by Classification")));
        cards.add(toolCard("Animal Management","Available actions depend on your account role.",
                shortcut("Edit Selected Animal", "Edit Animal"),
                shortcut("Delete Selected Animal", "Delete Animal"),
                shortcut("Mark Adopted", "Mark Adopted"),
                shortcut("Undo Last Status Change", "Undo Last Status Change")));
        cards.add(toolCard("Keyboard Shortcuts","Useful keyboard commands in PawTrack.",
                new JLabel("Ctrl+R  Refresh animals"),
                new JLabel("Ctrl+F  Search or filter"),
                new JLabel("Ctrl+E  Export CSV"),
                new JLabel("Ctrl+T  Toggle appearance")));
        JButton advanced=PremiumUi.button("Open Advanced Controls",this::showAdvancedControls,false);
        JButton account = PremiumUi.button("Open Account Settings",
                openAccountSettings == null ? () -> {} : openAccountSettings, false);
        if (openAccountSettings == null) account.setEnabled(false);
        cards.add(toolCard("Advanced & Account","Your account and existing advanced options.",
                new JLabel("All actions keep their role restrictions."),
                account, advanced, shortcut("Sign Out", "Logout")));
        JScrollPane scroll=new JScrollPane(cards);
        scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        content.add(scroll,BorderLayout.CENTER);
        add(content,BorderLayout.CENTER);
    }
    private JPanel toolCard(String heading,String note,JComponent... controls){
        JPanel card=PremiumUi.card(new BorderLayout(0,16));
        JPanel top=PremiumUi.clear(new GridLayout(2,1,0,3));
        top.add(PremiumUi.title(heading,18));top.add(PremiumUi.caption(note));
        card.add(top,BorderLayout.NORTH);
        JPanel buttons=PremiumUi.clear(new GridLayout(controls.length,1,0,9));
        for(JComponent c:controls){
            if(c instanceof JLabel){c.setFont(PremiumUi.font(12,false));c.setForeground(PremiumUi.MUTED);}
            buttons.add(c);
        }
        card.add(buttons,BorderLayout.CENTER);return card;
    }
    private JButton shortcut(String label,String... candidates){
        JButton original=null;
        for(String candidate:candidates){original=findButton(legacyActions,candidate);if(original!=null) break;}
        final JButton underlying=original;
        JButton result=PremiumUi.button(label,()->{if(underlying!=null) underlying.doClick();},false);
        if(original==null){result.setEnabled(false);result.setToolTipText("This action is unavailable for your account role");}
        return result;
    }
    private static JButton findButton(Component c,String token){
        if(c instanceof JButton b && b.getText()!=null && b.getText().contains(token)) return b;
        if(c instanceof Container container){
            for(Component child:container.getComponents()){
                JButton result=findButton(child,token);if(result!=null)return result;
            }
        }
        return null;
    }
    private void showAdvancedControls(){
        Window parent=SwingUtilities.getWindowAncestor(this);
        JDialog dialog=new JDialog(parent,"Advanced PawTrack Controls",Dialog.ModalityType.APPLICATION_MODAL);
        Container previous=legacyActions.getParent();
        if(previous!=null)previous.remove(legacyActions);
        dialog.setLayout(new BorderLayout());
        dialog.add(new JLabel("  Original controls and classification selector"),BorderLayout.NORTH);
        dialog.add(legacyActions,BorderLayout.CENTER);
        dialog.setSize(470,650);dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        dialog.remove(legacyActions);
        if(previous!=null){previous.add(legacyActions);previous.revalidate();}
    }
}
