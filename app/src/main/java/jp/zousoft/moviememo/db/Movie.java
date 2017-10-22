package jp.zousoft.moviememo.db;

import java.util.Calendar;

import android.content.res.Resources;

import jp.zousoft.moviememo.R;

public class Movie
{
    public int		mID;		//!< ＤＢキー
    public long	mTime;		//!< millisの値
    public int		mFee;		//!< 料金
    public int		mScore;	//!< 評価(11段階；A+/A/A-/B+/B/B-/C+/C/C-/D/F/-)
    public String	mTitle;	//!< タイトル
    public String	mTheater;	//!< 映画館
    public String	mCountry;	//!< 制作国
    public String	mMemo;		//!< 特記事項(前売り券、無料券など)

    private static String[]	mWeek;
    private static String[]	mLevel;
    public static void Init(Resources cRes)
    {
        mWeek  = cRes.getStringArray(R.array.stat_week);
        mLevel = cRes.getStringArray(R.array.info_spin_score);
    }

    public static String DateT(Calendar C)
    {
        return C.get(Calendar.YEAR)+"年 " + (1+C.get(Calendar.MONTH)) + "/" + C.get(Calendar.DAY_OF_MONTH) + "("+mWeek[C.get(Calendar.DAY_OF_WEEK)-1]+")";
    }

    public static String DateY(Calendar C)
    {
        return C.get(Calendar.YEAR)+"/"+(1+C.get(Calendar.MONTH)) + "/" + C.get(Calendar.DAY_OF_MONTH);
    }

    public static String Date(Calendar C)
    {
        return (1+C.get(Calendar.MONTH)) + "/" + C.get(Calendar.DAY_OF_MONTH);
    }

    public static String Time(Calendar C)
    {
        int	M = C.get(Calendar.MINUTE);
        return C.get(Calendar.HOUR_OF_DAY) + ":" + ((M<10)?"0":"")+M;
    }

    public static String	N2S(int cScore) { return mLevel[cScore]; }

    public static int S2N(String cScore)
    {
        for(int i=0 ; i<mLevel.length ; ++i)
        {
            if(cScore.equals(mLevel[i])) return i;
        }

        return mLevel.length-1;
    }
}
