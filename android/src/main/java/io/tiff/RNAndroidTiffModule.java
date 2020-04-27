package io.tiff;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import org.beyka.tiffbitmapfactory.IProgressListener;
import org.beyka.tiffbitmapfactory.TiffConverter;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RNAndroidTiffModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public RNAndroidTiffModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAndroidTiff";
    }

    // Method for save file from base64
    public File saveImage(final String imageData) throws IOException {
        final byte[] imgBytesData = android.util.Base64.decode(imageData, android.util.Base64.DEFAULT);

        final File file = File.createTempFile("image", null, this.reactContext.getCacheDir());
        final FileOutputStream fileOutputStream;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        try {
            bufferedOutputStream.write(imgBytesData);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    @ReactMethod
    public void convertBase64ToPng(String base64String, Promise promise) {
        try {
            // save img locally
            File file = this.saveImage(base64String);

            // Convert to PNG
            TiffConverter.ConverterOptions options = new TiffConverter.ConverterOptions();
            options.throwExceptions = false; // Set to true if you want use java exception mechanism;
            options.availableMemory = 128 * 1024 * 1024; // Available 128Mb for work;
            options.readTiffDirectory = 1; // Number of tiff directory to convert;
            IProgressListener progressListener = new IProgressListener() {
                @Override
                public void reportProgress(long processedPixels, long totalPixels) {
                    Log.v("Progress reporter",
                            String.format("Processed %d pixels from %d", processedPixels, totalPixels));
                }
            };
            String tiffPath = file.getPath();
            String pngPath = this.reactContext.getCacheDir().getPath() + "/out.png";
            TiffConverter.convertTiffPng(tiffPath, pngPath, options, progressListener);

            // Get converted image and return as b64
            Bitmap bm = BitmapFactory.decodeFile(pngPath);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, stream); // bm is the bitmap object
            byte[] byteArrayImage = stream.toByteArray();
            String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
            promise.resolve(encodedImage);
        } catch (IOException e) {
            e.printStackTrace();
            promise.resolve("failed on saving image");
        }
    }
}
