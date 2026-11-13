# UI Component Hierarchy Diagram

## Overview
This document describes the Swing UI component hierarchy for the PawTrack application.

## MainFrame (JFrame)
```
MainFrame
├── JTabbedPane (contentPanel with CardLayout)
│   ├── "Animals" → JPanel (buildAnimalTab)
│   │   └── JScrollPane
│   │       └── JTable (animalTable) with AnimalTableModel
│   ├── "Shelter Analytics" → AnalyticsPanel (staff only)
│   │   ├── JComboBox (metric selector)
│   │   ├── JButton (refresh)
│   │   ├── BarChartPanel
│   │   └── JScrollPane → JTextArea (summary)
│   ├── "Activity Log" → JScrollPane → JTextArea (logArea) (staff only)
│   ├── "Waitlist Requests" → JPanel (buildWaitlistTab) (staff only)
│   │   ├── JToolBar (top)
│   │   ├── JScrollPane
│   │       └── JTable (waitlistTable) with DefaultTableModel
│   └── "My Records" → JScrollPane → JTextArea (myRecordsArea) (adopter only)
├── West: JScrollPane (toolbarScroll)
│   └── JPanel (toolbar) - Vertical BoxLayout
│       ├── RoundedButton (toggle toolbar)
│       ├── User Profile Panel
│       ├── RoundedButton (Logout)
│       ├── RoundedButton (List / Refresh Animals) - Ctrl+R
│       ├── RoundedButton (Filter by Classification) - Ctrl+F
│       ├── RoundedButton (Import Intake History) - Ctrl+I (staff)
│       ├── RoundedButton (Add Animal) (staff)
│       ├── RoundedButton (Edit Animal) (staff)
│       ├── RoundedButton (Delete Animal) (staff)
│       ├── RoundedButton (Record Interest)
│       ├── RoundedButton (Mark Adopted) (staff)
│       ├── RoundedButton (Undo Last Status Change) - Ctrl+U (staff)
│       ├── RoundedButton (Fetch Sample Pet Image)
│       ├── RoundedComboBox (classificationFilter)
│       └── JProgressBar
├── East: JPanel (imagePanel)
│   ├── JLabel (title)
│   ├── JLabel (subtitle)
│   ├── JPanel (previewCard)
│   │   └── JLabel (imageLabel)
│   ├── JLabel (imageInfoLabel)
│   └── LogoLabel (PawTrack logo)
└── South: JLabel (statusLabel)

## Dialogs
- JFileChooser (CSV import)
- JOptionPane (adopter forms, confirmations, messages)
- JDialog (status filter popup)
- LoginDialog (LoginPanel)
- CreateAccountDialog (CreateAccountPanel)

## Custom Components
- RoundedUi.RoundedButton
- RoundedUi.RoundedComboBox
- RoundedUi.RoundedBorder
- RoundedUi.RoundedPanel
- LoginPanel.LogoLabel
- RoundedUi.RoundedButton (tab buttons)
- BlueTableRenderer (custom table cell renderer)
- StatusHeaderRenderer (custom table header renderer)
- StatusPopupPanel (custom dropdown panel)

## Keyboard Shortcuts
- Ctrl+R: Refresh animals
- Ctrl+F: Focus classification filter
- Ctrl+I: Import CSV (staff)
- Ctrl+U: Undo last status change (staff)
- F5: Refresh analytics
- Escape: Close dialogs/popups
```