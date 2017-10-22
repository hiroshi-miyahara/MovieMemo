package jp.zousoft.moviememo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;

public class DlogJump extends AlertDialog.Builder implements DialogInterface.OnClickListener
{
    private View		mLayout;
    private ListView	mList;

    public DlogJump(Activity cActive, ListView cList)
    {
        super(cActive);
        mLayout = cActive.getLayoutInflater().inflate(R.layout.layout_jump_dlog, null);
        mList   = cList;

        setTitle(R.string.dlog_jump_title);
        setMessage(R.string.dlog_jump_msg);
        setView(mLayout);
        setNegativeButton(R.string.dlog_jump_cancel, null);
        setPositiveButton(R.string.dlog_jump_ok, this);
        show();
    }

    public void onClick(DialogInterface dialog, int whichButton)
    {
        int	n = -1;
        switch(((RadioGroup)mLayout.findViewById(R.id.dlog_jump_radio)).getCheckedRadioButtonId())
        {
            case R.id.dlog_jump_first:	n = 1;					break;
            case R.id.dlog_jump_last:	n = mList.getCount();	break;
            case R.id.dlog_jump_num:
                try {
                    TextView	T = (TextView)mLayout.findViewById(R.id.dlog_jump_text);
                    n = Integer.parseInt(T.getText().toString());
                    if(     n < 1)                n = 1;
                    else if(n > mList.getCount()) n = mList.getCount();
                } catch(NumberFormatException e) { n = -1; }
                break;
        }
        if(-1 != n)
        {
            mList.setSelection(n-1);
        }
    }
}
