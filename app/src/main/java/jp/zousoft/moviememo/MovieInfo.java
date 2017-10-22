package jp.zousoft.moviememo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Calendar;
import java.util.Iterator;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import jp.zousoft.moviememo.db.MDB;
import jp.zousoft.moviememo.db.Movie;

public class MovieInfo extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener
{
    private final static int	WC = LinearLayout.LayoutParams.WRAP_CONTENT;

    private int		mIndex;
    private Movie   mMovie;
    private String		mDesc;
    private Calendar	mCal;
    private long		mTime;
    private int		mTIdx;
    private int		mCIdx;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_info);

        MDB DB = (MDB)getApplication();

        mIndex = getIntent().getExtras().getInt("index");
        mCal   = Calendar.getInstance();
        if(-1 == mIndex)
        {
            // 新規
            mCal.setTimeInMillis(mCal.getTimeInMillis()/10000*10000);
            mMovie = new Movie();
            mMovie.mID      = 0;
            mMovie.mTime    = mCal.getTimeInMillis();
            mMovie.mFee     = 0;
            mMovie.mScore   = 11;
            mMovie.mTitle   = "";
            mMovie.mTheater = "";
            mMovie.mCountry = "";
            mMovie.mMemo    = "";
            mDesc = "";
        }
        else
        {
            // 既存データの修正
            mMovie = DB.Get(mIndex);
            mCal.setTimeInMillis(mMovie.mTime);
            mDesc = DB.GetDesc(mMovie.mID);
            if(null == mDesc) mDesc = "";
        }
        mTime = mMovie.mTime;

        TextView	T1 = (TextView)findViewById(R.id.info_txt_date);
        T1.setText(Movie.DateT(mCal));
        T1.setOnClickListener(this);

        TextView	T2 = (TextView)findViewById(R.id.info_txt_time);
        T2.setText(Movie.Time(mCal));
        T2.setOnClickListener(this);

        TextView	T3 = (TextView)findViewById(R.id.info_txt_country);
        if(0 == mMovie.mCountry.length()) T3.setText(R.string.info_unknown);
        else                                T3.setText(mMovie.mCountry);
        T3.setOnClickListener(this);

        ((EditText)findViewById(R.id.info_edit_title)).setText(mMovie.mTitle);
        ((EditText)findViewById(R.id.info_edit_fee  )).setText((0==mMovie.mFee)?"":(""+mMovie.mFee));
        ((EditText)findViewById(R.id.info_edit_memo )).setText(mMovie.mMemo);
        ((EditText)findViewById(R.id.info_edit_description)).setText(mDesc);

        findViewById(R.id.info_btn_ok    ).setOnClickListener(this);
        findViewById(R.id.info_btn_cancel).setOnClickListener(this);
        if(-1 != mIndex)
        {
            ((TextView)findViewById(R.id.info_btn_cancel)).setText(R.string.info_btn_close);
            ((TextView)findViewById(R.id.info_btn_ok    )).setText(R.string.info_btn_save);
        }

        mTIdx = createSpinner(R.id.info_spin_theater, DB.Theater(), mMovie.mTheater);
        createSpinner(R.id.info_spin_score, null, Movie.N2S(mMovie.mScore));
    }

    private int createSpinner(int cRID, final Iterator<String> cIter, String cText)
    {
        Spinner	aSpinner = (Spinner)findViewById(cRID);
        if(null != cIter)
        {
            aSpinner.setAdapter(
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item) {{
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    add(getResources().getString(R.string.info_unknown));
                    while(cIter.hasNext()) add(cIter.next());
                    add(getResources().getString(R.string.info_add));
                }} );
            aSpinner.setOnItemSelectedListener(this);
        }

        for(int i=0 ; i<aSpinner.getCount() ; ++i)
        {
            if(cText.equals(aSpinner.getItemAtPosition(i)))
            {
                aSpinner.setSelection(i);
                return i;
            }
        }

        return -1;
    }

    private void setDate(int Y, int M, int D)
    {
        mCal.setTimeInMillis(mTime);
        mCal.set(Calendar.YEAR, Y);
        mCal.set(Calendar.MONTH, M-1);
        mCal.set(Calendar.DAY_OF_MONTH, D);
        mTime = mCal.getTimeInMillis();

        ((TextView)findViewById(R.id.info_txt_date)).setText(Movie.Date(mCal));
    }

    private void setTime(int H, int M)
    {
        mCal.setTimeInMillis(mTime);
        mCal.set(Calendar.HOUR_OF_DAY, H);
        mCal.set(Calendar.MINUTE, M);
        mTime = mCal.getTimeInMillis();

        ((TextView)findViewById(R.id.info_txt_time)).setText(Movie.Time(mCal));
    }

    public void onClick(View v)
    {
        MDB DB = (MDB)getApplication();
        switch(v.getId())
        {
            case R.id.info_txt_date:
                mCal.setTimeInMillis(mTime);
                new DatePickerDialog(
                        this,
                        new DatePickerDialog.OnDateSetListener() {
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
                            {
                                setDate(year, 1+monthOfYear, dayOfMonth);
                            }
                        },
                        mCal.get(Calendar.YEAR),
                        mCal.get(Calendar.MONTH),
                        mCal.get(Calendar.DAY_OF_MONTH) )
                    .show();
                break;

            case R.id.info_txt_time:
                mCal.setTimeInMillis(mTime);
                new TimePickerDialog(
                        this,
                        new TimePickerDialog.OnTimeSetListener() {
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute)
                            {
                                setTime(hourOfDay, minute);
                            }
                        },
                        mCal.get(Calendar.HOUR_OF_DAY),
                        mCal.get(Calendar.MINUTE),
                        true )
                    .show();
                break;

            case R.id.info_txt_country:
                new CountryDialog(this).show();
                break;

            case R.id.info_btn_ok:
                // 各種チェックが必要！
                try {
                    mMovie.mFee = Integer.parseInt(((EditText)findViewById(R.id.info_edit_fee)).getText().toString());
                } catch(java.lang.NumberFormatException e) { mMovie.mFee = 0; }

                mMovie.mTime    = mTime;
                mMovie.mScore   = ((Spinner) findViewById(R.id.info_spin_score)).getSelectedItemPosition();
                mMovie.mTitle   = ((EditText)findViewById(R.id.info_edit_title)).getText().toString();
                mMovie.mMemo    = ((EditText)findViewById(R.id.info_edit_memo )).getText().toString();
                mMovie.mTheater = (String)((Spinner)findViewById(R.id.info_spin_theater)).getSelectedItem();
                mMovie.mCountry = ((TextView)findViewById(R.id.info_txt_country)).getText().toString();
                String	aDesc = ((EditText)findViewById(R.id.info_edit_description)).getText().toString();
                if(-1 == mIndex)
                {
                    if(0 != aDesc.length())
                    {
                        mMovie.mID = DB.NewID();
                        DB.SetDesc(mMovie.mID, aDesc);
                    }
                    DB.Add(mMovie);
                }
                else
                {
                    if(0 == aDesc.length())
                    {
                        if(0 != mMovie.mID)
                        {
                            DB.DelDesc(mMovie.mID);
                            mMovie.mID = 0;
                        }
                    }
                    else if(! mDesc.equals(aDesc))
                    {
                        if(0 == mMovie.mID) mMovie.mID = DB.NewID();
                        DB.SetDesc(mMovie.mID, aDesc);
                    }
                }
                setResult(Activity.RESULT_OK, new Intent());
                finish();
                break;

            case R.id.info_btn_cancel:
                setResult(Activity.RESULT_CANCELED, new Intent());
                finish();
                break;
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        if(R.id.info_spin_theater == parent.getId())
        {
            mSpin = (Spinner)parent;
            if(position == mSpin.getCount()-1)
            {
                new NewFieldDialog(this).show();
            }
            else
            {
                mTIdx = mSpin.getSelectedItemPosition();
            }
        }
    }

    public void onNothingSelected(AdapterView<?> arg0)
    {
    }

    /*
     * for Option MENU
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_movie_info, menu);
        if(-1 == mIndex) menu.findItem(R.id.info_menu_copy).setEnabled(false);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.info_menu_file:
                readFile();
                return true;

            case R.id.info_menu_copy:
                if(-1 == mIndex)
                {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.txt_dlog_no)
                            .setPositiveButton("OK", null)
                            .show();
                } else
                {
                    duplicate();
                    mIndex = -1;
                    item.setEnabled(false);
                }
                return true;
        }

        return false;
    }

    private void readFile()
    {
        final MDB DB = (MDB)getApplication();
        final String[]	aFile = DB.ExtDir().list(new java.io.FilenameFilter() {
            public boolean accept(File cDir, String cName) { return cName.endsWith(".txt"); }
        });
        if((null==aFile) || (0==aFile.length))
        {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.txt_dlog_ext)
                    .setMessage(R.string.txt_dlog_msg)
                    .setPositiveButton("OK", null)
                    .show();
        }
        else
        {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.txt_dlog_ext)
                    .setNegativeButton(R.string.btn_close, null)
                    .setItems(aFile, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            StringBuilder	aText = new StringBuilder();
                            try {
                                BufferedReader	aReader = new BufferedReader(
                                                            new InputStreamReader(
                                                            new FileInputStream(new File(DB.ExtDir(), aFile[which]))
                                                                , "SJIS"));	// or "MS932"
                                for(;;)
                                {
                                    String	aLine = aReader.readLine();
                                    if(null == aLine) break;
                                    aText.append(aLine+"\n");
                                }
                                aReader.close();
                            }
                            catch(FileNotFoundException e) { }
                            catch(IOException e) { }
                            ((EditText)findViewById(R.id.info_edit_description)).setText(aText.substring(0, aText.length()-1));
                        }
                    })
                    .show();
        }
    }

    private void duplicate()
    {
        mCal.setTimeInMillis(Calendar.getInstance().getTimeInMillis()/10000*10000);
        Movie	aNew = new Movie();
        aNew.mID      = 0;
        aNew.mTime    = mCal.getTimeInMillis();
        aNew.mFee     = mMovie.mFee;
        aNew.mScore   = mMovie.mScore;
        aNew.mTitle   = mMovie.mTitle;
        aNew.mTheater = mMovie.mTheater;
        aNew.mCountry = mMovie.mCountry;
        aNew.mMemo    = mMovie.mMemo;
        mMovie = aNew;
        mDesc  = "";

        ((TextView)findViewById(R.id.info_btn_cancel)).setText(R.string.info_btn_close);
        ((TextView)findViewById(R.id.info_btn_ok    )).setText(R.string.info_btn_save);
        ((TextView)findViewById(R.id.info_txt_date  )).setText(Movie.DateT(mCal));
        ((TextView)findViewById(R.id.info_txt_time  )).setText(Movie.Time(mCal));
        ((EditText)findViewById(R.id.info_edit_description)).setText("");
    }

    private Spinner	mSpin;
    class NewFieldDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener
    {
        private EditText	mText;

        public NewFieldDialog(Context cContext)
        {
            super(cContext);

            mText = new EditText(cContext);

            setTitle(R.string.info_title_theater);
            setView(mText);
            setNegativeButton(R.string.info_btn_cancel, this);
            setPositiveButton(R.string.info_btn_ok,     this);
        }

        public void onClick(DialogInterface dialog, int whichButton)
        {
            if(DialogInterface.BUTTON_POSITIVE == whichButton)
            {
                String	aName = mText.getText().toString();
                if(CheckName(aName))
                {
                    ArrayAdapter<String>	AA = (ArrayAdapter<String>)mSpin.getAdapter();
                    AA.insert(aName, AA.getCount()-1);
                    mSpin.setSelection(mSpin.getCount()-2);
                    MDB DB = (MDB)getApplication();
                    if(R.id.info_spin_theater == mSpin.getId())
                    {
                        mTIdx = mSpin.getCount()-2;
                        DB.AddTheater(aName);
                    }
                    else
                    {
                        mCIdx = mSpin.getCount()-2;
                        DB.AddCountry(aName);
                    }
                }
                else
                {
                    new AlertDialog.Builder(MovieInfo.this)
                            .setTitle("ERROR")
                            .setMessage("Illegal Name Text ["+aName+"]")
                            .setPositiveButton(R.string.info_btn_ok, null)
                            .show();
                    mSpin.setSelection((R.id.info_spin_theater==mSpin.getId())?mTIdx:mCIdx);
                }
            }
            else if(DialogInterface.BUTTON_NEGATIVE == whichButton)
            {
                mSpin.setSelection((R.id.info_spin_theater==mSpin.getId())?mTIdx:mCIdx);
            }
            mSpin.invalidate();
        }

        private boolean CheckName(String cText)
        {
            return true;
        }
    }

    class CountryDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener
    {
        private LinearLayout	mPanel;

        public CountryDialog(final Context cContext)
        {
            super(cContext);
            Point	aSize = new Point();
            MovieInfo.this.getWindowManager().getDefaultDisplay().getSize(aSize);
            final int	W = ((aSize.x<aSize.y)?aSize.x:aSize.y) * 8 / 10;	// 画面の80%
            final int	F = 48;	// フォントサイズ

            mPanel = new LinearLayout(cContext) {{
                setOrientation(LinearLayout.VERTICAL);
                setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
            }};

            LinearLayout aLine  = new LinearLayout(cContext) {{
                setOrientation(LinearLayout.HORIZONTAL);
                setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
            }};

            int	w = 0;
            for(Iterator<String> i=((MDB)getApplication()).Country() ; i.hasNext() ; )
            {
                final String	aName = i.next();
                CheckBox aCheck = new CheckBox(cContext) {{
                    setText(aName);
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, F);
                    setChecked(-1 != mMovie.mCountry.indexOf(aName));
                }};
                w += F*(aName.length()+1);
                if(w >= W)
                {
                    if(0 == aLine.getChildCount())
                    {
                        aLine.addView(aCheck);
                        aCheck = null;
                        w = 0;
                    } else
                    {
                        w = F*(aName.length()+1);
                    }
                    mPanel.addView(aLine);
                    aLine = new LinearLayout(cContext) {{
                        setOrientation(LinearLayout.HORIZONTAL);
                        setLayoutParams(new LinearLayout.LayoutParams(WC,WC));
                    }};
                }
                if(null != aCheck) aLine.addView(aCheck);
            }
            if(w > 0) mPanel.addView(aLine);

            aLine = new LinearLayout(cContext) {{
                setOrientation(LinearLayout.HORIZONTAL);
                setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
                addView(new TextView(cContext) {{
                        setText(R.string.info_new_country);
                        setTextSize(16);
                        setLayoutParams(new LinearLayout.LayoutParams(WC,WC));
                    }} );
                addView(new EditText(cContext) {{
                        setTextSize(24);
                        setTag("new");
                        setMaxLines(1);
                        setLayoutParams(new LinearLayout.LayoutParams(WC,WC));
                    }} );
            }};
            mPanel.addView(aLine);

            ScrollView	aScroll = new ScrollView(MovieInfo.this)
            {{
                addView(mPanel);
            }};

            setTitle(R.string.info_title_country);
            setView(aScroll);
            setNegativeButton(R.string.info_btn_cancel, null);
            setPositiveButton(R.string.info_btn_ok,     this);
        }

        public void onClick(DialogInterface dialog, int whichButton)
        {
            String	aCNTR = "";
            for(int i=0 ; i<mPanel.getChildCount()-1 ; ++i)
            {
                LinearLayout	aLine = (LinearLayout)mPanel.getChildAt(i);
                for(int j=0 ; j<aLine.getChildCount() ; ++j)
                {
                    CheckBox	aCheck = (CheckBox)aLine.getChildAt(j);
                    if(aCheck.isChecked())
                    {
                        String	aName = aCheck.getText().toString();
                        aCNTR += ","+aName;
                    }
                }
            }
            EditText	aText = (EditText)mPanel.findViewWithTag("new");
            if(0 != aText.length())
            {
                MDB DB = (MDB)getApplication();
                String	aName = aText.getText().toString();
                for(StringTokenizer aToken=new StringTokenizer(aName, ", ") ; aToken.hasMoreTokens() ; )
                {
                    String	aNew = aToken.nextToken();
                    DB.AddCountry(aNew);
                    aCNTR += ","+aNew;
                }
            }
            ((TextView)findViewById(R.id.info_txt_country)).setText(aCNTR.substring(1));
        }
    }
}
