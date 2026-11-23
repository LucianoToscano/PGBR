package android.content;
public interface DialogInterface {
    interface OnClickListener { void onClick(DialogInterface dialog, int which); }
    interface OnShowListener { void onShow(DialogInterface dialog); }
    interface OnDismissListener { void onDismiss(DialogInterface dialog); }
}
