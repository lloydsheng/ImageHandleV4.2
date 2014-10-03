package com.jacp.image.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.jacp.image.R;

public class ToneView
{
	/**
	 * 楗卞拰搴�
	 */
	private TextView mSaturation;
	private SeekBar mSaturationBar;
	
	/**
	 * 鑹茶皟
	 */
	private TextView mHue;
	private SeekBar mHueBar;
	
	/**
	 * 浜害
	 */
	private TextView mLum;
	private SeekBar mLumBar;
	
	private float mDensity;
	private static final int TEXT_WIDTH = 50;
	
	private LinearLayout mParent;
	
	private ColorMatrix mLightnessMatrix;
	private ColorMatrix mSaturationMatrix;
	private ColorMatrix mHueMatrix;
	private ColorMatrix mAllMatrix;
	
	/**
	 * 浜害
	 */
	private float mLightnessValue = 1F;
	
	/**
	 * 楗卞拰搴�
	 */
	private float mSaturationValue = 0F;
	
	/**
	 * 鑹茬浉
	 */
	private float mHueValue = 0F;
	private final int MIDDLE_VALUE = 127;
	
	/**
	 * 澶勭悊鍚庣殑鍥剧墖
	 */
	private Bitmap mBitmap;
	
	public ToneView(Context context)
	{
		init(context);
	}
	
	private void init(Context context)
	{
		mDensity = context.getResources().getDisplayMetrics().density;
		
		mSaturation = new TextView(context);
		mSaturation.setText(R.string.saturation);
		mHue = new TextView(context);
		mHue.setText(R.string.contrast);
		mLum = new TextView(context);
		mLum.setText(R.string.lightness);
		
		mSaturationBar = new SeekBar(context);
		mSaturationBar.setMax(255);
		mSaturationBar.setProgress(127);
		mSaturationBar.setTag(1);
		
		mHueBar = new SeekBar(context);
		mHueBar.setMax(255);
		mHueBar.setProgress(127);
		mHueBar.setTag(2);
		
		mLumBar = new SeekBar(context);
		mLumBar.setMax(255);
		mLumBar.setProgress(127);
		mLumBar.setTag(3);
		
		LinearLayout saturation = new LinearLayout(context);
		saturation.setOrientation(LinearLayout.HORIZONTAL);
		saturation.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		LinearLayout.LayoutParams txtLayoutparams = new LinearLayout.LayoutParams((int) (TEXT_WIDTH * mDensity), LinearLayout.LayoutParams.MATCH_PARENT);
		mSaturation.setGravity(Gravity.CENTER);
		saturation.addView(mSaturation, txtLayoutparams);
		
		LinearLayout.LayoutParams seekLayoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		saturation.addView(mSaturationBar, seekLayoutparams);
		
		
		LinearLayout hue = new LinearLayout(context);
		hue.setOrientation(LinearLayout.HORIZONTAL);
		hue.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		mHue.setGravity(Gravity.CENTER);
		hue.addView(mHue, txtLayoutparams);
		
		hue.addView(mHueBar, seekLayoutparams);
		
		
		LinearLayout lum = new LinearLayout(context);
		lum.setOrientation(LinearLayout.HORIZONTAL);
		lum.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		mLum.setGravity(Gravity.CENTER);
		lum.addView(mLum, txtLayoutparams);
		lum.addView(mLumBar, seekLayoutparams);
		
		mParent = new LinearLayout(context);
		mParent.setOrientation(LinearLayout.VERTICAL);
		mParent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		mParent.addView(saturation);
		mParent.addView(hue);
		mParent.addView(lum);
	}
	
	public View getParentView()
	{
		return mParent;
	}
	
	public void setSaturationBarListener(OnSeekBarChangeListener l)
	{
		mSaturationBar.setOnSeekBarChangeListener(l);
	}
	
	public void setHueBarListener(OnSeekBarChangeListener l)
	{
		mHueBar.setOnSeekBarChangeListener(l);
	}
	
	public void setLumBarListener(OnSeekBarChangeListener l)
	{
		mLumBar.setOnSeekBarChangeListener(l);
	}
	
	public void setSaturation(int saturation)
	{
		mSaturationValue = (float) (saturation * 1.0D / MIDDLE_VALUE);
	}
	
	public void setHue(int hue)
	{
		mHueValue = (float) (hue * 1.0D / MIDDLE_VALUE);
	}
	
	public void setLum(int lum)
	{
		mLightnessValue = (float) ((lum - MIDDLE_VALUE) * 1.0D / MIDDLE_VALUE * 180);
	}
	
	/**
	 * 杩斿洖澶勭悊鍚庣殑鍥剧墖
	 * @return
	 */
	public Bitmap getBitmap()
	{
		return mBitmap;
	}
	
	/**
	 * 
	 * @param flag
	 *            姣旂壒浣� 琛ㄧず鏄惁鏀瑰彉鑹茬浉锛屾瘮浣�琛ㄧず鏄惁鏀瑰彉楗卞拰搴�姣旂壒浣�琛ㄧず鏄惁鏀瑰彉鏄庝寒搴�
	 */
	public Bitmap handleImage(Bitmap bm, int flag)
	{
		Bitmap bmp = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);
		// 鍒涘缓涓�釜鐩稿悓灏哄鐨勫彲鍙樼殑浣嶅浘鍖�鐢ㄤ簬缁樺埗璋冭壊鍚庣殑鍥剧墖
		Canvas canvas = new Canvas(bmp); // 寰楀埌鐢荤瑪瀵硅薄
		Paint paint = new Paint(); // 鏂板缓paint
		paint.setAntiAlias(true); // 璁剧疆鎶楅敮榻�涔熷嵆鏄竟缂樺仛骞虫粦澶勭悊
		if (null == mAllMatrix)
		{
			mAllMatrix = new ColorMatrix();
		}
		
		if (null == mLightnessMatrix)
		{
			mLightnessMatrix = new ColorMatrix(); // 鐢ㄤ簬棰滆壊鍙樻崲鐨勭煩闃碉紝android浣嶅浘棰滆壊鍙樺寲澶勭悊涓昏鏄潬璇ュ璞″畬鎴�
		}
		
		if (null == mSaturationMatrix)
		{
			mSaturationMatrix = new ColorMatrix();
		}
		
		if (null == mHueMatrix)
		{
			mHueMatrix = new ColorMatrix();
		}

		switch (flag)
		{
		case 0: // 闇�鏀瑰彉鑹茬浉
			// f 琛ㄧず浜害姣斾緥锛屽彇鍊煎皬浜�锛岃〃绀轰寒搴﹀噺寮憋紝鍚﹀垯浜害澧炲己
			mHueMatrix.reset();
			mHueMatrix.setScale(mHueValue, mHueValue, mHueValue, 1); // 绾�缁裤�钃濅笁鍒嗛噺鎸夌浉鍚岀殑姣斾緥,鏈�悗涓�釜鍙傛暟1琛ㄧず閫忔槑搴︿笉鍋氬彉鍖栵紝姝ゅ嚱鏁拌缁嗚鏄庡弬鑰�
			// // android
			// doc
			Log.d("may", "鏀瑰彉鑹茬浉");
			break;
		case 1: // 闇�鏀瑰彉楗卞拰搴�
			// saturation 楗卞拰搴﹀�锛屾渶灏忓彲璁句负0锛屾鏃跺搴旂殑鏄伆搴﹀浘(涔熷氨鏄織璇濈殑鈥滈粦鐧藉浘鈥�锛�
			// 涓�琛ㄧず楗卞拰搴︿笉鍙橈紝璁剧疆澶т簬1锛屽氨鏄剧ず杩囬ケ鍜�
			mSaturationMatrix.reset();
			mSaturationMatrix.setSaturation(mSaturationValue);
			Log.d("may", "鏀瑰彉楗卞拰搴�");
			break;
		case 2: // 浜害
			// hueColor灏辨槸鑹茶疆鏃嬭浆鐨勮搴�姝ｅ�琛ㄧず椤烘椂閽堟棆杞紝璐熷�琛ㄧず閫嗘椂閽堟棆杞�
			mLightnessMatrix.reset(); // 璁句负榛樿鍊�
			mLightnessMatrix.setRotate(0, mLightnessValue); // 鎺у埗璁╃孩鑹插尯鍦ㄨ壊杞笂鏃嬭浆hueColor钁涜搴�
			mLightnessMatrix.setRotate(1, mLightnessValue); // 鎺у埗璁╃豢绾㈣壊鍖哄湪鑹茶疆涓婃棆杞琱ueColor钁涜搴�
			mLightnessMatrix.setRotate(2, mLightnessValue); // 鎺у埗璁╄摑鑹插尯鍦ㄨ壊杞笂鏃嬭浆hueColor钁涜搴�
			// 杩欓噷鐩稿綋浜庢敼鍙樼殑鏄叏鍥剧殑鑹茬浉
			Log.d("may", "鏀瑰彉浜害");
			break;
		}
		mAllMatrix.reset();
		mAllMatrix.postConcat(mHueMatrix);
		mAllMatrix.postConcat(mSaturationMatrix); // 鏁堟灉鍙犲姞
		mAllMatrix.postConcat(mLightnessMatrix); // 鏁堟灉鍙犲姞

		paint.setColorFilter(new ColorMatrixColorFilter(mAllMatrix));// 璁剧疆棰滆壊鍙樻崲鏁堟灉
		canvas.drawBitmap(bm, 0, 0, paint); // 灏嗛鑹插彉鍖栧悗鐨勫浘鐗囪緭鍑哄埌鏂板垱寤虹殑浣嶅浘鍖�
		// 杩斿洖鏂扮殑浣嶅浘锛屼篃鍗宠皟鑹插鐞嗗悗鐨勫浘鐗�
		mBitmap = bmp;
		return bmp;
	}
	
}
