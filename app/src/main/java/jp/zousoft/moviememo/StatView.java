package jp.zousoft.moviememo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import jp.zousoft.moviememo.db.MDB;
import jp.zousoft.moviememo.db.Movie;

public class StatView extends AppCompatActivity implements Runnable
{
    private Calendar		mCal;
    private Vector<String>	mBuf;
    private Vector<View>	mViews;

    private static int[] TxtID = { R.string.stat_txt_count, R.string.stat_txt_fee, R.string.stat_txt_score };

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stat_view);

        mCal   = Calendar.getInstance();
        mBuf   = new Vector<String>();
        mViews = new Vector<View>();

        findViewById(R.id.stat_close).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                Intent	aIT = new Intent();
                setResult(Activity.RESULT_OK, aIT);
                finish();
                overridePendingTransition(R.anim.left_in, R.anim.left_out);
            }
        });
        ((Spinner)findViewById(R.id.stat_types)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                int	I = ((Spinner)parent).getSelectedItemPosition();
                ((TextView)findViewById(R.id.stat_txt_title)).setText(TxtID[I]);
                for(int i=0 ; i<mViews.size() ; ++i)
                {
                    View	v = mViews.get(i);
                    v.setVisibility((""+I).equals(v.getTag())?View.VISIBLE:View.GONE);
                }
            }
            public void onNothingSelected(AdapterView<?> arg0) { }
        });

        mProg = new ProgressDialog(this) {{
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            setMessage(getResources().getString(R.string.info_progress));
            setMax(13);
            setCancelable(false);
            show();
        }};

        mHndl = new Handler();
        new Thread(this).start();
    }

    private ProgressDialog	mProg;
    private Handler		mHndl;

    // 表の作成
    public void run()
    {
        MDB DB = (MDB)getApplication();

        // 月別鑑賞本数／料金表の作成
        PickMovie	P1 = new PickMovie() {
            public int pickup(Movie cMovie, Calendar cCal)
            {
                return cCal.get(Calendar.MONTH);
            }
        };
        mViews.add(createPanel(R.string.stat_title_month, R.array.stat_month, P1, true));
        mProg.setProgress(1);
        mViews.add(createPanel(R.string.stat_title_month, R.array.stat_month, P1, false));
        mProg.setProgress(2);

        // 評価別鑑賞本数／料金表の作成
        PickMovie	P2 = new PickMovie() {
            public int pickup(Movie cMovie, Calendar cCal)
            {
                return cMovie.mScore;
            }
        };
        mViews.add(createPanel(R.string.stat_title_score, R.array.info_spin_score, P2, true ));
        mProg.setProgress(3);
        mViews.add(createPanel(R.string.stat_title_score, R.array.info_spin_score, P2, false));
        mProg.setProgress(4);

        // 曜日別鑑賞本数／料金表の作成
        PickMovie	P3 = new PickMovie() {
            public int pickup(Movie cMovie, Calendar cCal)
            {
                return cCal.get(Calendar.DAY_OF_WEEK)-1;
            }
        };
        mViews.add(createPanel(R.string.stat_title_week, R.array.stat_week, P3, true ));
        mProg.setProgress(5);
        mViews.add(createPanel(R.string.stat_title_week, R.array.stat_week, P3, false));
        mProg.setProgress(6);

        // 劇場別鑑賞本数／料金表の作成
        PickMovie2	P4 = new PickMovie2() {
            public String[] pickup(Movie cMovie)
            {
                String[]	aTemp = new String [1];
                aTemp[0] = cMovie.mTheater;
                return aTemp;
            }
        };
        mViews.add(createPanel2(DB.Theater(), R.string.stat_title_theater, R.string.stat_txt_theater, P4, true,  null));
        mProg.setProgress(7);
        mViews.add(createPanel2(DB.Theater(), R.string.stat_title_theater, R.string.stat_txt_theater, P4, false, null));
        mProg.setProgress(8);

        // 制作国別鑑賞本数／料金表の作成
        PickMovie2	P5 = new PickMovie2() {
            public String[] pickup(Movie cMovie)
            {
                mBuf.clear();
                for(StringTokenizer	aToken=new StringTokenizer(cMovie.mCountry, ",") ; aToken.hasMoreTokens() ; ) mBuf.add(aToken.nextToken());
                return mBuf.toArray(new String[1]);
            }
        };
        mViews.add(createPanel2(DB.Country(), R.string.stat_title_country, R.string.stat_txt_country, P5, true,  DB.CompCountry()));
        mProg.setProgress(9);
        mViews.add(createPanel2(DB.Country(), R.string.stat_title_country, R.string.stat_txt_country, P5, false, DB.CompCountry()));
        mProg.setProgress(10);
        final int aLimit = mViews.size();

        // 劇場ごとの評価数
        mViews.add(createPanel3(DB.Theater(), R.string.stat_title_theater, R.string.stat_txt_theater, P4, null));
        mProg.setProgress(11);

        // 制作国ごとの評価数
        mViews.add(createPanel3(DB.Country(), R.string.stat_title_country, R.string.stat_txt_country, P5, DB.CompCountry()));
        mProg.setProgress(12);

        mHndl.post(new Runnable() {
            public void run()
            {
                LinearLayout	aLayout = (LinearLayout)findViewById(R.id.stat_table_panel);
                for(int i=0 ; i<aLimit ; ++i)
                {
                    View	v = mViews.get(i);
                    v.setTag(""+(i%2));
                    v.setVisibility((0==(i%2))?View.VISIBLE:View.GONE);
                    aLayout.addView(v);
                }
                for(int i=aLimit ; i<mViews.size() ; ++i)
                {
                    View	v = mViews.get(i);
                    v.setTag("2");
                    v.setVisibility(View.GONE);
                    aLayout.addView(v);
                }
                mHndl = null;
            }
        });

        while(null != mHndl)
        {
            try { Thread.sleep(100); } catch (InterruptedException e) { }
        }
        mProg.dismiss();
    }

    interface PickMovie
    {
        int	pickup(Movie cMovie, Calendar cCal);
    }
    private View createPanel(int cSID, int cAID, PickMovie cPick, boolean cCount)
    {
        MDB DB = (MDB)getApplication();

        View	aView = getLayoutInflater().inflate(R.layout.layout_table, null);
        ((TextView)aView.findViewById(R.id.table_title)).setText(cSID);

        String[]	aArray  = getResources().getStringArray(cAID);
        TableLayout	aLayout = (TableLayout)aView.findViewById(R.id.stat_table);
        int			aGray   = Color.rgb(0x44, 0x44, 0x44);

        TableRow	aTable  = new TableRow(this);
        aTable.addView(newText(R.string.stat_txt_year, aGray));
        for(String s : aArray) aTable.addView(newText(s, aGray));
        aTable .addView(newText(R.string.stat_txt_total, aGray));
        aLayout.addView(aTable);

        Iterator<String>	aIter = DB.Year();
        int	Y1 = Integer.parseInt(aIter.next());
        int	Y2 = Y1;
        while(aIter.hasNext()) Y2 = Integer.parseInt(aIter.next());
        int		aCount = Y2-Y1+1;
        int[]	aPart  = new int [aArray.length*aCount];
        for(int i=0 ; i<DB.Count() ; ++i)
        {
            Movie	aMovie = DB.Get(i);
            mCal.setTimeInMillis(aMovie.mTime);
            int	Y = mCal.get(Calendar.YEAR);
            aPart[cPick.pickup(aMovie, mCal)+aArray.length*(Y-Y1)] += cCount ? 1 : (aMovie.mFee/100);
        }

        int[]	aSum = new int [aArray.length];
        for(int i=Y1 ; i<=Y2 ; ++i)
        {
            aTable = new TableRow(this);
            aTable.addView(newText(""+i, aGray));
            int	aTotal = 0;
            for(int j=0 ; j<aArray.length ; ++j)
            {
                int	aNum = aPart[j+aArray.length*(i-Y1)];
                aTotal  += aNum;
                aSum[j] += aNum;
                aTable.addView(newText(""+aNum, Color.BLACK));
            }
            aTable .addView(newText(""+aTotal, Color.RED));
            aLayout.addView(aTable);
        }

        aTable = new TableRow(this);
        aTable.addView(newText(R.string.stat_txt_total, aGray));
        int	aTotal = 0;
        for(int s : aSum)
        {
            aTotal += s;
            aTable.addView(newText(""+s, Color.RED));
        }
        aTable .addView(newText(""+aTotal, Color.RED));
        aLayout.addView(aTable);

        return aView;
    }

    interface PickMovie2
    {
        public String[]	pickup(Movie cMovie);
    }
    class Calc
    {
        int		mTotal;
        int[]	mSum;
    }
    private View createPanel2(Iterator<String> cIter, int cTID, int cSID, PickMovie2 cPick, boolean cCount, Comparator<String> cComp)
    {
        MDB DB = (MDB)getApplication();

        View	aView = getLayoutInflater().inflate(R.layout.layout_table, null);
        ((TextView)aView.findViewById(R.id.table_title)).setText(cTID);

        Iterator<String>	aIter = DB.Year();
        int	Y1 = Integer.parseInt(aIter.next());
        int	Y2 = Y1;
        while(aIter.hasNext()) Y2 = Integer.parseInt(aIter.next());
        int	aCount = Y2-Y1+1;

        TreeMap<String, Calc>	aMap = new TreeMap<String, Calc>(cComp);
        while(cIter.hasNext())
        {
            Calc	C = new Calc();
            C.mTotal = 0;
            C.mSum   = new int [aCount];
            for(int j=0 ; j<aCount ; ++j) C.mSum[j] = 0;
            aMap.put(cIter.next(), C);
        }

        for(int i=0 ; i<DB.Count() ; ++i)
        {
            Movie	aMovie = DB.Get(i);
            mCal.setTimeInMillis(aMovie.mTime);
            String[]	aList = cPick.pickup(aMovie);
            int		I = cCount ? 1 : (aMovie.mFee/100);
            for(int j=0 ; j<aList.length ; ++j)
            {
                Calc	C = aMap.get(aList[j]);
                C.mTotal += I;
                C.mSum[mCal.get(Calendar.YEAR)-Y1] += I;
                aMap.put(aList[j], C);
            }
        }

        int	aGray = Color.rgb(0x44, 0x44, 0x44);

        TableLayout	aLayout = (TableLayout)aView.findViewById(R.id.stat_table);
        TableRow	aTable  = new TableRow(this);
        aTable.addView(newText(cSID, aGray));
        aTable.addView(newText(R.string.stat_txt_total, aGray));
        for(int i=Y1 ; i<=Y2 ; ++i) aTable.addView(newText(""+i, aGray));
        aLayout.addView(aTable);

        for(Iterator<Map.Entry<String, Calc> > i=aMap.entrySet().iterator() ; i.hasNext() ; )
        {
            aTable = new TableRow(this);
            Map.Entry<String, Calc>	I = i.next();
            Calc	C = I.getValue();
            aTable.addView(newText(I.getKey(),  Color.BLACK));
            aTable.addView(newText(""+C.mTotal, Color.RED));
            for(int j=0 ; j<aCount ; ++j) aTable.addView(newText(""+C.mSum[j], aGray));
            aLayout.addView(aTable);
        }

        return aView;
    }

    private View createPanel3(Iterator<String> cIter, int cTID, int cSID, PickMovie2 cPick, Comparator<String> cComp)
    {
        View	aView = getLayoutInflater().inflate(R.layout.layout_table, null);
        ((TextView)aView.findViewById(R.id.table_title)).setText(cTID);

        String[]	aArray  = getResources().getStringArray(R.array.info_spin_score);

        TreeMap<String, Calc>	aMap = new TreeMap<String, Calc>(cComp);
        while(cIter.hasNext())
        {
            Calc	C = new Calc();
            C.mTotal = 0;
            C.mSum   = new int [aArray.length];
            for(int j=0 ; j<aArray.length ; ++j) C.mSum[j] = 0;
            aMap.put(cIter.next(), C);
        }

        MDB DB = (MDB)getApplication();
        for(int i=0 ; i<DB.Count() ; ++i)
        {
            Movie	aMovie = DB.Get(i);
            mCal.setTimeInMillis(aMovie.mTime);
            String[]	aList = cPick.pickup(aMovie);
            for(String s : aList)
            {
                Calc	C = aMap.get(s);
                ++C.mTotal;
                ++C.mSum[aMovie.mScore];
                aMap.put(s, C);
            }
        }

        int	aGray = Color.rgb(0x44, 0x44, 0x44);

        TableLayout	aLayout = (TableLayout)aView.findViewById(R.id.stat_table);
        TableRow	aTable  = new TableRow(this);
        aTable.addView(newText(cSID, aGray));
        aTable.addView(newText(R.string.stat_txt_total, aGray));
        for(String s : aArray) aTable.addView(newText(s, aGray));
        aLayout.addView(aTable);

        for(Iterator<Map.Entry<String, Calc> > i=aMap.entrySet().iterator() ; i.hasNext() ; )
        {
            aTable = new TableRow(this);
            Map.Entry<String, Calc>	I = i.next();
            Calc	C = I.getValue();
            aTable.addView(newText(I.getKey(),  Color.BLACK));
            aTable.addView(newText(""+C.mTotal, Color.RED));
            for(int j=0 ; j<aArray.length ; ++j) aTable.addView(newText(""+C.mSum[j], aGray));
            aLayout.addView(aTable);
        }

        return aView;
    }

    private TextView newText(int cTID, int cColor)
    {
        TextView	aText = new TextView(this);
        aText.setText(cTID);
        aText.setPadding(0, 0, 16, 0);
        aText.setGravity(Gravity.RIGHT);
        aText.setTextColor(cColor);

        return aText;
    }

    private TextView newText(String cText, int cColor)
    {
        TextView	aText = new TextView(this);
        aText.setText(cText);
        aText.setPadding(0, 0, 16, 0);
        aText.setGravity(Gravity.RIGHT);
        aText.setTextColor(cColor);

        return aText;
    }
}
