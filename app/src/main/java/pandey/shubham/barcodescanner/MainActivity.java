package pandey.shubham.barcodescanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import dmax.dialog.SpotsDialog;
import pandey.shubham.barcodescanner.Helper.GraphicOverlay;
import pandey.shubham.barcodescanner.Helper.RectOverlay;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    Button scan;
    AlertDialog waitingDialog;
    GraphicOverlay graphicOverlay;

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        scan = findViewById(R.id.scan_code);
        graphicOverlay = findViewById(R.id.graphicOverlay);


        waitingDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Please wait...")
                .setCancelable(false)
                .build();

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.start();
                cameraView.captureImage();
                graphicOverlay.clear();
            }
        });

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                waitingDialog.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(),false);
                cameraView.start();
                
                runDetector(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });
    }

    private void runDetector(Bitmap bitmap) {
        FirebaseVisionImage visionImage = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_QR_CODE,
                        FirebaseVisionBarcode.FORMAT_PDF417,
                        FirebaseVisionBarcode.FORMAT_ALL_FORMATS
                )
                .build();
        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
        detector.detectInImage(visionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                        processResult(firebaseVisionBarcodes);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        for (FirebaseVisionBarcode items : firebaseVisionBarcodes){

//            Draw rect
            Rect rect = items.getBoundingBox();
            RectOverlay rectOverlay =  new RectOverlay(graphicOverlay,rect);
            graphicOverlay.add(rectOverlay);

            int valueType = items.getValueType();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.create();
            switch (valueType){
                case FirebaseVisionBarcode.TYPE_TEXT:
                    builder.setMessage(items.getRawValue());
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    dialog.show();
                    break;
                case FirebaseVisionBarcode.TYPE_URL:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(items.getRawValue())));
                    break;
                case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
                    String info = new StringBuilder("Name : ")
                            .append(items.getContactInfo().getName().getFormattedName())
                            .append("\n")
                            .append("Address : ")
                            .append(items.getContactInfo().getAddresses().get(0).getAddressLines())
                            .append("\n")
                            .append("Email : ")
                            .append(items.getContactInfo().getEmails().get(0).getAddress())
                            .toString();

                    builder.setMessage(info);
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    dialog.show();
                    break;
            }
            waitingDialog.dismiss();
        }
    }
}
