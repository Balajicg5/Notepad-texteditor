import javax.swing.*;
import javax.swing.undo.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class TextEditor extends JFrame implements ActionListener {
    private JTextArea textArea;
    private JFileChooser fileChooser;
    private UndoManager undoManager;
    private JCheckBoxMenuItem wordWrap;
    private JLabel statusBar;
    private Font currentFont;
    private boolean isModified;
    private File currentFile;
    private JSpinner fontSizeSpinner;
    private JComboBox<String> fontFamilySelector;
    
    
    private Theme currentTheme;
    private JMenu themeMenu;

    private static class Theme {
        String name;
        Color backgroundColor;
        Color textColor;
        Color caretColor;
        Color selectionColor;
        Color selectionTextColor;
        Color statusBarBackground;
        Color statusBarForeground;
        
        Theme(String name, Color backgroundColor, Color textColor, Color caretColor, 
              Color selectionColor, Color selectionTextColor, 
              Color statusBarBackground, Color statusBarForeground) {
            this.name = name;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.caretColor = caretColor;
            this.selectionColor = selectionColor;
            this.selectionTextColor = selectionTextColor;
            this.statusBarBackground = statusBarBackground;
            this.statusBarForeground = statusBarForeground;
        }
    }
    
    
    private static final Theme[] THEMES = {
        new Theme("Light", 
            new Color(255, 255, 255),
            new Color(0, 0, 0),    
            new Color(0, 0, 0),       
            new Color(173, 214, 255), 
            new Color(0, 0, 0),       
            new Color(238, 238, 238), 
            new Color(0, 0, 0)        
        ),
        new Theme("Dark", 
            new Color(43, 43, 43),    
            new Color(169, 183, 198), 
            new Color(187, 187, 187), 
            new Color(33, 66, 131),   
            new Color(169, 183, 198), 
            new Color(60, 63, 65),    
            new Color(187, 187, 187)  
        ),
        new Theme("Sepia", 
            new Color(251, 240, 217), 
            new Color(95, 75, 50),    
            new Color(95, 75, 50),    
            new Color(244, 223, 184),
            new Color(95, 75, 50),    
            new Color(236, 224, 200), 
            new Color(95, 75, 50)     
        ),
        new Theme("High Contrast", 
            new Color(0, 0, 0),     
            new Color(0, 255, 0),     
            new Color(0, 255, 0),     
            new Color(0, 102, 0),     
            new Color(0, 255, 0),     
            new Color(0, 0, 0),       
            new Color(0, 255, 0)      
        )
    };
    
    public TextEditor() {
        setTitle("Notepad");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (checkSaveBeforeClosing()) {
                    System.exit(0);
                }
            }
        });

        
        currentFont = new Font("Monospaced", Font.PLAIN, 14);
        textArea = new JTextArea();
        textArea.setFont(currentFont);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        
        statusBar = new JLabel("Ready");
        add(statusBar, BorderLayout.SOUTH);

        
        isModified = false;
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { textModified(); }
            public void removeUpdate(DocumentEvent e) { textModified(); }
            public void changedUpdate(DocumentEvent e) { textModified(); }
        });

        undoManager = new UndoManager();
        textArea.getDocument().addUndoableEditListener(undoManager);

        
        createToolbar();

        
        createMenus();

        
        setTheme(THEMES[0]); 
        
        
        addKeyboardShortcuts();
    }
    
    private void createToolbar() {
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontFamilySelector = new JComboBox<>(fonts);
        fontFamilySelector.setSelectedItem("Monospaced");
        fontFamilySelector.addActionListener(e -> updateFont());
        toolBar.add(new JLabel("Font: "));
        toolBar.add(fontFamilySelector);
        
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(14, 8, 72, 2));
        fontSizeSpinner.addChangeListener(e -> updateFont());
        toolBar.add(new JLabel(" Size: "));
        toolBar.add(fontSizeSpinner);

        
        JButton boldButton = new JButton("B");
        boldButton.setFont(new Font("Dialog", Font.BOLD, 12));
        boldButton.addActionListener(e -> toggleBold());
        toolBar.add(boldButton);
    }
    
    private void createMenus() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu viewMenu = new JMenu("View");
        JMenu toolsMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);

    
        themeMenu = new JMenu("Themes");
        viewMenu.add(themeMenu);
        
        
        ButtonGroup themeGroup = new ButtonGroup();
        for (Theme theme : THEMES) {
            JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(theme.name);
            themeItem.addActionListener(e -> setTheme(theme));
            themeGroup.add(themeItem);
            themeMenu.add(themeItem);
            if (theme.name.equals("Light")) {
                themeItem.setSelected(true);
            }
        }

        
        fileChooser = new JFileChooser();
        
        
        addMenuItem(fileMenu, "New", this);
        addMenuItem(fileMenu, "Open", this);
        addMenuItem(fileMenu, "Save", this);
        addMenuItem(fileMenu, "Save As", this);
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Print", this);
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Exit", this);
        
        
        addMenuItem(editMenu, "Undo", this);
        addMenuItem(editMenu, "Redo", this);
        editMenu.addSeparator();
        addMenuItem(editMenu, "Cut", this);
        addMenuItem(editMenu, "Copy", this);
        addMenuItem(editMenu, "Paste", this);
        editMenu.addSeparator();
        addMenuItem(editMenu, "Find", this);
        addMenuItem(editMenu, "Replace", this);
        addMenuItem(editMenu, "Select All", this);
        
        
        wordWrap = new JCheckBoxMenuItem("Word Wrap");
        wordWrap.addActionListener(this);
        toolsMenu.add(wordWrap);
        addMenuItem(toolsMenu, "Insert Date/Time", this);
        addMenuItem(toolsMenu, "Word Count", this);
        
    
       
        addMenuItem(helpMenu, "About", this);

        
        addKeyboardShortcuts();
    }
    
    private void setTheme(Theme theme) {
        currentTheme = theme;
        
        
        textArea.setBackground(theme.backgroundColor);
        textArea.setForeground(theme.textColor);
        textArea.setCaretColor(theme.caretColor);
        textArea.setSelectionColor(theme.selectionColor);
        textArea.setSelectedTextColor(theme.selectionTextColor);
        
        
        statusBar.setBackground(theme.statusBarBackground);
        statusBar.setForeground(theme.statusBarForeground);
        statusBar.setOpaque(true);
        
        
        JScrollPane scrollPane = (JScrollPane) textArea.getParent().getParent();
        scrollPane.getViewport().setBackground(theme.backgroundColor);
        
        
        SwingUtilities.updateComponentTreeUI(this);
    }

    
    private void addKeyboardShortcuts() {
      
      int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      
      
      addKeyboardShortcut("ctrl N", "New", KeyStroke.getKeyStroke('N', ctrl));
      addKeyboardShortcut("ctrl O", "Open", KeyStroke.getKeyStroke('O', ctrl));
      addKeyboardShortcut("ctrl S", "Save", KeyStroke.getKeyStroke('S', ctrl));
      addKeyboardShortcut("ctrl P", "Print", KeyStroke.getKeyStroke('P', ctrl));
      
      
      addKeyboardShortcut("ctrl Z", "Undo", KeyStroke.getKeyStroke('Z', ctrl));
      addKeyboardShortcut("ctrl Y", "Redo", KeyStroke.getKeyStroke('Y', ctrl));
      addKeyboardShortcut("ctrl F", "Find", KeyStroke.getKeyStroke('F', ctrl));
      addKeyboardShortcut("ctrl H", "Replace", KeyStroke.getKeyStroke('H', ctrl));
      addKeyboardShortcut("ctrl A", "Select All", KeyStroke.getKeyStroke('A', ctrl));
  }
  
  private void addKeyboardShortcut(String key, String action, KeyStroke keyStroke) {
      textArea.getInputMap().put(keyStroke, key);
      textArea.getActionMap().put(key, new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
              TextEditor.this.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, action));
          }
      });
  }

  private void addMenuItem(JMenu menu, String title, ActionListener listener) {
      JMenuItem item = new JMenuItem(title);
      item.addActionListener(listener);
      menu.add(item);
  }
  
  private void textModified() {
      if (!isModified) {
          isModified = true;
          updateTitle();
      }
      updateStatusBar();
  }
  
  private void updateStatusBar() {
      String text = textArea.getText();
      int characters = text.length();
      int words = text.isEmpty() ? 0 : text.split("\\s+").length;
      int lines = textArea.getLineCount();
      statusBar.setText(String.format("Lines: %d   Words: %d   Characters: %d", lines, words, characters));
  }
  
  private void updateTitle() {
      String title = "Untitled";
      if (currentFile != null) {
          title = currentFile.getName();
      }
      setTitle(title + (isModified ? " *" : "") + " - Text Editor");
  }
  
  private void updateFont() {
      String fontFamily = (String) fontFamilySelector.getSelectedItem();
      int fontSize = (Integer) fontSizeSpinner.getValue();
      int style = currentFont.getStyle();
      currentFont = new Font(fontFamily, style, fontSize);
      textArea.setFont(currentFont);
  }
  
  private void toggleBold() {
      int style = currentFont.getStyle() ^ Font.BOLD;
      currentFont = currentFont.deriveFont(style);
      textArea.setFont(currentFont);
  }
  
  private boolean checkSaveBeforeClosing() {
      if (isModified) {
          int response = JOptionPane.showConfirmDialog(this,
              "Do you want to save changes?",
              "Unsaved Changes",
              JOptionPane.YES_NO_CANCEL_OPTION);
          
          if (response == JOptionPane.YES_OPTION) {
              saveFile(false);
              return !isModified; 
          }
          return response == JOptionPane.NO_OPTION;
      }
      return true;
  }

  public void actionPerformed(ActionEvent e) {
      String command = e.getActionCommand();
      
      switch (command) {
          case "New":
              if (checkSaveBeforeClosing()) {
                  textArea.setText("");
                  isModified = false;
                  currentFile = null;
                  updateTitle();
              }
              break;
          case "Open":
              if (checkSaveBeforeClosing()) {
                  openFile();
              }
              break;
          case "Save":
              saveFile(false);
              break;
          case "Save As":
              saveFile(true);
              break;
          case "Print":
              try {
                  textArea.print();
              } catch (Exception ex) {
                  JOptionPane.showMessageDialog(this, "Error printing: " + ex.getMessage());
              }
              break;
          case "Exit":
              if (checkSaveBeforeClosing()) {
                  System.exit(0);
              }
              break;
          case "Cut":
              textArea.cut();
              break;
          case "Copy":
              textArea.copy();
              break;
          case "Paste":
              textArea.paste();
              break;
          case "Select All":
              textArea.selectAll();
              break;
          case "Undo":
              if (undoManager.canUndo()) undoManager.undo();
              break;
          case "Redo":
              if (undoManager.canRedo()) undoManager.redo();
              break;
          case "Find":
              showFindDialog();
              break;
          case "Replace":
              showReplaceDialog();
              break;
          case "Insert Date/Time":
              insertDateTime();
              break;
          case "Word Count":
              showWordCount();
              break;
          case "About":
              JOptionPane.showMessageDialog(this,
                  "Notepad\nVersion 1.0\n\nAn enhanced text editor with a modern user interface.",
                  "About",
                  JOptionPane.INFORMATION_MESSAGE);
              break;
          case "Word Wrap":
              textArea.setLineWrap(wordWrap.isSelected());
              textArea.setWrapStyleWord(wordWrap.isSelected());
              break;
      }
  }
  
  private void showFindDialog() {
      String searchText = JOptionPane.showInputDialog(this, "Find what:");
      if (searchText != null && !searchText.isEmpty()) {
          String text = textArea.getText();
          int index = text.indexOf(searchText);
          if (index != -1) {
              textArea.setCaretPosition(index);
              textArea.select(index, index + searchText.length());
          } else {
              JOptionPane.showMessageDialog(this, "Text not found");
          }
      }
  }
  
  private void showReplaceDialog() {
      JPanel panel = new JPanel(new GridLayout(2, 2));
      panel.add(new JLabel("Find what:"));
      JTextField findField = new JTextField(20);
      panel.add(findField);
      panel.add(new JLabel("Replace with:"));
      JTextField replaceField = new JTextField(20);
      panel.add(replaceField);
      
      int result = JOptionPane.showConfirmDialog(this, panel, "Replace",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
          
      if (result == JOptionPane.OK_OPTION) {
          String findText = findField.getText();
          String replaceText = replaceField.getText();
          if (!findText.isEmpty()) {
              String text = textArea.getText();
              text = text.replace(findText, replaceText);
              textArea.setText(text);
          }
      }
  }
  
  private void insertDateTime() {
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      String dateTime = now.format(formatter);
      textArea.insert(dateTime, textArea.getCaretPosition());
  }
  
  private void showWordCount() {
      String text = textArea.getText();
      int characters = text.length();
      int words = text.isEmpty() ? 0 : text.split("\\s+").length;
      int lines = textArea.getLineCount();
      
      JOptionPane.showMessageDialog(this,
          String.format("Lines: %d\nWords: %d\nCharacters: %d", lines, words, characters),
          "Word Count",
          JOptionPane.INFORMATION_MESSAGE);
  }
  
  private void openFile() {
      int returnValue = fileChooser.showOpenDialog(this);
      if (returnValue == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();
          try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
              textArea.read(reader, null);
              currentFile = file;
              isModified = false;
              updateTitle();
              updateStatusBar();
          } catch (IOException ex) {
              JOptionPane.showMessageDialog(this,
                  "Error opening file: " + ex.getMessage(),
                  "Error",
                  JOptionPane.ERROR_MESSAGE);
          }
      }
  }
  
  private void saveFile(boolean saveAs) {
      if (saveAs || currentFile == null) {
          int returnValue = fileChooser.showSaveDialog(this);
          if (returnValue != JFileChooser.APPROVE_OPTION) return;
          currentFile = fileChooser.getSelectedFile();
      }
      
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
          textArea.write(writer);
          isModified = false;
          updateTitle();
      } catch (IOException ex) {
          JOptionPane.showMessageDialog(this,
              "Error saving file: " + ex.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE);
      }
  }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new TextEditor().setVisible(true));
    }
}