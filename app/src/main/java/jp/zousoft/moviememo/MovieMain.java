package jp.zousoft.moviememo;

import android.app.ProgressDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Point;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import jp.zousoft.moviememo.db.MDB;
import jp.zousoft.moviememo.db.Movie;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MovieMain extends AppCompatActivity implements TextFinder.Update, View.OnClickListener, AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_main);

        updateSpins();
        ((Spinner)findViewById(R.id.movie_year   )).setOnItemSelectedListener(this);
        ((Spinner)findViewById(R.id.movie_theater)).setOnItemSelectedListener(this);
        ((Spinner)findViewById(R.id.movie_country)).setOnItemSelectedListener(this);
        ((Spinner)findViewById(R.id.movie_sort   )).setOnItemSelectedListener(this);

        mList = (ListView)findViewById(R.id.spec_list);
        mList.setAdapter(new ListArrayAdapter());
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);

        mCal  = Calendar.getInstance();
        mNeed = true;
        mFind = new TextFinder(this, this);
        updateText();

        findViewById(R.id.finder_btn_find ).setOnClickListener(this);
        findViewById(R.id.finder_btn_clear).setOnClickListener(this);
        findViewById(R.id.finder_btn_memo ).setOnClickListener(this);
        findViewById(R.id.finder_btn_clear).setVisibility(View.GONE);
        ((CheckBox)findViewById(R.id.finder_btn_memo)).setChecked(false);
        findViewById(R.id.btn_new).setOnClickListener(this);
    }

    private int[]     mCurr;
    private Calendar	mCal;
    private boolean	mNeed;
    private ListView   mList;
    private TextFinder	mFind;

    private final static int	WC = LinearLayout.LayoutParams.WRAP_CONTENT;

    private static final int	NEW_ID	= 0;
    private static final int	EDIT_ID	= 1;
    private static final int	STAT_ID	= 2;

    private int		mYear;
    private String	    mTheater;
    private String	    mCountry;
    public void onClick(View v)
    {
        int	aID = v.getId();
        if(R.id.btn_new == aID)
        {
            Intent	aIT = new Intent(this, MovieInfo.class);
            aIT.putExtra("index", (int)-1);
            startActivityForResult(aIT, NEW_ID);
        }
        else
        {
            mFind.check(aID);
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        int	aID = parent.getId();
        Spinner	aSpin = (Spinner)findViewById(aID);
        switch(aID)
        {
            case R.id.movie_year:
                if(0 == aSpin.getSelectedItemPosition())
                {
                    mNeed = true;
                    mYear = -1;
                }
                else
                {
                    mNeed = false;
                    mYear = Integer.parseInt((String)aSpin.getSelectedItem());
                }
                break;

            case R.id.movie_theater:
                if(0 == aSpin.getSelectedItemPosition())
                {
                    mTheater = null;
                }
                else
                {
                    mTheater = (String)aSpin.getSelectedItem();
                }
                break;

            case R.id.movie_country:
                if(0 == aSpin.getSelectedItemPosition())
                {
                    mCountry = null;
                }
                else
                {
                    mCountry = (String)aSpin.getSelectedItem();
                }
                break;

            case R.id.movie_sort:
                break;
        }
        updateText();
    }

    public void onNothingSelected(AdapterView<?> arg0)
    {
    }

    private int mSelected;
    private long	mOldTime;
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        mSelected = position;
        mOldTime = ((MDB)getApplication()).Get(mCurr[position]).mTime;
        Intent	aIT = new Intent(this, MovieInfo.class);
        aIT.putExtra("index", mCurr[position]);
        startActivityForResult(aIT, EDIT_ID);
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        final MDB DB = (MDB)getApplication();
        mSelected = position;
        final int   aIdx = mCurr[position];
        mOldTime = DB.Get(aIdx).mTime;
        String	aMsg = "\""+DB.Get(mCurr[position]).mTitle+"\""+getResources().getString(R.string.txt_del_msg);
        new AlertDialog.Builder(this)
                .setTitle(R.string.txt_del_title)
                .setMessage(aMsg)
                .setPositiveButton(R.string.btn_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton)
                            {
                                DB.Del(aIdx);
                                mCal.setTimeInMillis(mOldTime);
                                DB.WriteData(mCal.get(Calendar.YEAR));
                                updateText();
                            }
                        })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();

        return true;
    }

    protected void onActivityResult(int cReq, int cRes, Intent cIT)
    {
        MDB DB = (MDB)getApplication();
        if(RESULT_OK != cRes) return;
        if(NEW_ID == cReq)
        {
            Movie	aMovie = DB.Get(DB.Count()-1);
            mCal.setTimeInMillis(aMovie.mTime);
            DB.WriteData(mCal.get(Calendar.YEAR));
            DB.WriteDB();
        }
        else if(EDIT_ID == cReq)
        {
            int     aIdx   = mCurr[mSelected];
            Movie	aMovie = DB.Get(aIdx);
            mCal.setTimeInMillis(aMovie.mTime);
            int	Y = mCal.get(Calendar.YEAR);
            DB.WriteData(Y);
            if(mOldTime != aMovie.mTime)
            {
                // 時間が変わっている場合は該当年をセーブ
                mCal.setTimeInMillis(mOldTime);
                int	Y1 = mCal.get(Calendar.YEAR);
                if(Y != Y1) DB.WriteData(Y1);
            }
            DB.WriteDB();
        }
        updateSpins();
        updateText();
        mList.invalidateViews();
    }

    public void updateText()
    {
        final MDB DB = (MDB)getApplication();
        int[]	aTemp = new int [DB.Count()];
        int		aCnt  = 0;
        for(int i=0 ; i<aTemp.length ; ++i)
        {
            Movie	aMovie = DB.Get(i);
            if(-1 != mYear)
            {
                mCal.setTimeInMillis(aMovie.mTime);
                if(mYear != mCal.get(Calendar.YEAR)) continue;
            }
            if(null != mTheater)
            {
                if(! mTheater.equals(aMovie.mTheater)) continue;
            }
            if(null != mCountry)
            {
                if(-1 == aMovie.mCountry.indexOf(mCountry)) continue;
            }
            if(! mFind.empty())
            {
                if(mFind.useMemo())
                {
                    // メモ
                    String	aDesc = DB.GetDesc(aMovie.mID);
                    if(null == aDesc) continue;
                    if(! mFind.findOr(aDesc)) continue;
                } else
                {
                    // タイトル
                    if(! mFind.findOr(aMovie.mTitle)) continue;
                }
            }
            aTemp[aCnt++] = i;
        }
        mCurr = new int [aCnt];
        for(int i=0 ; i<aCnt ; ++i) mCurr[i] = aTemp[i];
        if(0 != ((Spinner)findViewById(R.id.movie_sort)).getSelectedItemPosition())
        {
            // 評価順にソートする場合
            Integer[]	IA = new Integer [aCnt];
            for(int i=0 ; i<aCnt ; ++i) IA[i] = Integer.valueOf(mCurr[i]);
            Arrays.sort(IA, new Comparator<Integer>() {
                public int compare(Integer I1, Integer I2)
                {
                    return DB.Get(I1.intValue()).mScore - DB.Get(I2.intValue()).mScore;
                }
            });
            for(int i=0 ; i<aCnt ; ++i) mCurr[i] = IA[i].intValue();
        }

        ((TextView)findViewById(R.id.txt_count)).setText("("+ mCurr.length+"/"+DB.Count()+")");
        mList.invalidateViews();
    }

    /*
     * for Option MENU
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_movie_main, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        final MDB DB = (MDB)getApplication();
        switch(item.getItemId())
        {
            case R.id.menu_file:
                readFile(new Jobs() {
                    public void doit(final String cFile)
                    {
                        final Handler aHndl = new Handler();
                        final ProgressDialog aProg = new ProgressDialog(MovieMain.this) {{
                            setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            setTitle(R.string.prog_title);
                            show();
                        }};
                        new Thread(new Runnable() {
                            public void run() {
                                int[]	aYear = DB.AddFile(cFile);
                                if(null != aYear)
                                {
                                    DB.WriteDB();
                                    for(int y : aYear) DB.WriteData(y);
                                    aHndl.post(new Runnable() {
                                        public void run() { update(); }
                                    });
                                }
                                aProg.dismiss();
                            }
                        }).start();
                    }
                    public int title() { return R.string.txt_dlog_title; }
                });
                return true;

            case R.id.menu_clear:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dlog_del_title)
                        .setPositiveButton(R.string.btn_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton)
                                    {
                                        DB.Clear();
                                        updateText();
                                        mList.invalidateViews();
                                    }
                                })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
                return true;

            case R.id.menu_stat:
                startActivityForResult(new Intent(this, StatView.class), STAT_ID);
                overridePendingTransition(R.anim.right_in, R.anim.right_out);
                return true;

            case R.id.menu_jump:
                new DlogJump(this, mList);
                return true;

            case R.id.menu_quit:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.txt_quit_title)
                        .setMessage(R.string.txt_msg_quit)
                        .setPositiveButton(R.string.btn_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton)
                                    {
                                        finish();
                                    }
                                })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
                return true;
        }

        return false;
    }

    private void update()
    {
        updateSpins();
        updateText();
        mList.invalidateViews();
    }

    interface Jobs
    {
        public void	doit(String cFile);
        public int	title();
    }

    private void readFile(final Jobs cJobs)
    {
        if(! checkRTP()) return;

        // 外部ファイル読み込み
        final String[]	aFile = ((MDB)getApplication()).ExtDir().list(new java.io.FilenameFilter() {
            public boolean accept(java.io.File cDir, String cName) { return cName.endsWith(".txt"); }
        });
        if((null==aFile) || (0==aFile.length))
        {
            new AlertDialog.Builder(this)
                    .setTitle(cJobs.title())
                    .setMessage(R.string.txt_dlog_msg)
                    .setPositiveButton(R.string.text_ok, null)
                    .show();
        }
        else
        {
            new AlertDialog.Builder(this)
                    .setTitle(cJobs.title())
                    .setNegativeButton(R.string.btn_close, null)
                    .setItems(aFile, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            cJobs.doit(aFile[which]);
                        }
                    })
                    .show();
        }
    }

    private void updateSpins()
    {
        MDB DB = (MDB)getApplication();
        updateSpin(DB.Year(),    R.string.select_year,    R.id.movie_year);
        updateSpin(DB.Theater(), R.string.select_theater, R.id.movie_theater);
        updateSpin(DB.Country(), R.string.select_country, R.id.movie_country);
    }

    private void updateSpin(Iterator<String> cIter, int cAID, int cSID)
    {
        // 年メニュー更新
        ArrayAdapter<String>	aAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        aAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aAdapter.add(getResources().getString(cAID));
        while(cIter.hasNext()) aAdapter.add(cIter.next());

        Spinner	aSpin = (Spinner)findViewById(cSID);
        String	aText = (String)aSpin.getSelectedItem();
        aSpin.setAdapter(aAdapter);
        for(int i=0 ; i<aSpin.getCount() ; ++i)
        {
            if(aSpin.getItemAtPosition(i).equals(aText))
            {
                aSpin.setSelection(i);
                break;
            }
        }
        aSpin.invalidate();
    }

    // RuntimePermissionのレスポンスのためのＩＤ
    private final static int	ID_PERMISSION_FOR_READ	= 0;
    private final static int	ID_PERMISSION_FOR_WRITE	= 1;

    private boolean checkRTP()
    {
        // RuntimePermissionの確認
        if(PermissionChecker.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED)
        {
            return true;
        }
        else
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE))
            {
                // 既にユーザに拒否されていた場合
                // SDにアクセスできないとアプリを終了させるしかないが、二度と起動できなくなるためもう一度確認する
                new AlertDialog.Builder(this)
                        .setTitle(R.string.txt_query_title)
                        .setMessage(R.string.txt_query_text)
                        .setPositiveButton(R.string.btn_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton)
                                    {
                                        ActivityCompat.requestPermissions(MovieMain.this, new String[]{READ_EXTERNAL_STORAGE}, ID_PERMISSION_FOR_READ);
                                    }
                                }
                        )
                        .show();
            }
            else
            {
                // 初回起動の場合には許可を求めるダイアログを表示
                ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE}, ID_PERMISSION_FOR_READ);
            }
            // ダイアログは処理を止めないのでここはfalseを返す
            return false;
        }
    }

    private class ListArrayAdapter extends BaseAdapter
    {
        private int mCol1;
        private int mCol2;

        public ListArrayAdapter() {
            mCol1 = Color.rgb(255, 192, 203);
            mCol2 = Color.rgb(200, 200, 200);
        }

        public int getCount()
        {
            if (null == mCurr) return 0;
            return mCurr.length;
        }

        public String getItem(int cPos)
        {
            if (null == mCurr) return "";
            return ((MDB) getApplication()).Get(mCurr[cPos]).mTitle;
        }

        public long getItemId(int cPos) {
            return cPos;
        }

        public View getView(int cPos, View cConvert, ViewGroup cParent)
        {
            cConvert = getLayoutInflater().inflate(R.layout.layout_item, null);

            Movie	aMovie = ((MDB)getApplication()).Get(mCurr[cPos]);

            int w1, w2, fs;
            TextView aText = (TextView)cConvert.findViewById(R.id.movie_no);
            if(null != aText)
            {
                aText.setText((1+cPos)+".");
                createNexusPage(cConvert, aMovie);
                w1 = 180;
                w2 = 96;
                fs = -1;
            }
            else
            {
                createSmallPage(cConvert, aMovie);
                w1 = 108;
                w2 = 64;
                fs = 12;
            }

            mCal.setTimeInMillis(aMovie.mTime);
            String	aDate;
            aText = (TextView)cConvert.findViewById(R.id.movie_date);
            if(mNeed)
            {
                aDate = Movie.DateY(mCal);
                aText.setLayoutParams(new LinearLayout.LayoutParams(w1, WC));
            } else
            {
                aDate = Movie.Date(mCal);
                aText.setLayoutParams(new LinearLayout.LayoutParams(w2, WC));
            }
            aText.setText(aDate);
            if(fs > 0) aText.setTextSize(fs);

            return cConvert;
        }

        private void createNexusPage(View v, Movie cMovie)
        {
            MDB DB = (MDB)getApplication();
            Point	aSize = new Point();
            MovieMain.this.getWindowManager().getDefaultDisplay().getSize(aSize);

            ((TextView)v.findViewById(R.id.movie_score  )).setText(Movie.N2S(cMovie.mScore));
            ((TextView)v.findViewById(R.id.movie_theater)).setText(cMovie.mTheater);
            ((TextView)v.findViewById(R.id.movie_title  )).setText(cMovie.mTitle);

            if(cMovie.mCountry.equals(getResources().getString(R.string.info_unknown)))
            {
                ((TextView)v.findViewById(R.id.movie_country)).setText(R.string.txt_Q);
            } else
            {
                ((TextView)v.findViewById(R.id.movie_country)).setText(cMovie.mCountry);
            }
            ((TextView)v.findViewById(R.id.movie_no)).setTextColor(DB.HasDesc(cMovie.mID)?mCol1:mCol2);

            if(aSize.x < aSize.y)
            {
                v.findViewById(R.id.movie_theater).setLayoutParams(new LinearLayout.LayoutParams(256, WC));
                v.findViewById(R.id.movie_fee ).setVisibility(View.GONE);
                v.findViewById(R.id.movie_memo).setVisibility(View.GONE);
                v.findViewById(R.id.movie_time).setVisibility(View.GONE);
            } else
            {
                v.findViewById(R.id.movie_theater).setLayoutParams(new LinearLayout.LayoutParams(300, WC));
                TextView aText = (TextView)v.findViewById(R.id.movie_fee);
                aText.setVisibility(View.VISIBLE);
                aText.setText(""+cMovie.mFee);
                aText = (TextView)v.findViewById(R.id.movie_memo);
                aText.setVisibility(View.VISIBLE);
                aText.setText(cMovie.mMemo);
                aText = (TextView)v.findViewById(R.id.movie_time);
                aText.setVisibility(View.VISIBLE);
                aText.setText(Movie.Time(mCal));
            }
        }

        private void createSmallPage(View v, Movie cMovie)
        {
            ((TextView)v.findViewById(R.id.movie_score)).setText(Movie.N2S(cMovie.mScore));
            ((TextView)v.findViewById(R.id.movie_title)).setText(cMovie.mTitle);
        }
    }
}
