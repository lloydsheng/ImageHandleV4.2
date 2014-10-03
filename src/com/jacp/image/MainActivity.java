package com.jacp.image;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.jacp.image.util.EditImage;
import com.jacp.image.util.ImageFrameAdder;
import com.jacp.image.util.ImageSpecific;
import com.jacp.image.util.ReverseAnimation;
import com.jacp.image.view.CropImageView;
import com.jacp.image.view.ToneView;
import com.jacp.image.view.menu.MenuView;
import com.jacp.image.view.menu.OnMenuClickListener;
import com.jacp.image.view.menu.SecondaryListMenuView;
import com.jacp.image.view.menu.ToneMenuView;

public class MainActivity extends Activity implements OnSeekBarChangeListener
{
	public boolean mWaitingToPick; // Whether we are wait the user to pick a face.
    public boolean mSaving; // Whether the "save" button is already clicked.
    
    private Handler mHandler = null;
    private ProgressDialog mProgress;
    
	private Bitmap mBitmap;
	
	/**
	 * 临时保存
	 */
	private Bitmap mTmpBmp;
	
	private CropImageView mImageView;
	private EditImage mEditImage;
	private ImageFrameAdder mImageFrame;
	private ImageSpecific mImageSpecific;
	
	/**
	 * 一级菜单
	 */
	private MenuView mMenuView;
	
	private final int[] EDIT_IMAGES = new int[] { R.drawable.ic_menu_crop, R.drawable.ic_menu_rotate_left, R.drawable.ic_menu_mapmode, R.drawable.btn_rotate_horizontalrotate };
	private final int[] EDIT_TEXTS = new int[] { R.string.crop, R.string.rotate, R.string.resize, R.string.reverse_transform };
	
	private final int[] FRAME_IMAGE = new int[] { R.drawable.btn_mainmenu_frame_normal, R.drawable.btn_mainmenu_color_normal, R.drawable.old_remember };
	private final int[] FRAME_TEXTS = new int[] { R.string.frame, R.string.doodle, R.string.specific };
	
	/**
	 * 二级菜单
	 */
	private SecondaryListMenuView mSecondaryListMenu;
	private final int[] ROTATE_IMGRES = new int[] { R.drawable.ic_menu_rotate_left, R.drawable.ic_menu_rotate_right };
	private final int[] ROTATE_TEXTS = new int[] { R.string.rotate_left, R.string.rotate_right };
	
	private final int[] RESIZE_TEXTS = new int[] { R.string.resize_one_to_two, R.string.resize_one_to_three, R.string.resize_one_to_four };
	
	private final int[] FRAME_ADD_IMAGES = new int[] { R.drawable.frame_around1, R.drawable.frame_around2, R.drawable.frame_small1 };
	
	private final int[] FRAME_DOODLE = new int[] { R.drawable.cloudy, R.drawable.qipao1, R.drawable.qipao2 };
	
	private final int[] EDIT_REVERSE = new int[] { R.drawable.btn_rotate_horizontalrotate, R.drawable.btn_rotate_verticalrotate };
	
	/**
	 * 调色菜单
	 */
	private ToneMenuView mToneMenu;
	private ToneView mToneView;
	
	
	/** 调色 */
	private final int FLAG_TONE = 0x1;
	/** 边框 */
	private final int FLAG_FRAME = FLAG_TONE + 1;
	/** 添加边框 */
	private final int FLAG_FRAME_ADD = FLAG_TONE + 6;
	/** 涂鸦 */
	private final int FLAG_FRAME_DOODLE = FLAG_TONE + 7;
	/** 特效 */
	private final int FLAG_FRAME_SPECIFIC = FLAG_TONE + 10;
	/** 编辑 */
	private final int FLAG_EDIT = FLAG_TONE + 2;
	/** 裁剪 */
	private final int FLAG_EDIT_CROP = FLAG_TONE + 3;
	/** 旋转 */
	private final int FLAG_EDIT_ROTATE = FLAG_TONE + 4;
	/** 缩放 */
	private final int FLAG_EDIT_RESIZE = FLAG_TONE + 5;
	/** 反转 */
	private final int FLAG_EDIT_REVERSE = FLAG_TONE + 8;
	
	private View mSaveAll;
	private View mSaveStep;
	
	private final int STATE_CROP = 0x1;
	private final int STATE_DOODLE = STATE_CROP <<1;
	private final int STATE_NONE = STATE_CROP <<2;
	private final int STATE_TONE = STATE_CROP <<3;
	private final int STATE_REVERSE = STATE_CROP <<4;
	private int mState;
	
	/**
	 * 反转动画
	 */
	private ReverseAnimation mReverseAnim;
	private int mImageViewWidth;
	private int mImageViewHeight;
	
	private ProgressDialog mProgressDialog;
	
	private TextView mShowHandleName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		mHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				closeProgress();
				reset();
			}
		};
		
		// 全屏显示
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.image_main);
		
		mSaveAll = findViewById(R.id.save_all);
		mSaveStep = findViewById(R.id.save_step);
		mShowHandleName = (TextView) findViewById(R.id.handle_name);
		
		/*Intent intent = getIntent();
		String path = intent.getStringExtra("path");
		Log.d("may", "MainActivity--->path="+path);
		if (null == path)
		{
			Toast.makeText(this, R.string.load_failure, Toast.LENGTH_SHORT).show();
			finish();
		}*/
		
		mBitmap = BitmapFactory.decodeFile("sdcard/dcim/pig1.jpg");
//		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mm);
		mTmpBmp = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
		mImageView = (CropImageView) findViewById(R.id.crop_image);
		mImageView.setImageBitmap(mBitmap);
        mImageView.setImageBitmapResetBase(mBitmap, true);
        
        mEditImage = new EditImage(this, mImageView, mBitmap);
        mImageFrame = new ImageFrameAdder(this, mImageView, mBitmap);
        mImageView.setEditImage(mEditImage);
        mImageSpecific = new ImageSpecific(this);
        
	}
	
	// -----------------------------------菜单事件------------------------------------
	
	public void onClick(View v)
	{
		int flag = -1;
		switch (v.getId())
		{
		case R.id.save:
//			onSaveClicked();
			String path = saveBitmap(mBitmap);
			if (mProgressDialog != null)
			{
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			Intent data = new Intent();
			data.putExtra("path", path);
			setResult(RESULT_OK, data);
			finish();
			return;
		case R.id.cancel:
			setResult(RESULT_CANCELED);
            finish();
			return;
		case R.id.save_step:
			if (mState == STATE_CROP)
			{
				mTmpBmp = mEditImage.cropAndSave(mTmpBmp);
			}
			else if (mState == STATE_DOODLE)
			{
				mTmpBmp = mImageFrame.combinate(mTmpBmp);
			}
			else if(mState == STATE_TONE)
			{
				// TODO 在菜单消失时要变调色状态为NONE状态
				mTmpBmp = mToneView.getBitmap();
			}
			else if(mState == STATE_REVERSE)
			{
				// 反转完成，要将ImageView反转，再设置图片
				mReverseAnim.cancel();
				mReverseAnim = null;
			}
			
			mBitmap = mTmpBmp;
			showSaveAll();
			reset();
			
			mEditImage.mSaving = true;
			mImageViewWidth = mImageView.getWidth();
			mImageViewHeight = mImageView.getHeight();
			return;
		case R.id.cancel_step:
			if (mState == STATE_CROP)
			{
				mEditImage.cropCancel();
			}
			else if (mState == STATE_DOODLE)
			{
				mImageFrame.cancelCombinate();
			}
			else if (mState == STATE_REVERSE)
			{
				mReverseAnim.cancel();
			}
			showSaveAll();
			resetToOriginal();
			return;
		case R.id.edit:
			flag = FLAG_EDIT;
			break;
		case R.id.tone:
			initTone();
			showSaveStep();
			return;
		case R.id.frame:
			flag = FLAG_FRAME;
			break;
		}
		
		initMenu(flag);
	}
	
	private void initTone()
	{
		if (null == mToneMenu)
		{
			mToneMenu = new ToneMenuView(this);
		}
		
		mToneMenu.show();
		
		mState = STATE_TONE;
		
		mToneView = mToneMenu.getToneView();
		mToneMenu.setHueBarListener(this);
		mToneMenu.setLumBarListener(this);
		mToneMenu.setSaturationBarListener(this);
	}
	
	private void initMenu(int flag)
    {
    	if (null == mMenuView)
    	{
    		mMenuView = new MenuView(this);
			mMenuView.setBackgroundResource(R.drawable.popup);
			mMenuView.setTextSize(16);
			
			switch (flag)
			{
			case FLAG_EDIT:
				mMenuView.setImageRes(EDIT_IMAGES);
				mMenuView.setText(EDIT_TEXTS);
				mMenuView.setOnMenuClickListener(editListener());
				break;
			case FLAG_TONE:
				break;
			case FLAG_FRAME:
				mMenuView.setImageRes(FRAME_IMAGE);
				mMenuView.setText(FRAME_TEXTS);
				mMenuView.setOnMenuClickListener(frameListener());
				break;
			}
		}

		mMenuView.show();
    }
	
	/**
	 * 编辑监听器
	 * @return
	 */
	private OnMenuClickListener editListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				int[] location = new int[2];
				view.getLocationInWindow(location);
				int left = location[0];
				int flag = -1;
				switch (position)
				{
				case 0: // 裁剪
					mMenuView.hide();
					crop();
					showSaveStep();
					return;
				case 1: // 旋转
					flag = FLAG_EDIT_ROTATE;
					break;
				case 2:// 缩放
					flag = FLAG_EDIT_RESIZE;
					break;
				case 3: // 反转
					flag = FLAG_EDIT_REVERSE;
					break;
				}
				
				initSecondaryMenu(flag ,left);
			}

			@Override
			public void hideMenu()
			{
				dimissMenu();
			}
			
		};
	}
	
	private OnMenuClickListener frameListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				int[] location = new int[2];
				view.getLocationInWindow(location);
				int left = location[0];
				int flag = -1;
				switch (position)
				{
				case 0: // 加边框
					flag = FLAG_FRAME_ADD;
					break;
				case 1: // 涂鸦
					flag = FLAG_FRAME_DOODLE;
					break;
				case 2: // 特效
					flag = FLAG_FRAME_SPECIFIC;
					break;
				}
				
				initSecondaryMenu(flag ,left);
			}

			@Override
			public void hideMenu()
			{
				dimissMenu();
			}
			
		};
	}
	
	/**
	 * 菜单消失处理
	 */
	private void dimissMenu()
	{
		mMenuView.dismiss();
		mMenuView = null;
	}
	
	/**
	 * 初始化二级菜单
	 * @param flag
	 * @param left
	 */
	private void initSecondaryMenu(int flag, int left)
	{
		mSecondaryListMenu = new SecondaryListMenuView(this);
		mSecondaryListMenu.setBackgroundResource(R.drawable.popup_bottom_tip);
		mSecondaryListMenu.setTextSize(16);
		mSecondaryListMenu.setWidth(300);
		mSecondaryListMenu.setMargin(left);
		switch (flag)
		{
		case FLAG_EDIT_ROTATE: // 旋转
			mSecondaryListMenu.setImageRes(ROTATE_IMGRES);
			mSecondaryListMenu.setText(ROTATE_TEXTS);
			mSecondaryListMenu.setOnMenuClickListener(rotateListener());
			break;
		case FLAG_EDIT_RESIZE: // 缩放
			mSecondaryListMenu.setText(RESIZE_TEXTS);
			mSecondaryListMenu.setOnMenuClickListener(resizeListener());
			break;
		case FLAG_EDIT_REVERSE: // 反转
			mSecondaryListMenu.setImageRes(EDIT_REVERSE);
			mSecondaryListMenu.setOnMenuClickListener(reverseListener());
			break;
		case FLAG_FRAME_ADD: // 添加边框
			mSecondaryListMenu.setWidth(480);
			mSecondaryListMenu.setImageRes(FRAME_ADD_IMAGES);
			mSecondaryListMenu.setOnMenuClickListener(addFrameListener());
			break;
		case FLAG_FRAME_DOODLE: // 涂鸦
			mSecondaryListMenu.setImageRes(FRAME_DOODLE);
			mSecondaryListMenu.setOnMenuClickListener(doodleListener());
			break;
		case FLAG_FRAME_SPECIFIC: // 特效
			mSecondaryListMenu.setWidth(400);
			mSecondaryListMenu.setText(getResources().getStringArray(R.array.specific_item));
			mSecondaryListMenu.setOnMenuClickListener(specificListener());
			break;
		}

		mSecondaryListMenu.show();
	}
	
	/**
	 * 二级菜单中的旋转事件
	 * @return
	 */
	private OnMenuClickListener rotateListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				switch (position)
				{
				case 0: // 左旋转
					rotate(-90);
					break;
				case 1: // 右旋转
					rotate(90);
					break;
				}
				
				// 一级菜单隐藏
				mMenuView.hide();
				showSaveStep();
			}

			@Override
			public void hideMenu()
			{
				dismissSecondaryMenu();
			}
			
		};
	}
	
	/**
	 * 二级菜单中的缩放事件
	 * @return
	 */
	private OnMenuClickListener resizeListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				float scale = 1.0F;
				switch (position)
				{
				case 0: // 1:2
					scale /= 2;
					break;
				case 1: // 1:3
					scale /= 3;
					break;
				case 2: // 1:4
					scale /= 4;
					break;
				}
				
				resize(scale);
				// 一级菜单隐藏
				mMenuView.hide();
				showSaveStep();
			}
			
			@Override
			public void hideMenu()
			{
				dismissSecondaryMenu();
			}
			
		};
	}
	
	private OnMenuClickListener reverseListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				int flag = -1;
				switch (position)
				{
				case 0: // 水平反转
					flag = 0;
					break;
				case 1: // 垂直反转
					flag = 1;
					break;
				}
				
				reverse(flag);
				// 一级菜单隐藏
				mMenuView.hide();
				showSaveStep();
			}

			@Override
			public void hideMenu()
			{
				dismissSecondaryMenu();
			}
			
		};
	}
	
	/**
	 * 二级菜单中的添加边框事件
	 * @return
	 */
	private OnMenuClickListener addFrameListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				int flag = -1;
				int res = 0;
				switch (position)
				{
				case 0: // 边框1
					flag = ImageFrameAdder.FRAME_SMALL;
					res = 0;
					break;
				case 1: // 边框2
					flag = ImageFrameAdder.FRAME_SMALL;
					res = 1;
					break;
				case 2: // 边框3
					flag = ImageFrameAdder.FRAME_BIG;
					res = R.drawable.frame_big1;
					break;
				case 3: // 边框4
					flag = ImageFrameAdder.FRAME_BIG;
					res = 2;
					break;
				}
				
				addFrame(flag, res);
//				mImageView.center(true, true);
				
				// 一级菜单隐藏
				mMenuView.hide();
				showSaveStep();
			}
			
			@Override
			public void hideMenu()
			{
				dismissSecondaryMenu();
			}
			
		};
	}
	
	/**
	 * 二级菜单中的涂鸦事件
	 * @return
	 */
	private OnMenuClickListener doodleListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				// 涂鸦状态
				mImageView.setState(CropImageView.STATE_DOODLE);
				switch (position)
				{
				case 0: // 涂鸦1
					doodle(R.drawable.cloudy);
					break;
				case 1: // 涂鸦2
					doodle(R.drawable.qipao1);
					break;
				case 2: // 涂鸦3
					doodle(R.drawable.qipao2);
					break;
				}
				
				// 一级菜单隐藏
				mMenuView.hide();
				showSaveStep();
			}
			
			@Override
			public void hideMenu()
			{
				dismissSecondaryMenu();
			}
			
		};
	}
	
	/**
	 * 特效处理标识
	 */
	private int specFlag = -1;
	
	/**
	 * 二级菜单中的特效事件
	 * @return
	 */
	private OnMenuClickListener specificListener()
	{
		return new OnMenuClickListener()
		{
			@Override
			public void onMenuItemClick(AdapterView<?> parent, View view,
					int position)
			{
				switch (position)
				{
				case 0: // 怀旧效果
					specFlag = ImageSpecific.FLAG_OLD_REMEMBER;
					break;
				case 1: // 模糊效果
					specFlag = ImageSpecific.FLAG_BLUR;
					break;
				case 2: // 锐化效果
					specFlag = ImageSpecific.FLAG_SHARPEN;
					break;
				case 3: // 像框叠加
					specFlag = ImageSpecific.FLAG_OVERLAY;
					break;
				case 4: // 底片
					specFlag = ImageSpecific.FLAG_FILM;
					break;
				case 5: // 浮雕
					specFlag = ImageSpecific.FLAG_EMBOSS;
					break;
				case 6: // 光照效果
					specFlag = ImageSpecific.FLAG_SUNSHINE;
					break;
				case 7: // 霓虹
					specFlag = ImageSpecific.FLAG_NEON;
					break;
				case 8: // 叠加2
					specFlag = ImageSpecific.FLAG_ALPHA_LAYER;
					break;
				}
				
				imageSpecific(specFlag);
				// 一级菜单隐藏
				mMenuView.hide();
				showSaveStep();
			}

			@Override
			public void hideMenu()
			{
				dismissSecondaryMenu();
			}
			
		};
	}
	
	/**
	 * 隐藏二级菜单
	 */
	private void dismissSecondaryMenu()
	{
		mSecondaryListMenu.dismiss();
		mSecondaryListMenu = null;
	}
	
	private void showSaveStep()
	{
		mSaveStep.setVisibility(View.VISIBLE);
		mSaveAll.setVisibility(View.GONE);
	}
	
	private void showSaveAll()
	{
		mSaveStep.setVisibility(View.GONE);
		mSaveAll.setVisibility(View.VISIBLE);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
			if (mMenuView != null && mMenuView.isShow() || null != mToneMenu && mToneMenu.isShow())
			{
				mMenuView.hide();
				mToneMenu.hide();
				mToneMenu = null;
			} else
			{
				if (mSaveAll.getVisibility() == View.GONE)
				{
					showSaveAll();
				}
				else
				{
					finish();
				}
			}
			break;
		case KeyEvent.KEYCODE_MENU:
			break;
		
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	
	// ----------------------------------------------------功能----------------------------------------------------

	/**
	 * 进行操作前的准备
	 * @param state 当前准备进入的操作状态
	 * @param imageViewState ImageView要进入的状态
	 * @param hideHighlight 是否隐藏裁剪框
	 */
	private void prepare(int state, int imageViewState, boolean hideHighlight)
	{
		resetToOriginal();
		mEditImage.mSaving = false;
		if (null != mReverseAnim)
		{
			mReverseAnim.cancel();
			mReverseAnim = null;
		}
		
		if (hideHighlight)
		{
			mImageView.hideHighlightView();
		}
		mState = state;
		mImageView.setState(imageViewState);
		mImageView.invalidate();
	}
	
	/**
	 * 裁剪
	 */
	private void crop()
	{
		// 进入裁剪状态
		prepare(STATE_CROP, CropImageView.STATE_HIGHLIGHT, false);
		mShowHandleName.setText(R.string.crop);
		mEditImage.crop(mTmpBmp);
		reset();
	}
	
	/**
	 * 旋转
	 * @param degree 
	 */
	private void rotate(float degree)
	{
		// 未进入特殊状态
		mImageViewWidth = mImageView.getWidth();
		mImageViewHeight = mImageView.getHeight();
		
		prepare(STATE_NONE, CropImageView.STATE_NONE, true);
		mShowHandleName.setText(R.string.rotate);
		Bitmap bm = mEditImage.rotate(mTmpBmp, degree);
		mTmpBmp = bm;
		reset();
	}
	
	private void reverse(int flag)
	{
		// 未进入特殊状态
		prepare(STATE_REVERSE, CropImageView.STATE_NONE, true);
		mShowHandleName.setText(R.string.reverse_transform);
		int type = 0;
		switch (flag)
		{
		case 0:
			type = ReverseAnimation.HORIZONTAL;
			break;
		case 1:
			type = ReverseAnimation.VERTICAL;
			break;
		}
		
		mReverseAnim = new ReverseAnimation(0F, 180F, mImageViewWidth == 0 ? mImageView.getWidth() / 2 : mImageViewWidth / 2, mImageViewHeight == 0 ? mImageView.getHeight() / 2 : mImageViewHeight / 2, 0, true);
		mReverseAnim.setReverseType(type);
		mReverseAnim.setDuration(1000);
		mReverseAnim.setFillEnabled(true);
		mReverseAnim.setFillAfter(true);
		mImageView.startAnimation(mReverseAnim);
		Bitmap bm = mEditImage.reverse(mTmpBmp, flag);
		mTmpBmp = bm;
//		reset();
	}
	
	/**
	 * 缩放
	 * @param bm
	 * @param scale
	 */
	private void resize(float scale)
	{
		// 未进入特殊状态
		prepare(STATE_NONE, CropImageView.STATE_NONE, true);
		mShowHandleName.setText(R.string.resize);
		Bitmap bmp = mEditImage.resize(mTmpBmp, scale);
		mTmpBmp = bmp;
		reset();
	}
	
	/**
	 * 添加边框
	 */
	private void addFrame(int flag, int res)
	{
		// 未进入特殊状态
		prepare(STATE_NONE, CropImageView.STATE_NONE, true);
		mShowHandleName.setText(R.string.frame);
		mTmpBmp = mImageFrame.addFrame(flag, mBitmap, res);
		reset();
	}
	
	/**
	 * 涂鸦
	 */
	private void doodle(int res)
	{
		// 进入涂鸦状态
		prepare(STATE_DOODLE, CropImageView.STATE_DOODLE, true);
		mShowHandleName.setText(R.string.frame_doodle);
		mImageFrame.doodle(res);
		reset();
	}
	
	/**
	 * 特效处理
	 * @param flag
	 */
	private void imageSpecific(final int flag)
	{
		prepare(STATE_NONE, CropImageView.STATE_NONE, true);
		showProgress();
		new Thread(new Runnable()
		{
			public void run()
			{
				mTmpBmp = mImageSpecific.imageSpecific(mTmpBmp, flag);
				mHandler.sendEmptyMessage(0);
			}
		}).start();
	}
	
	/**
	 * 重新设置一下图片
	 */
	private void reset()
	{
		mImageView.setImageBitmap(mTmpBmp);
		mImageView.invalidate();
	}
	
	private void resetToOriginal()
	{
		mTmpBmp = mBitmap;
		mImageView.setImageBitmap(mBitmap);
		// 已经保存图片
		mEditImage.mSaving = true;
		// 清空裁剪操作
		mImageView.mHighlightViews.clear();
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser)
	{
		
		int flag = -1;
		switch ((Integer) seekBar.getTag())
		{
		case 1: // 饱和度
			flag = 1;
			mToneView.setSaturation(progress);
			break;
		case 2: // 色调
			flag = 0;
			mToneView.setHue(progress);
			break;
		case 3: // 亮度
			flag = 2;
			mToneView.setLum(progress);
			break;
		}
		
		Bitmap bm = mToneView.handleImage(mTmpBmp, flag);
		mImageView.setImageBitmapResetBase(bm, true);
		mImageView.center(true, true);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
	}
	
	/**
	 * 显示进度条
	 */
	private void showProgress()
	{
		Context context = this;
		mProgress = ProgressDialog.show(context, null, context.getResources().getString(R.string.handling));
		mProgress.show();
		Log.d("may", "show Progress");
	}
	
	/**
	 * 关闭进度条
	 */
	private void closeProgress()
	{
		if (null != mProgress)
		{
			mProgress.dismiss();
			mProgress = null;
		}
	}
	
	/**
	 * 保存图片到本地
	 * @param bm
	 */
	private String saveBitmap(Bitmap bm)
	{
		mProgressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.save_bitmap));
		mProgressDialog.show();
		return mEditImage.saveToLocal(bm);
	}
	
}
