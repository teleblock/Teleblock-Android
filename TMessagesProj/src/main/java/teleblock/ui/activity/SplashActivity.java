package teleblock.ui.activity;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;

import androidx.annotation.Nullable;

import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.hjq.http.listener.OnHttpListener;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ActivitySplashBinding;
import org.telegram.ui.LaunchActivity;

import java.util.HashMap;

import teleblock.config.AppConfig;
import teleblock.model.SystemEntity;
import teleblock.network.BaseBean;
import teleblock.network.api.SystemCheckApi;
import teleblock.util.EventUtil;
import teleblock.util.MMKVUtil;
import teleblock.util.ManifestUtil;
import teleblock.util.SystemUtil;

/**
 * 创建日期：2022/6/21
 * 描述：
 */
public class SplashActivity extends BaseActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                getWindow().setBackgroundDrawable(null);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = ActivitySplashBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        binding.tvSplashTips.setText(LocaleController.getString("splash_tips", R.string.splash_tips));

        if (MMKVUtil.firstLogin()) {
            EventUtil.track(mActivity, EventUtil.Even.第一次打开, new HashMap<>());
            MMKVUtil.firstLogin(false);
        }

        getDeviceMsg();
        systemCheck();

        if (ApplicationLoader.applicationInited) {
            startMainAct();
        } else {
            if ("nicegram".equals(ManifestUtil.getChannel(this))) {
                startMainAct();
            } else {
                playAnimation();
            }
        }
    }

    private void playAnimation() {
        binding.animationView.setAnimation(R.raw.logo_launch_page);
        binding.animationView.playAnimation();
        binding.animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                startMainAct();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * 获取设备信息
     */
    private void getDeviceMsg() {
        if (MMKVUtil.getString(AppConfig.MkKey.DEVICE_ID).isEmpty()) {
                        MMKVUtil.saveValue(AppConfig.MkKey.DEVICE_ID, SystemUtil.getUniquePsuedoID());
            MMKVUtil.saveValue(AppConfig.MkKey.COUNTRY_CODE, SystemUtil.getCountryZipCode(this));
            MMKVUtil.saveValue(AppConfig.MkKey.SIM_CODE, SystemUtil.getTelContry(this));
        }
    }

    /**
     * 登录完成之后才会跳转主页面
     */
    private void startMainAct() {
        startActivity(new Intent(this, LaunchActivity.class));
        finish();
    }

    /**
     * 请求系统配置信息
     */
    private void systemCheck() {
        EasyHttp.post(new ApplicationLifecycle())
                .api(new SystemCheckApi()
                        .setCountrycode(MMKVUtil.getString(AppConfig.MkKey.COUNTRY_CODE))
                        .setSimcode(MMKVUtil.getString(AppConfig.MkKey.SIM_CODE)))
                .request(new OnHttpListener<BaseBean<SystemEntity>>() {
                    @Override
                    public void onSucceed(BaseBean<SystemEntity> result) {
                        MMKVUtil.setSystemMsg(result.getData());
                    }

                    @Override
                    public void onFail(Exception e) {

                    }
                });
    }
}