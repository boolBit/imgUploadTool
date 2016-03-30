import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.helpers.MessageFormatter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Listener.SelectOuputFilePathListener;
import Listener.SelectUploadFileDirPathListener;
import lombok.Data;

/**
 * @author lorabit
 * @since 16-3-18
 */
@Data
public class App extends JFrame implements Observer, ChangeListener {
  private final Logger logger = Logger.getRootLogger();

  private JButton sourceFilePathBtn;
  private JTextField filePathField;
  private JButton successOutputPathBtn;
  private JTextField excelOutputPathField;
  private JButton uploadBtn;

  private JLabel mainFileName;
  private JTextField mainPathField;
  private JLabel detailFileName;
  private JTextField detailPathField;

  private JLabel logPathLabel;
  private JTextField logPathField;

  private JLabel cookieLabel;
  private JTextField cookieField;

  private JProgressBar progressbar;
  private JLabel processBarLabel;
  private JTextArea console;
  private JScrollPane sp;
  private CustomHttpHelper httpHelper = new CustomHttpHelper();
  ExecutorService executor = Executors.newSingleThreadExecutor();

  public App() {
    super();
    super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setBounds(500, 200, 600, 450);
    this.getContentPane().setLayout(null);
    this.add(createPathLabel());
    this.add(createPathField());
    this.add(createMainLabel());
    this.add(createMainField());
    this.add(createOutputLabel());
    this.add(createOutputField());
    this.add(createDetailLable());
    this.add(createDetailField());
    this.add(createLogLabel());
    this.add(createLogField());

    this.add(createCookieLabel());
    this.add(createCookieField());

    this.add(createProgressBarLabel());
    this.add(createJTextArea());
    this.add(createUploadButton());
    this.setTitle("图片上传");
  }

  public static void main(String[] args) {
    App app = new App();
    app.sourceFilePathBtn.addActionListener(
        new SelectUploadFileDirPathListener(app.filePathField));
    app.successOutputPathBtn.addActionListener(
        new SelectOuputFilePathListener(app.excelOutputPathField));
    app.uploadBtn.addActionListener(new ClickToUploadListener(app));

    app.getHttpHelper().addObserver(app);
    app.setVisible(true);
  }

  private JButton createPathLabel() {
    if (sourceFilePathBtn == null) {
      sourceFilePathBtn = new JButton();
      sourceFilePathBtn.setBounds(34, 49, 120, 18);
      sourceFilePathBtn.setText("文件夹路径");
    }
    return sourceFilePathBtn;
  }

  private JTextField createPathField() {
    if (filePathField == null) {
      filePathField = new JTextField();
      filePathField.setBounds(180, 49, 200, 20);
    }
    return filePathField;
  }

  private JLabel createMainLabel() {
    if (mainFileName == null) {
      mainFileName = new JLabel("主图文件名");
      mainFileName.setBounds(385, 49, 80, 20);
    }
    return mainFileName;
  }

  private JTextField createMainField() {
    if (mainPathField == null) {
      mainPathField = new JTextField("主图");
      mainPathField.setBounds(470, 49, 70, 20);
    }
    return mainPathField;
  }

  private JButton createOutputLabel() {
    if (successOutputPathBtn == null) {
      successOutputPathBtn = new JButton();
      successOutputPathBtn.setBounds(34, 75, 120, 20);
      successOutputPathBtn.setText("输出路径");
    }
    return successOutputPathBtn;
  }

  private JTextField createOutputField() {
    if (excelOutputPathField == null) {
      excelOutputPathField = new JTextField();
      excelOutputPathField.setBounds(180, 75, 200, 20);
    }
    return excelOutputPathField;
  }

  private JLabel createDetailLable() {
    if (detailFileName == null) {
      detailFileName = new JLabel("详情图文件名");
      detailFileName.setBounds(385, 75, 80, 20);
    }
    return detailFileName;
  }

  private JTextField createDetailField() {
    if (detailPathField == null) {
      detailPathField = new JTextField("详情图");
      detailPathField.setBounds(470, 75, 70, 20);
    }
    return detailPathField;
  }

  private JProgressBar createProgressBar(int total) {
    if (progressbar == null) {
      progressbar = new JProgressBar();
      progressbar.setBounds(150, 120, 200, 20);
      progressbar.setOrientation(JProgressBar.HORIZONTAL);
      progressbar.setMinimum(0);
      progressbar.setMaximum(total);
      progressbar.setValue(0);
      progressbar.setStringPainted(true);
      progressbar.addChangeListener(this);
      progressbar.setPreferredSize(new Dimension(200, 20));
      progressbar.setBorderPainted(true);
      progressbar.setBackground(Color.pink);
    }
    return progressbar;
  }

  private JLabel createLogLabel() {
    if (logPathLabel == null) {
      logPathLabel = new JLabel("log path");
      logPathLabel.setBounds(60, 100, 80, 20);
    }
    return logPathLabel;
  }

  private JTextField createLogField() {
    if (logPathField == null) {
      logPathField = new JTextField();
      logPathField.setBounds(150, 100, 140, 20);
    }
    return logPathField;
  }

  private JLabel createCookieLabel() {
    if (cookieLabel == null) {
      cookieLabel = new JLabel("Cookie(not required):");
      cookieLabel.setBounds(300, 100, 80, 20);
    }
    return cookieLabel;
  }

  private JTextField createCookieField() {
    if (cookieField == null) {
      cookieField = new JTextField();
      cookieField.setBounds(390, 100, 200, 20);
    }
    return cookieField;
  }


  private JLabel createProgressBarLabel() {
    processBarLabel = new JLabel();
    processBarLabel.setBounds(150, 145, 200, 20);
    return processBarLabel;
  }

  private JScrollPane createJTextArea() {
    console = new JTextArea("", 30, 70);
    console.setSelectedTextColor(Color.BLUE);
    console.setLineWrap(true);        //激活自动换行功能
    console.setWrapStyleWord(true);            // 激活断行不断字功能
    console.setFont(new Font("标楷体", Font.BOLD, 14));
    sp = new JScrollPane(console);
    sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    sp.setBounds(100, 170, 420, 220);
    return sp;
  }

  private JButton createUploadButton() {
    if (uploadBtn == null) {
      uploadBtn = new JButton();
      uploadBtn.setBounds(150, 400, 100, 30);
      uploadBtn.setText("开始上传");
    }
    return uploadBtn;
  }

  int currentProgress;
  int totalProgress;

  @Override
  public void update(Observable o, Object arg) {
    ExeDetail detail = (ExeDetail) arg;
    switch (detail.getState()) {
      case RUNNABLE:
        Map<String, Integer> nameCount = detail.getNameAndCount();
        final int[] total = {0};
        nameCount.forEach((k, v) -> {
          total[0] += v;
          console.append("文件" + k + "总共" + v + System.lineSeparator());
        });
        this.add(createProgressBar(total[0]));
        totalProgress = total[0];
        break;
      case RUNNING:
        String name = detail.getCurrUploadingFileName();
        int index = detail.getCurrUploadingIndex();
        console.append(index + " -> " + name + System.lineSeparator());
        progressbar.setValue(++currentProgress);
        if (StringUtils.isNotEmpty(detail.getMsg())) {
          console.append(detail.getMsg() + System.lineSeparator());
        }
        if (detail.getException() != null) {
          console.append(ExceptionUtils.getStackTrace(detail.getException()));
        }
        break;
      case SUCCESSED:
        console.append("######## SUCCESSED ########" + System.lineSeparator());
        Map<String, List<String>> errorIndexs = detail.getNameAndErrorIndex();
        if (errorIndexs.size() > 0) {
          console.append("以下文件夹出错（index）：" + System.lineSeparator());
          errorIndexs.forEach((String k, List<String> indexs) -> {
            indexs.stream().forEach(msg -> console.append(msg + System.lineSeparator()));
          });
        }
        break;
      case FAILED:
        console.append("######## FAILED ########" + System.lineSeparator());
        Map<String, List<String>> errorIndexs2 = detail.getNameAndErrorIndex();
        if (errorIndexs2.size() > 0) {
          console.append("以下文件夹出错（index）：" + System.lineSeparator());
          errorIndexs2.forEach((k, indexs) -> {
            indexs.stream().forEach(msg -> console.append(k + ": " + msg + System.lineSeparator()));
          });
        }
        if (StringUtils.isNotEmpty(detail.getMsg())) {
          console.append("error msg: " + detail.getMsg() + System.lineSeparator());
        }
        if (detail.getException() != null) {
          console.append(ExceptionUtils.getStackTrace(detail.getException()));
        }
        break;
    }
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    int value = progressbar.getValue();
    if (e.getSource() == progressbar) {
      processBarLabel.setText("目前已完成进度：" + ((double) value * 100) / totalProgress + "%");
      processBarLabel.setForeground(Color.blue);
    }
  }

  public void initLog4j() {
    DailyRollingFileAppender appenderMain = new DailyRollingFileAppender();
    appenderMain.setName("main");
    appenderMain.setFile(getLogPath() + File.separator + "main.log");
    appenderMain.setDatePattern("'.'yyyy-MM-dd'.log'"); //拼接到原有的名字后面
    appenderMain.setLayout(new PatternLayout("%d %-5p  %m%n"));
    appenderMain.setThreshold(Level.INFO);
    appenderMain.setAppend(true);
    appenderMain.activateOptions();
    Logger.getRootLogger().addAppender(appenderMain);

    DailyRollingFileAppender appenderOrderInfo = new DailyRollingFileAppender();
    appenderOrderInfo.setName("orderInfos");
    appenderOrderInfo.setFile(getLogPath() + File.separator + "order.log");
    appenderOrderInfo.setDatePattern("'.'yyyy-MM-dd'.log'"); //拼接到原有的名字后面
    appenderOrderInfo.setLayout(new PatternLayout("%d %-5p  %m%n"));
    appenderOrderInfo.setThreshold(Level.INFO);
    appenderOrderInfo.setAppend(true);
    appenderOrderInfo.activateOptions();
    Logger.getLogger("orderInfo").addAppender(appenderOrderInfo);

//    ConsoleAppender consoleAppender = new ConsoleAppender();
  }

  boolean write = false;

  public String getLogPath() {
    String path = System.getProperty("user.dir");
    if (StringUtils.isNotEmpty(logPathField.getText())) {
      path = logPathField.getText();
    }
    if (!write) {
      console.append("write log to " + path + System.lineSeparator());
      write = true;
    }
    return path;
  }
}

@Data
class UploadContext {
  String uploadFilePath;
  String successOutputPath;
  String errorOutputPath;
  String mainFileName = "主图";
  String detailFileName = "详情图";
  String cookie;

  public UploadContext(App app) {
    this.uploadFilePath = app.getFilePathField().getText();
    this.successOutputPath = app.getExcelOutputPathField().getText();
  }

  public UploadContext() {
  }
}

class ClickToUploadListener implements ActionListener {
  private App app;
  private UploadContext ctx = new UploadContext();

  public ClickToUploadListener(App app) {
    this.app = app;
  }

  public String check() {
    String msg = null;
    if (StringUtils.isEmpty(ctx.getUploadFilePath())
        || StringUtils.isEmpty(ctx.getSuccessOutputPath())) {
      msg = MessageFormatter.format("必须指定上传文件目录和输出路径 dir: {} output {}",
          ctx.getUploadFilePath(), ctx.getSuccessOutputPath()).getMessage();
    }
    if (!new File(ctx.getUploadFilePath()).exists()) {
      msg = MessageFormatter.format("上传文件目录 {} 不存在", ctx.getUploadFilePath()).getMessage();
    }
    return msg;
  }

  public void generete() {
    ctx.setUploadFilePath(app.getFilePathField().getText());
    ctx.setSuccessOutputPath(app.getExcelOutputPathField().getText());
    ctx.setErrorOutputPath(ctx.getSuccessOutputPath());
    ctx.setDetailFileName(app.getDetailPathField().getText());
    ctx.setMainFileName(app.getMainPathField().getText());
    ctx.setCookie(app.getCookieField().getText());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    generete();
    String msg = check();
    if (StringUtils.isNotBlank(msg)) {
      JOptionPane.showMessageDialog(null, msg, "error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    app.write = false;
    app.initLog4j();
    app.getUploadBtn().setEnabled(false);
    app.executor.submit(() -> {
      app.getHttpHelper().upload(ctx);
      app.getUploadBtn().setEnabled(true);
      return null;
    });
  }
}


