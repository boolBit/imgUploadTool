import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * @author lorabit
 * @since 16-3-1
 */
public class CustomHttpHelper extends java.util.Observable {

  private static final Logger logger = Logger.getLogger(CustomHttpHelper.class);
  private static final Logger loggerOrderInfo = Logger.getLogger("orderInfo");


  private static CloseableHttpClient httpClient;

  private static final int DEFAULT_TIMEOUT = 3000;

  public static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    RequestConfig defaultRequestConfig = RequestConfig.custom()
        .setSocketTimeout(DEFAULT_TIMEOUT)
        .setConnectTimeout(DEFAULT_TIMEOUT)
        .build();
    httpClient = HttpClients.custom()
        .setDefaultRequestConfig(defaultRequestConfig)
        .build();
    Runtime.getRuntime().addShutdownHook(
        new Thread(() -> {
          try {
            if (httpClient != null) {
              httpClient.close();
            }
          } catch (IOException e) {
          }
        }
        ));
  }

  public static String postImg(Map<String, File> files, String cookie) throws IOException {
    HttpPost httppost = new HttpPost("http://www.duitang.com/napi/upload/photo/");
    MultipartEntityBuilder reqBuilder = MultipartEntityBuilder.create();
    if (files != null && !files.isEmpty()) {
      for (Map.Entry<String, File> file : files.entrySet()) {
        reqBuilder.addPart(file.getKey(), new FileBody(file.getValue()));
      }
    }
    HttpEntity reqEntity = reqBuilder.build();
    httppost.setEntity(reqEntity);
    String ck = "__utmt=1; username=matchDay; " +
        "sessionid=dab0fe92-7cf9-4646-aef7-ea075cf6219a; _auth_user_id=10249029; js=1; __utma=74400135.586148953.1442301992.1457452142.1458227400.13; __utmb=74400135.2.10.1458227400; __utmc=74400135; __utmz=74400135.1455959494.11.5.utmcsr=neitui.me|utmccn=(referral)|utmcmd=referral|utmcc";
    if (StringUtils.isNotEmpty(cookie)) {
      ck = cookie;
    }
    httppost.setHeader("Cookie", ck);
    return httpClient.execute(httppost, new basicHandler<String>() {
      @Override
      String handler(HttpResponse response) throws IOException {
        String s = EntityUtils.toString(response.getEntity());
        Map resp = mapper.readValue(s, Map.class);
        return (String) ((Map) resp.get("data")).get("img_url");
      }
    }, new BasicHttpContext());
  }


  public void upload(UploadContext ctx) {
    ExeDetail exeDetail = new ExeDetail();
    File img = new File(ctx.getUploadFilePath());
    LinkedHashMap<String, UploadImg> uploadedImgs = new LinkedHashMap<>();
    //make for output
    List<Object> contents = new ArrayList<>();
    List<String> displayHeaders = Lists.newArrayList("商品名称", "主图", "详情图");
    List<String> headers = Lists.newArrayList("name", "main", "detail");
    try {
      HashMap<String, Integer> nameAndCount = new HashMap<>();
      logger.info("start to upload file : " + img.getName());
      for (File imgFiles : img.listFiles()) {
        if (ctx.getDetailFileName().equals(imgFiles.getName())) {
          int count = imgFiles.listFiles().length;
          nameAndCount.put(imgFiles.getName(), count);
          logUploadOrderInfo(imgFiles);
        } else if (ctx.getMainFileName().equals(imgFiles.getName())) {
          int count = imgFiles.listFiles().length;
          nameAndCount.put(imgFiles.getName(), count);
          logUploadOrderInfo(imgFiles);
        }
      }

      //RUNNABLE
      exeDetail.setNameAndCount(nameAndCount);
      exeDetail.setState(ExeDetail.State.RUNNABLE);
      notifyConsole(exeDetail);

      for (File imgFiles : img.listFiles()) {
        if (ctx.getDetailFileName().equals(imgFiles.getName())) {
          uploadPicsInDir(imgFiles, ctx, uploadedImgs, UploadImg.URLTYPE.DETAIL, exeDetail);
        } else if (ctx.getMainFileName().equals(imgFiles.getName())) {
          Thread.sleep(1000);
          uploadPicsInDir(imgFiles, ctx, uploadedImgs, UploadImg.URLTYPE.MAIN, exeDetail);
        }
      }

      //make result
      for (Map.Entry entry : uploadedImgs.entrySet()) {
        Map m = new HashMap();
        m.put("name", entry.getKey());
        m.put("main", ((UploadImg) entry.getValue()).getMainPicsUrl());
        m.put("detail", ((UploadImg) entry.getValue()).getDetailPicsUrl());
        contents.add(m);
      }
      ExcelMaker.from(contents, headers)
          .displayHeaders(displayHeaders)
          .resultType(ExcelMaker.ExcelFileType.XLS)
          .create(ctx.getSuccessOutputPath());
      exeDetail.setState(ExeDetail.State.SUCCESSED);
      notifyConsole(exeDetail);
    } catch (Exception e) {
      logger.error("error during upload pics " + e.getMessage(), e);
      exeDetail.setMsg("");
      exeDetail.setState(ExeDetail.State.FAILED);
      exeDetail.setException(e);
      if (StringUtils.isEmpty(ctx.getErrorOutputPath())) {
        notifyConsole(exeDetail);
        return;
      }

      for (Map.Entry entry : uploadedImgs.entrySet()) {
        Map m = new HashMap();
        m.put("name", entry.getKey());
        m.put("main", ((UploadImg) entry.getValue()).getMainPicsUrl());
        m.put("detail", ((UploadImg) entry.getValue()).getDetailPicsUrl());
        contents.add(m);
      }
      try {
        ExcelMaker.from(contents, headers)
            .displayHeaders(displayHeaders)
            .resultType(ExcelMaker.ExcelFileType.XLS)
            .create(ctx.getErrorOutputPath());
        exeDetail.setMsg("已保存error excel 到" + ctx.getErrorOutputPath());
      } catch (IOException e1) {
        logger.error("fails in handle error wirte to excel" + e1.getMessage(), e1);
        try {
          String msg = mapper.writeValueAsString(contents);
          exeDetail.setMsg("error uploaded info :" + msg);
          File f = new File(ctx.getErrorOutputPath());
          FileUtils.write(f, msg);
          //todo serialize
        } catch (Exception e2) {
          logger.error("fails in handle error write text msg ", e2);
        }
      }
      notifyConsole(exeDetail);
    }
  }


  private void logUploadOrderInfo(File imgFiles) {
    loggerOrderInfo.info("#### the order of  " + imgFiles.getName());
    File[] files = imgFiles.listFiles();
    Arrays.sort(files);
    for (File f : files) {
      File[] pics = f.listFiles();
      Arrays.sort(pics);
      loggerOrderInfo.info(f.getName() + "===> ");
      for (File p : pics) {
        loggerOrderInfo.info(p.getName());
      }
    }
  }

  private void notifyConsole(ExeDetail detail) {
    this.setChanged();
    this.notifyObservers(detail);
  }

  public static void main(String[] args) {
    UploadContext ctx = new UploadContext();
    ctx.setUploadFilePath("/home/hellokitty/桌面/测试");
    ctx.setSuccessOutputPath("/home/hellokitty/桌面/success.xls");
//    upload(ctx);
  }


  abstract static class basicHandler<T> implements ResponseHandler<T> {
    private final int UNSUCCESS_CODE = 300;

    @Override
    public T handleResponse(HttpResponse response) throws IOException {
      if (response.getStatusLine().getStatusCode() >= UNSUCCESS_CODE) {
        int code = response.getStatusLine().getStatusCode();
        String msg = EntityUtils.toString(response.getEntity());
        logger.info("code:" + code + "resp:" + msg);
        throw new HttpResponseException(response.getStatusLine().getStatusCode(),
            "request error. code:" + code + "resp:" + msg);
      }
      return handler(response);
    }

    abstract T handler(HttpResponse response) throws IOException;
  }

  public static boolean isImg(File f) {
    if (f != null && !f.isDirectory()) {
      if (
          f.getName().endsWith("jpg") || f.getName().endsWith("jpeg")
              || f.getName().endsWith("png") || f.getName().endsWith("tif")
              || f.getName().endsWith("gif") || f.getName().endsWith("jpe"))
        return true;
    }
    return false;
  }

  public void uploadPicsInDir(
      File imgFiles,
      UploadContext ctx,
      LinkedHashMap<String, UploadImg> imgs,
      UploadImg.URLTYPE type,
      ExeDetail detail
  ) throws InterruptedException, IOException {
    Map<String, File> postParam = new HashMap();
    File[] picFiles = imgFiles.listFiles();
    if (picFiles == null || picFiles.length == 0) return;
    Arrays.sort(picFiles);
    logger.info(imgFiles.getName() + "总共: " + picFiles.length);
    int count = 0;
    for (File detailFile : picFiles) {
      count++;
      logger.info("at " + count);
      String name = detailFile.getName().trim();

      // RUNNING UPLOAD new file
      detail.setState(ExeDetail.State.RUNNING);
      detail.setCurrUploadingFileName(name);
      detail.setCurrUploadingIndex(count);
      detail.setMsg("");
      notifyConsole(detail);

      Thread.sleep(100);
      if (!imgs.containsKey(name)) {
        imgs.put(name, new UploadImg());
      }
      List<String> uploadedURLs = new ArrayList<>();
      File[] pics = detailFile.listFiles();
      if (pics == null || pics.length == 0) continue;
      Arrays.sort(pics);
      for (File pic : pics) {
        if (isImg(pic)) {
          postParam.put("img", pic);
          try {
            detail.exception = null;
            detail.msg = "";
            String url = postImg(postParam, ctx.cookie);
            uploadedURLs.add(url);
          } catch (Exception e) {
            String msg = String.format("upload %s 's pic: %s failed + at count: %d",
                detailFile.getName(), pic.getName(), count);
            detail.setMsg(msg);
            logger.error(msg, e);
            List<String> errItems = detail.getNameAndErrorIndex().get(name);
            if (errItems == null) {
              detail.getNameAndErrorIndex().put(name, new ArrayList<>());
              errItems = detail.getNameAndErrorIndex().get(name);
            }
            errItems.add(count + ":" + pic.getName());
            detail.setException(e);
            this.setChanged();
            this.notifyObservers(detail);
          }
        }
      }

      switch (type) {
        case MAIN:
          imgs.get(name).setMainPicsUrl(Joiner.on(";").join(uploadedURLs));
          break;
        case DETAIL:
          imgs.get(name).setDetailPicsUrl(Joiner.on(";").join(uploadedURLs));
          break;
        default:
          logger.warn("no match url type : " + type + Joiner.on(";").join(uploadedURLs));
          break;
      }
    }
  }
}

@Data
class ExeDetail {
  Map<String, Integer> nameAndCount = new LinkedHashMap<>();
  Map<String, List<String>> nameAndErrorIndex = new LinkedHashMap<>();
  String currUploadingFileName;
  int currUploadingIndex;
  String msg;
  State state;
  Throwable exception;

  enum State {
    RUNNABLE,
    RUNNING,
    SUCCESSED,
    FAILED
  }
}


@Data
class UploadImg {
  String name;
  List<File> mainPics;
  List<File> detailPics;
  String mainPicsUrl;
  String detailPicsUrl;

  enum URLTYPE {
    MAIN,
    DETAIL
  }
}




