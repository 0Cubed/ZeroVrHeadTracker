package com.zerocubed.zerovrheadtracker.demo;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Created by Zero
 * Created on 2018/04/11
 * MainActivity
 * Copied from ZeroMediaPlayer: https://github.com/0Cubed/ZeroMediaPlayer
 */

public class MainActivity extends AppCompatActivity {
    private final static int FILE_SELECT_CODE = 0;

    private EditText mEditTextFilePath;
    private RadioGroup mRadioGroupPlayerCore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mEditTextFilePath = (EditText) findViewById(R.id.etFilePath);
        String uri = "file:///storage/emulated/0/DubaiAirport.mp4";
        mEditTextFilePath.setText(uri);

        ImageView ivChooseFile = (ImageView) findViewById(R.id.ivChoose);
        ivChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent, getString(R.string
                                    .choose_video)),
                            FILE_SELECT_CODE);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, R.string.install_file_manager, Toast
                            .LENGTH_SHORT).show();
                }
            }
        });

        RadioButton rbAndroidMediaPlayer = (RadioButton) findViewById(R.id.rbAndroidMediaPlayer);
        rbAndroidMediaPlayer.setChecked(true);

        mRadioGroupPlayerCore = (RadioGroup) findViewById(R.id.rgPlayerCore);

        ImageView ivStart = (ImageView) findViewById(R.id.ivStart);
        ivStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((null == mEditTextFilePath.getText()) || mEditTextFilePath.getText().toString
                        ().trim()
                        .equals("")) {
                    Toast.makeText(MainActivity.this, getString(R.string.choose_video), Toast
                            .LENGTH_LONG).show();
                    return;
                }
                String filePath = mEditTextFilePath.getText().toString();
                String playerCore;
                switch (mRadioGroupPlayerCore.getCheckedRadioButtonId()) {
                    case R.id.rbAndroidMediaPlayer:
                        playerCore = "AndroidMediaPlayer";
                        break;
                    case R.id.rbExoPlayer1:
                        playerCore = "ExoPlayer1.X";
                        break;
                    case R.id.rbExoPlayer2:
                        playerCore = "ExoPlayer2.X";
                        break;
                    case R.id.rbIJKPlayer:
                        playerCore = "IJKPlayer";
                        break;
                    default:
                        playerCore = "AndroidMediaPlayer";
                        break;
                }

                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("filePath", filePath);
                intent.putExtra("playerCore", playerCore);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String path = getPath(this, uri);
                    mEditTextFilePath.setText(path);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
