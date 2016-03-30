package Listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

/**
 * @author lorabit
 * @since 16-3-28
 */
public class SelectUploadFileDirPathListener implements ActionListener {
  private JTextField field;

  public SelectUploadFileDirPathListener(JTextField field) {
    this.field = field;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.showDialog(new JLabel(), "选择");
    File file = chooser.getSelectedFile();
    if (file == null) return;
    System.out.println(file.getAbsolutePath());
    field.setText(file.getAbsolutePath());
  }
}
