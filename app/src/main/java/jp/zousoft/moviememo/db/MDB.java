package jp.zousoft.moviememo.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import jp.zousoft.moviememo.R;

/**
 * Directory MEMO
 *	Environment.*
 *	getDownloadCacheDirectory()	/cache
 *	getExternalStorageDirectory()	/storage/emulated/0
 *	getRootDirectory()			/system
 *	getDataDirectory()			/data
 *	getExternalStoragePublicDirectory()
 *		DIRECTORY_ALARMS		/storage/emulated/0/Alarms
 *		DIRECTORY_DCIM			/storage/emulated/0/DCIM
 *		DIRECTORY_DOWNLOADS		/storage/emulated/0/Download
 *		DIRECTORY_MOVIES		/storage/emulated/0/Movies
 *		DIRECTORY_MUSIC			/storage/emulated/0/Music
 *		DIRECTORY_NOTIFICATIONS	/storage/emulated/0/Notifications
 *		DIRECTORY_PICTURES		/storage/emulated/0/Pictures
 *		DIRECTORY_PODCASTS		/storage/emulated/0/Podcasts
 *		DIRECTORY_RINGTONES		/storage/emulated/0/Ringtones
 */
public class MDB extends Application
{

    @Override
    public void onCreate()
    {
        super.onCreate();

        Movie.Init(getResources());

        mComp = new Comparator<String>() {
            public int compare(String C1, String C2)
            {
                int	i = mCtName.indexOf(C1);	if(-1 == i) i = 1000;
                int	j = mCtName.indexOf(C2);	if(-1 == j) j = 1000;
                if(i != j) return i-j;
                return C1.compareTo(C2);
            }
        };
        mTheater = new TreeSet<String>();
        mCountry = new TreeSet<String>(mComp);
        mYear    = new TreeSet<String>();
        mInfo    = new Vector<Movie>();
        mCal     = Calendar.getInstance();
        mCtName  = getResources().getString(R.string.countries);

        try {
            BufferedReader aReader = new BufferedReader(new InputStreamReader(openFileInput(SPEC_FILE)));
            ReadDB(aReader);
            aReader.close();
        }
        catch(FileNotFoundException e) { android.util.Log.e("DB", "No DB File"); }
        catch(IOException e) { android.util.Log.e("DB", "Error at DB File"); }

        // 本体データの読み込み
        for(Iterator<String> i=mYear.iterator() ; i.hasNext() ; )
        {
            String	aFile = "";
            try {
                aFile = i.next() + FILE_EXT;
                BufferedReader aReader = new BufferedReader(new InputStreamReader(openFileInput(aFile)));
                ReadMovie(aReader);
                aReader.close();
            }
            catch(FileNotFoundException e) { android.util.Log.e("DB", "No File ["+aFile+"]"); }
            catch(IOException e) { android.util.Log.e("DB", "Error at "+aFile+"]"); }
        }
        // 日付順ソート
        Movie[]	aTemp = mInfo.toArray(new Movie[0]);
        Arrays.sort(aTemp, new Comparator<Movie>() {
            public int compare(Movie M1, Movie M2) {
                return (int) (M1.mTime / 10000 - M2.mTime / 10000);
            }
        });
        mInfo.clear();
        for(int i=0 ; i<aTemp.length ; ++i) mInfo.add(aTemp[i]);

        DBHelper	aDBH = new DBHelper(this);
        mDB = aDBH.getWritableDatabase();
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
    }

    private final static String	SPEC_FILE	= "Spec.txt";
    private final static String	FILE_EXT	= ".txt";
    private final static String	DB_NAME	= "MDB.db";
    private final static String	DB_TABLE	= "movie";
    private final static int	DB_VER		= 1;

    private int				mID;
    private Calendar			mCal;
    private SQLiteDatabase	    mDB;
    private TreeSet<String>	mTheater;
    private TreeSet<String>	mCountry;
    private TreeSet<String>	mYear;
    private Vector<Movie>	    mInfo;

    private String				mCtName;
    private Comparator<String>	mComp;

    public int	NewID() { return ++mID; }

    public int	Count() { return mInfo.size(); }
    public Movie	Get(int cIndex) { return mInfo.get(cIndex); }
    public void	Add(Movie cMovie)
    {
        mInfo.add(cMovie);
        Calendar	C = Calendar.getInstance();
        C.setTimeInMillis(cMovie.mTime);
        mYear.add("" + C.get(Calendar.YEAR));
    }
    public void Del(int cIndex)
    {
        mInfo.remove(cIndex);
    }

    public void Clear()
    {
        mInfo.clear();
        mTheater.clear();
        mCountry.clear();
        mYear.clear();
        WriteDB();
    }

    public void AddTheater(String cTheater) { mTheater.add(cTheater); }

    public void AddCountry(String cCountry)
    {
        String[]	aCT = GetCountry(cCountry);
        for(int i=0 ; i<aCT.length ; ++i) mCountry.add(aCT[i]);
    }

    public Iterator<String>	Theater() { return mTheater.iterator(); }
    public Iterator<String>	Country() { return mCountry.iterator(); }
    public Iterator<String>	Year   () { return mYear.iterator(); }

    public int	CountYear() { return mYear.size(); }

    public String GetDesc(int cID)
    {
        if(cID <= 0) return null;

        Cursor	C = mDB.query(DB_TABLE, new String[] { "id", "info" }, "id='"+cID+"'", null, null, null, null);
        if(0 == C.getCount()) return null;

        C.moveToFirst();
        String	aText = C.getString(1);
        C.close();

        return aText;
    }

    public boolean HasDesc(int cID)
    {
        if(cID <= 0) return false;
        Cursor	C = mDB.query(DB_TABLE, new String[] { "id", "info" }, "id='"+cID+"'", null, null, null, null);
        return 0 != C.getCount();
    }

    public void SetDesc(int cID, String cText)
    {
        String	aTemp = GetDesc(cID);
        if(null != aTemp)
        {
            mDB.delete(DB_TABLE, "id="+cID, null);
        }

        ContentValues	aVal = new ContentValues();
        aVal.put("id",   ""+cID);
        aVal.put("info", cText);
        mDB.insert(DB_TABLE, null, aVal);
    }

    public void DelDesc(int cID)
    {
        mDB.delete(DB_TABLE, "id="+cID, null);
    }

    public Comparator<String> CompCountry() { return mComp; }

    // データクリア
    public void Clear(Context cContext)
    {
        // ファイルクリア
        try {
            OutputStream	aOut = cContext.openFileOutput(SPEC_FILE, Context.MODE_PRIVATE);
            aOut.write("".getBytes());
            aOut.close();
            for(Iterator<String> i=mYear.iterator() ; i.hasNext(); )
            {
                String	Y = i.next();
                aOut = cContext.openFileOutput(Y+FILE_EXT, Context.MODE_PRIVATE);
                aOut.write("".getBytes());
                aOut.close();
            }
        } catch(FileNotFoundException e) { android.util.Log.e("DB", "File Not Found"); }
        catch(IOException e) { android.util.Log.e("DB", "IOException"); }

        // DBの削除
        mDB.execSQL("delete from "+DB_TABLE+";");

        // 内容クリア
        mID = 0;
        mInfo.clear();
        mYear.clear();
        mTheater.clear();
        mCountry.clear();
    }

    public void WriteData(int cYear)
    {
        if(mYear.contains(""+cYear))
        {
            try {
                OutputStream	aOut = openFileOutput(cYear+FILE_EXT, Context.MODE_PRIVATE);
                WriteMovie(aOut, cYear);
                aOut.close();
            }
            catch(FileNotFoundException e) { android.util.Log.e("DB", "Error WriteData No File"); }
            catch(IOException e) { android.util.Log.e("DB", "Error WriteData"); }
        }
    }

    private void ReadDB(BufferedReader cReader) throws IOException
    {
        String	aLine = cReader.readLine();
        if(null == aLine)
        {
            mID = 0;
            return;
        }

        // ID
        mID   = Integer.parseInt(aLine);
        aLine = cReader.readLine();
        if(null == aLine) return;

        // year
        for(StringTokenizer aToken=new StringTokenizer(aLine, ",") ; aToken.hasMoreElements() ; )
        {
            String	aYear = aToken.nextToken();
            try {
                int	Y = Integer.parseInt(aYear);
                if((Y>=2000) && (Y<3000)) mYear.add(aYear);
            } catch(java.lang.NumberFormatException e) { }
        }
    }

    public void WriteDB()
    {
        try {
            OutputStream	aOut = openFileOutput(SPEC_FILE, Context.MODE_PRIVATE);
            // ID
            aOut.write((mID+"\n").getBytes());
            // YEAR
            String	aTemp = "";
            for(Iterator<String> i=mYear.iterator() ; i.hasNext() ; ) aTemp += "," + i.next();
            if(aTemp.length() >= 2) aOut.write((aTemp.substring(1)+"\n").getBytes());
            aOut.close();
        }
        catch(IOException e) { android.util.Log.e("DB", "Error WriteDB"); }
    }

    private void ReadMovie(BufferedReader cRead) throws IOException
    {
        String[]	aBuf = new String [8];
        for(;;)
        {
            String	aLine = cRead.readLine();
            if(null == aLine) break;

            StringTokenizer	aToken = new StringTokenizer(aLine, "\t");
            int	aCnt = 0;
            while(aToken.hasMoreTokens())
            {
                aBuf[aCnt++] = aToken.nextToken();
                if(aCnt >= aBuf.length) break;
            }
            if(aCnt >= aBuf.length-1)
            {
                try {
                    Movie	aMovie = new Movie();
                    aMovie.mID      = Integer.parseInt(aBuf[0]);
                    aMovie.mTime    = 10000L * (long)Integer.parseInt(aBuf[1]);
                    aMovie.mFee     = Integer.parseInt(aBuf[2]);
                    aMovie.mScore   = Integer.parseInt(aBuf[3]);
                    aMovie.mTheater = aBuf[4];
                    aMovie.mCountry = aBuf[5];
                    aMovie.mTitle   = aBuf[6];
                    aMovie.mMemo    = (aCnt<aBuf.length) ? "" : aBuf[7];
                    mInfo.add(aMovie);
                    String[]	aCT = GetCountry(aBuf[5]);
                    for(int i=0 ; i<aCT.length ; ++i) mCountry.add(aCT[i]);
                    mTheater.add(aMovie.mTheater);
                } catch(java.lang.NumberFormatException e)
                {
                    android.util.Log.e("MDB", "TIME ERROR ["+aBuf[1]+"] at "+aBuf[6]);
                }
            }
        }
    }

    private void WriteMovie(OutputStream cOut, int cYear) throws IOException
    {
        for(Iterator<Movie> i=mInfo.iterator(); i.hasNext() ; )
        {
            Movie	aMovie = i.next();
            mCal.setTimeInMillis(aMovie.mTime);
            if(cYear == mCal.get(Calendar.YEAR))
            {
                String	aTemp = "" + aMovie.mID;
                aTemp += "\t" + (aMovie.mTime/10000);
                aTemp += "\t" + aMovie.mFee;
                aTemp += "\t" + aMovie.mScore;
                aTemp += "\t" + aMovie.mTheater;
                aTemp += "\t" + aMovie.mCountry;
                aTemp += "\t" + aMovie.mTitle;
                aTemp += "\t" + aMovie.mMemo;
                cOut.write((aTemp+"\n").getBytes());
            }
        }
    }

    public File ExtDir()
    {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    //    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    public int[] AddFile(String cFile)
    {
        int[]   aYears = null;
        try {
            BufferedReader aReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(ExtDir(), cFile).getPath()), "SJIS"));
            aYears = ReadRaw(aReader, Y2Int(cFile));
            aReader.close();
        }
        catch(FileNotFoundException e) { }
        catch(IOException e) { }

        return aYears;
    }

    private int Y2Int(String cYear)
    {
        int	b = -1;
        int	i = 0;
        while(i < cYear.length())
        {
            char	c = cYear.charAt(i);
            if((c>='0') && (c<='9'))
            {
                b = i;
                break;
            }
            ++i;
        }
        if(-1 != b)
        {
            int	e = -1;
            while(i < cYear.length())
            {
                char	c = cYear.charAt(i);
                if((c<'0') || (c>'9'))
                {
                    e = i-1;
                    break;
                }
                ++i;
            }
            if(-1 == e)
            {
                return Integer.parseInt(cYear);
            } else
            {
                return Integer.parseInt(cYear.substring(b, e+1));
            }
        }

        return Calendar.getInstance().get(Calendar.YEAR);
    }

    //	private static void ReadRaw(LineNumberReader cRead, int cYear) throws IOException
    private int[] ReadRaw(BufferedReader cReader, int cYear) throws IOException
    {
        int		aID   = -1;
        String	aDesc = null;
        TreeSet<Integer>	aYear = new TreeSet<Integer>();
        for( ; ; )
        {
            String	aLine = cReader.readLine();
            if(null == aLine) break;
            if(0 == aLine.length()) continue;
            if('\t' == aLine.charAt(0))
            {
                // 「感想」行
                if(null == aDesc) aDesc  = aLine.substring(1);			// １行目
                else              aDesc += "\n" + aLine.substring(1);	// ２行目以降
            }
            else
            {
                // 通常行
                // 「感想」があれば書き込む
                if(-1 != aID)
                {
                    if(null != aDesc)
                    {
                        SetDesc(aID, aDesc);
                        aDesc = null;
                    }
                    aID = -1;
                }
                // 通常行の解析
                StringTokenizer	aToken = new StringTokenizer(aLine, "\t");
                int			aCount = 0;
                String[]	aTemp  = new String [8];
                while(aToken.hasMoreElements())
                {
                    aTemp[aCount++] = aToken.nextToken();
                    if(aCount >= aTemp.length) break;
                }
                if(aCount < aTemp.length-2)
                {
                    aID = -1;
                }
                else
                {
                    String[]	aCT = GetCountry(aTemp[5]);
                    for(int i=0 ; i<aCT.length ; ++i) mCountry.add(aCT[i]);
                    Movie	aMovie = new Movie();
                    aMovie.mID      = NewID();
                    aMovie.mTime    = ToTime(aTemp[1], aTemp[2], cYear);
                    aMovie.mFee = Integer.parseInt(aTemp[4]);
                    aMovie.mScore   = Movie.S2N(aTemp[0]);		//!< 評価(11段階；A+/A/A-/B+/B/B-/C+/C/C-/D/F/-)
                    aMovie.mTheater = aTemp[3];
                    aMovie.mCountry = aCT[0];
                    for(int i=1 ; i<aCT.length ; ++i) aMovie.mCountry += ","+aCT[i];
                    aMovie.mTitle   = aTemp[6];
                    aMovie.mMemo    = (aCount>=8) ? aTemp[7] : "";
                    aID = aMovie.mID;
                    Add(aMovie);
                    mTheater.add(aMovie.mTheater);
                    mCal.setTimeInMillis(aMovie.mTime);
                    aYear.add(Integer.valueOf(mCal.get(Calendar.YEAR)));
                }

                aDesc = null;
            }
        }

        if(-1 != aID)
        {
            if(null != aDesc)
            {
                SetDesc(aID, aDesc);
            }
        }
        if(0 == aYear.size()) return null;

        int[]	aRes = new int [aYear.size()];
        int		j = 0;
        for(Iterator<Integer> i=aYear.iterator() ; i.hasNext(); )
        {
            int	Y = i.next().intValue();
            mYear.add(""+Y);
            aRes[j++] = Y;
        }
        return aRes;
    }

    private String[] GetCountry(String cText)
    {
        String	aText = cText;
        Vector<String>	aTemp = new Vector<String>();
        while(0 != aText.length())
        {
            String	aTop = aText.substring(0, 1);
            if(aTop.equals(","))
            {
                aText = aText.substring(1);
                continue;
            }
            if(-1 != mCtName.indexOf(aTop))
            {
                aTemp.add(aTop);
                aText = aText.substring(1);
                continue;
            }
            int	b = aText.indexOf(",");
            if(-1 != b)
            {
                aTemp.add(aText.substring(0, b));
                aText = aText.substring(b+1);
            }
            else
            {
                aTemp.add(aText);
                aText = "";
            }
        }

        return aTemp.toArray(new String[1]);
    }

    private long ToTime(String cDate, String cTime, int cYear)
    {
        mCal.setTimeInMillis(0);
        mCal.set(Calendar.YEAR, cYear);

        int	I = cDate.indexOf("/");
        if(-1 == I) return mCal.getTimeInMillis();

        int	J = cDate.indexOf("/", I+1);
        int	Y = cYear;
        int	M = Integer.parseInt(cDate.substring(0, I));
        int	D = 1;
        if(-1 == J)
        {
            D = Integer.parseInt(cDate.substring(I+1));
        }
        else
        {
            Y = M;
            M = Integer.parseInt(cDate.substring(I+1, J));
            D = Integer.parseInt(cDate.substring(J+1));
            mCal.set(Calendar.YEAR, Y);
        }
        mCal.set(Calendar.MONTH, M-1);
        mCal.set(Calendar.DAY_OF_MONTH, D);

        I = cTime.indexOf(":");
        if(-1 != I)
        {
            int	HH = Integer.parseInt(cTime.substring(0, I));
            int	MM = Integer.parseInt(cTime.substring(I+1));
            mCal.set(Calendar.HOUR_OF_DAY, HH);
            mCal.set(Calendar.MINUTE, MM);
        }

        return mCal.getTimeInMillis();
    }

    private class DBHelper extends SQLiteOpenHelper
    {
        public DBHelper(Context cContext)
        {
            super(cContext, DB_NAME, null, DB_VER);
        }

        public void onCreate(SQLiteDatabase cDB)
        {
            cDB.execSQL("create table if not exists "+DB_TABLE+"(id TEXT primary key, info TEXT)");
        }

        public void onUpgrade(SQLiteDatabase cDB, int cOld, int cNew)
        {
            cDB.execSQL("drop table if exists "+DB_TABLE);
            onCreate(cDB);
        }
    }
}
