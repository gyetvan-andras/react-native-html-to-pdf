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

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.UiThreadUtil;

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
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.pdf.PrintedPdfDocument;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class RNHTMLtoPDFModule extends ReactContextBaseJavaModule {

  private Promise promise;
  private final ReactApplicationContext mReactContext;
	WebView mWebView;
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

  @ReactMethod
  public void printhtml(final String html, final Promise promise) {

    final String jobName = "Document";

    try {
      UiThreadUtil.runOnUiThread(new Runnable() {
          @Override
          public void run() {
              // Create a WebView object specifically for printing
              WebView webView = new WebView(mReactContext);
              webView.setWebViewClient(new WebViewClient() {
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return false;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    // Get the print manager.
                    PrintManager printManager = (PrintManager) getCurrentActivity().getSystemService(
                            Context.PRINT_SERVICE);
                    // Create a wrapper PrintDocumentAdapter to clean up when done.
                    PrintDocumentAdapter adapter = new PrintDocumentAdapter() {
                        private final PrintDocumentAdapter mWrappedInstance =
                                mWebView.createPrintDocumentAdapter();
                        @Override
                        public void onStart() {
                            mWrappedInstance.onStart();
                        }
                        @Override
                        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                CancellationSignal cancellationSignal, LayoutResultCallback callback,
                                Bundle extras) {
                            mWrappedInstance.onLayout(oldAttributes, newAttributes, cancellationSignal,
                                    callback, extras);
                        }
                        @Override
                        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal, WriteResultCallback callback) {
                            mWrappedInstance.onWrite(pages, destination, cancellationSignal, callback);
                        }
                        @Override
                        public void onFinish() {
                            mWrappedInstance.onFinish();
                        }
                    };
                    // Pass in the ViewView's document adapter.
                    printManager.print(jobName, adapter, null);
                    mWebView = null;
                    promise.resolve(jobName);
                }
              });

              webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null);

              // Keep a reference to WebView object until you pass the PrintDocumentAdapter
              // to the PrintManager
              mWebView = webView;
          }
      });
    } catch (Exception e) {
      promise.reject("print_error", e);
    }
  }	
}
