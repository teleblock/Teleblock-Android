package teleblock.manager;

import android.text.TextUtils;

import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.hjq.http.listener.OnHttpListener;

import org.telegram.messenger.UserConfig;

import teleblock.config.AppConfig;
import teleblock.model.LoginDataResult;
import teleblock.network.BaseBean;
import teleblock.network.api.LoginApi;
import teleblock.util.JsonUtil;
import teleblock.util.MMKVUtil;

/**
 * Time:2022/6/30
 * Author:Perry
 * Description：登录管理类
 */
public class LoginManager {

    /**
     * 用户登录
     * @param loginSuccessful
     * @param loginError
     */
    public static void userLogin(Runnable loginSuccessful, Runnable loginError) {
        String requestTag = "LoginRequest";
        EasyHttp.cancel(requestTag);
        //tgid
        String tgClientUserId = String.valueOf(UserConfig.getInstance(UserConfig.selectedAccount).clientUserId);
        //上次登录存储的tgid
        String lastLoginClientId = MMKVUtil.getString(AppConfig.MkKey.LAST_LOGIN_TGID);
        if (LoginManager.isLogin() && tgClientUserId.equals(lastLoginClientId)) {
            if (loginSuccessful != null) {
                loginSuccessful.run();
            }
            return;
        }
        //判断是否是新用户
        boolean ifNewUser = MMKVUtil.telegramNewUser();

        //参数设置
        LoginApi requestLogin = new LoginApi()
                .setTg_user_id(tgClientUserId)
                .setDevice(MMKVUtil.getString(AppConfig.MkKey.DEVICE_ID))
                .setCountrycode(MMKVUtil.getString(AppConfig.MkKey.COUNTRY_CODE))
                .setSimcode(MMKVUtil.getString(AppConfig.MkKey.SIM_CODE))
                .setIs_tg_new(ifNewUser ? 1 : 0);

        EasyHttp.post(new ApplicationLifecycle())
                .api(requestLogin)
                .tag(requestTag)
                .request(new OnHttpListener<BaseBean<LoginDataResult>>() {
                    @Override
                    public void onSucceed(BaseBean<LoginDataResult> result) {
                        //登录成功，存储当前的tgid
                        MMKVUtil.saveValue(AppConfig.MkKey.LAST_LOGIN_TGID, tgClientUserId);
                        LoginManager.login(result.getData());
                        if (ifNewUser) {
                            MMKVUtil.telegramNewUser(false);
                        }
                        if (loginSuccessful != null) {
                            loginSuccessful.run();
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        if (loginError != null) {
                            loginError.run();
                        }
                    }
                });
    }

    /**
     * 存储登录返回
     * @param user
     */
    public static void login(LoginDataResult user) {
        MMKVUtil.saveValue(AppConfig.MkKey.LOGIN_DATA, JsonUtil.parseObjToJson(user));
    }

    /**
     * 是否登录
     * @return
     */
    public static boolean isLogin() {
        LoginDataResult user = getUser();
        return (user != null && !TextUtils.isEmpty(user.getToken()));
    }

    /**
     * 获取User实体
     * @return
     */
    public static LoginDataResult getUser() {
        //获取登录json
        String loginJson = MMKVUtil.getString(AppConfig.MkKey.LOGIN_DATA);

        if (loginJson.isEmpty()) {
            return new LoginDataResult();
        }

        return JsonUtil.parseJsonToBean(loginJson, LoginDataResult.class);
    }

    /***
     * 用户ID
     * @return
     */
    public static String getUserId() {
        try {
            return getUser().getUser().getUser_id();
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * 获取用户getUserToken
     * @return
     */
    public static String getUserToken() {
        LoginDataResult user = getUser();
        if (user == null) {
            return "";
        } else {
            return user.getToken();
        }
    }

    //是否我们的新用户
    public static boolean isNewer() {
        LoginDataResult user = getUser();
        if (user == null) {
            return false;
        } else {
            return user.isNewer();
        }
    }
}
