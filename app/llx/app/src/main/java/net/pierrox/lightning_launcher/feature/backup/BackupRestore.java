/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.feature.backup;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;

import net.pierrox.lightning_launcher.API;
import net.pierrox.lightning_launcher.activities.ResourceWrapperActivity;
import net.pierrox.lightning_launcher.data.FileUtils;
import net.pierrox.lightning_launcher.data.Page;
import net.pierrox.lightning_launcher.util.FileProvider;
import net.pierrox.lightning_launcher_extreme.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BackupRestore extends ResourceWrapperActivity implements View.OnClickListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, OnLongClickListener {
    private static final int DIALOG_BACKUP_IN_PROGRESS = 1;
    private static final int DIALOG_RESTORE_IN_PROGRESS = 2;
    private static final int DIALOG_SELECT_ARCHIVE_NAME = 3;
    private static final int DIALOG_SELECT_BACKUP_ACTION = 4;
    private static final int DIALOG_CONFIRM_RESTORE = 5;
    private static final int DIALOG_CONFIRM_DELETE = 6;

    private static final int REQUEST_SELECT_PAGES_FOR_EXPORT = 1;
    private static final int REQUEST_SELECT_FILE_TO_IMPORT = 2;
    private static final int REQUEST_SELECT_FILE_TO_LOAD = 3;

    private boolean mSelectArchiveNameForBackup;
    private String mArchiveName;
    private Uri mArchiveUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            intent.setAction(Intent.ACTION_MAIN);
            loadArchive(intent.getData(), null);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backup:
                exportArchive(true);
                break;

            case R.id.import_:
                selectFileToLoadOrImport(true);
                break;

            case R.id.export:
                exportArchive(false);
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.export:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.pierrox.net/cmsms/applications/lightning-launcher/templates.html")));
                return true;

            case R.id.import_:
                selectFileToLoadOrImport(false);
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder;
        ProgressDialog progress;

        switch (id) {
            case DIALOG_BACKUP_IN_PROGRESS:
                progress = new ProgressDialog(this);
                progress.setMessage(getString(R.string.backup_in_progress));
                progress.setCancelable(false);
                return progress;

            case DIALOG_RESTORE_IN_PROGRESS:
                progress = new ProgressDialog(this);
                progress.setMessage(getString(R.string.restore_in_progress));
                progress.setCancelable(false);
                return progress;

            case DIALOG_SELECT_ARCHIVE_NAME:
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.br_n);
                final String archive_name;
                if (mArchiveName == null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm");
                    archive_name = getString(mSelectArchiveNameForBackup ? R.string.backup_d : R.string.tmpl_fn) + "-" + sdf.format(new Date()) + ".lla";
                } else {
                    archive_name = mArchiveName;
                }
                final EditText edit_text = new EditText(this);
                edit_text.setText(archive_name);
                edit_text.setSelection(archive_name.length());
                FrameLayout l = new FrameLayout(this);
                l.setPadding(10, 10, 10, 10);
                l.addView(edit_text);
                builder.setView(l);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = edit_text.getText().toString().trim();
                        String archive_path = FileUtils.LL_EXT_DIR + "/" + name;
                        File old_archive_file = mArchiveName == null ? null : new File(FileUtils.LL_EXT_DIR + "/" + mArchiveName);
                        if (old_archive_file != null && old_archive_file.exists()) {
                            old_archive_file.renameTo(new File(archive_path));
                            loadArchivesList();
                        } else {
                            if (mSelectArchiveNameForBackup) {
                                new BackupTask().execute(archive_path);
                            } else {
                                mArchiveName = FileUtils.LL_EXT_DIR + "/" + name;
                                selectDesktopsToExport();
                            }
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return builder.create();

            case DIALOG_CONFIRM_RESTORE:
                if (mArchiveName != null || mArchiveUri != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_rc);
                    builder.setMessage(mArchiveName == null ? getNameForUri(mArchiveUri) : mArchiveName);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mArchiveName == null) {
                                new RestoreTask(mArchiveUri).execute();
                            } else {
                                String path;
                                if (!mArchiveName.startsWith("/")) {
                                    path = FileUtils.LL_EXT_DIR + "/" + mArchiveName;
                                } else {
                                    path = mArchiveName;
                                }
                                new RestoreTask(path).execute();
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return builder.create();
                }
                break;

            case DIALOG_CONFIRM_DELETE:
                if (mArchiveName != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_dc);
                    builder.setMessage(mArchiveName);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new File(FileUtils.LL_EXT_DIR + "/" + mArchiveName).delete();
                            loadArchivesList();
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return builder.create();
                }
                break;

            case DIALOG_SELECT_BACKUP_ACTION:
                if (mArchiveName != null) {
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.br_a);
                    builder.setItems(new String[]{getString(R.string.br_ob), getString(R.string.br_ot), getString(R.string.br_r), getString(R.string.br_s), getString(R.string.br_d)}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i) {
                                case 0:
                                    new BackupTask().execute(FileUtils.LL_EXT_DIR + "/" + mArchiveName);
                                    break;

                                case 1:
                                    mArchiveName = FileUtils.LL_EXT_DIR + "/" + mArchiveName;
                                    selectDesktopsToExport();
                                    break;

                                case 2:
                                    try {
                                        removeDialog(DIALOG_SELECT_ARCHIVE_NAME);
                                    } catch (Exception e) {
                                    }
                                    showDialog(DIALOG_SELECT_ARCHIVE_NAME);
                                    break;
                                case 3:
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    Uri uri = FileProvider.getUriForFile(new File(FileUtils.LL_EXT_DIR + "/" + mArchiveName));
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                    shareIntent.setType("application/zip");
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.br_s)));
                                    break;
                                case 4:
                                    try {
                                        removeDialog(DIALOG_CONFIRM_DELETE);
                                    } catch (Exception e) {
                                    }
                                    showDialog(DIALOG_CONFIRM_DELETE);
                                    break;
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, null);
                    return builder.create();
                }
                break;
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        loadArchive(null, adapterView.getAdapter().getItem(i).toString());
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        mArchiveName = adapterView.getAdapter().getItem(i).toString();
        showDialog(DIALOG_SELECT_BACKUP_ACTION);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_PAGES_FOR_EXPORT) {
            if (resultCode == RESULT_OK) {
                int[] selected_pages = data.getIntArrayExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS);

                ArrayList<Integer> all_pages = new ArrayList<Integer>();
                for (int p : selected_pages) {
                    all_pages.add(Integer.valueOf(p));
                    addSubPages(all_pages, p);
                }
                all_pages.add(Integer.valueOf(Page.APP_DRAWER_PAGE));
                all_pages.add(Integer.valueOf(Page.USER_MENU_PAGE));

                doExportTemplate(mArchiveName, all_pages);
            }
        } else if (requestCode == REQUEST_SELECT_FILE_TO_IMPORT) {
            if (resultCode == RESULT_OK) {
                importFile(data.getData());
            }
        } else if (requestCode == REQUEST_SELECT_FILE_TO_LOAD) {
            if (resultCode == RESULT_OK) {
                loadArchive(data.getData(), null);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void exportArchive(boolean for_backup) {
        mSelectArchiveNameForBackup = for_backup;
        mArchiveName = null;
        try {
            removeDialog(DIALOG_SELECT_ARCHIVE_NAME);
        } catch (Exception e) {
        }
        showDialog(DIALOG_SELECT_ARCHIVE_NAME);
    }
}
