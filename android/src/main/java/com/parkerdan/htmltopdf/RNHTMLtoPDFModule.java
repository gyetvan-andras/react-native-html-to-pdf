package com.parkerdan.htmltopdf;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.UUID;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;

import android.os.Environment;
import android.util.Log;

public class RNHTMLtoPDFModule extends ReactContextBaseJavaModule {

  private Promise promise;
  private final ReactApplicationContext mReactContext;

  public RNHTMLtoPDFModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mReactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNHTMLtoPDF";
  }

  @ReactMethod
  public void convert(final ReadableMap options, final Promise promise) {
    try {
			File destinationFile;
      String htmlString = options.hasKey("html") ? options.getString("html") : null;
      if (htmlString == null) return;
      float height = options.hasKey("height") ? (float)options.getDouble("height") : 0;
      float width = options.hasKey("width") ? (float)options.getDouble("width") : 0;

      // String fileName = options.hasKey("fileName") ? options.getString("fileName") : "";

      String fileName;
      if (options.hasKey("fileName")) {
        fileName = options.getString("fileName");
      } else {
        fileName = UUID.randomUUID().toString();
      }

      if (options.hasKey("directory") && options.getString("directory").equals("docs")) {
        File path = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS);
        if (!path.exists()) path.mkdir();
        destinationFile = new File(path, fileName + ".pdf");
      } else {
        destinationFile = getTempFile(fileName);
      }
			
      String filePath = getFilePath(htmlString, destinationFile, 0, 0, height, width);

      WritableMap resultMap = Arguments.createMap();
      resultMap.putString("filePath", filePath);

      promise.resolve(resultMap);

    } catch (Exception e) {
      promise.reject(e.getMessage());
    }
  }
  
  private String getFilePath(String htmlString, File file, float llx, float lly, float urx, float ury) throws Exception {

    // File path = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS);

    // if ( !path.exists() ) {
    //   path.mkdir();
    // }

    // File file = new File(path, fileName.equals("") ? "MyPdf.pdf" : fileName);

    try {
      Rectangle pageSize = urx == 0 && ury == 0 ? PageSize.A4 : new Rectangle(llx, lly, urx, ury);        

      String html = htmlString;

      Document doc = new Document(pageSize);

      InputStream in = new ByteArrayInputStream(html.getBytes());

      PdfWriter pdf = PdfWriter.getInstance(doc, new FileOutputStream(file));

      doc.open();

      XMLWorkerHelper.getInstance().parseXHtml(pdf, doc, in);

      doc.close();

      in.close();

      String absolutePath = file.getAbsolutePath();

      return absolutePath;

    } catch (Exception e) {
      throw new Exception();
    }
  }

  private File getTempFile(String fileName) throws Exception {
    try {
      File outputDir = getReactApplicationContext().getCacheDir();
      File outputFile = File.createTempFile(fileName, ".pdf", outputDir);

      return outputFile;

    } catch (Exception e) {
      throw new Exception(e);
    }
  }
	
}
